package com.asir.moodleactividades.ui.grupos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asir.moodleactividades.data.grupos.ErrorGrupos
import com.asir.moodleactividades.data.grupos.RepositorioGrupos
import com.asir.moodleactividades.domain.Chat
import com.asir.moodleactividades.domain.Examen
import com.asir.moodleactividades.domain.Grupo
import com.asir.moodleactividades.domain.Mensaje
import com.asir.moodleactividades.domain.Miembro
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PestanaGrupo(val etiqueta: String) {
    CHAT("Chat"),
    EXAMENES("Exámenes")
}

data class GruposUiState(
    /** Sin servidor configurado no hay nada que hacer: la pantalla lo explica. */
    val configurado: Boolean = false,
    val cargando: Boolean = false,
    val cargadoAlgunaVez: Boolean = false,
    val grupos: List<Grupo> = emptyList(),
    /** Identificador de este móvil en el servidor: dice qué mensajes son propios. */
    val yo: String = "",
    val error: String? = null,
    /** Confirmaciones breves: «Código copiado», «Has salido del grupo»… */
    val aviso: String? = null,
    /** La identidad guardada dejó de valer y se creó otra: los grupos hay que recuperarlos. */
    val identidadRenovada: Boolean = false,
    /** Una operación que cambia algo está en marcha: los botones esperan. */
    val trabajando: Boolean = false,

    val abierto: Grupo? = null,
    val pestana: PestanaGrupo = PestanaGrupo.CHAT,
    val miembros: List<Miembro> = emptyList(),
    val mensajes: List<Mensaje> = emptyList(),
    val mensajesCargados: Boolean = false,
    val examenes: List<Examen> = emptyList(),
    val examenesCargados: Boolean = false,
    val enviando: Boolean = false
) {
    val soyCreador: Boolean get() = abierto != null && yo.isNotEmpty() && abierto.creador == yo

    val miApodo: String get() = miembros.firstOrNull { it.usuario == yo }?.apodo.orEmpty()

    /** Borrar un examen: quien lo puso o quien lleva el grupo, igual que en el servidor. */
    fun puedeBorrar(examen: Examen): Boolean = examen.autor == yo || soyCreador
}

/**
 * [repositorio] es null cuando la app se compiló sin servidor de grupos: en vez de fallar
 * en cada llamada, la pantalla explica qué falta configurar.
 */
class GruposViewModel(private val repositorio: RepositorioGrupos?) : ViewModel() {

    private val _estado = MutableStateFlow(GruposUiState(configurado = repositorio != null))
    val estado: StateFlow<GruposUiState> = _estado.asStateFlow()

    /** La consulta del chat se repite cada pocos segundos: dos a la vez se pisarían. */
    private var trayendoMensajes = false

    fun cargar() {
        val repo = repositorio ?: return
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            try {
                val yo = repo.yo()
                val grupos = repo.misGrupos()
                _estado.update {
                    it.copy(
                        yo = yo,
                        grupos = grupos,
                        cargadoAlgunaVez = true,
                        error = null,
                        identidadRenovada = repo.identidadRenovada
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(error = mensajeDe(e)) }
            } finally {
                _estado.update { it.copy(cargando = false) }
            }
        }
    }

    fun crear(nombre: String, apodo: String, alTerminar: () -> Unit) = operar(alTerminar) { repo ->
        val grupo = repo.crear(nombre, apodo)
        _estado.update { it.copy(grupos = listOf(grupo) + it.grupos.filterNot { g -> g.id == grupo.id }) }
        abrir(grupo)
    }

    fun unirse(codigo: String, apodo: String, alTerminar: () -> Unit) = operar(alTerminar) { repo ->
        val grupo = repo.unirse(codigo, apodo)
        _estado.update { it.copy(grupos = listOf(grupo) + it.grupos.filterNot { g -> g.id == grupo.id }) }
        abrir(grupo)
    }

    fun abrir(grupo: Grupo) {
        _estado.update {
            it.copy(
                abierto = grupo,
                pestana = PestanaGrupo.CHAT,
                miembros = emptyList(),
                mensajes = emptyList(),
                mensajesCargados = false,
                examenes = emptyList(),
                examenesCargados = false
            )
        }
        viewModelScope.launch {
            traerMiembros()
            traerMensajes()
            traerExamenes()
        }
    }

    fun cerrar() = _estado.update { it.copy(abierto = null) }

    fun elegirPestana(pestana: PestanaGrupo) = _estado.update { it.copy(pestana = pestana) }

    /**
     * Pide solo lo posterior al último mensaje que ya se tiene. Si aparece alguien que no
     * está en la lista de miembros —acaba de entrar—, se vuelve a pedir para tener su apodo.
     */
    suspend fun traerMensajes() {
        val repo = repositorio ?: return
        val grupo = _estado.value.abierto ?: return
        if (trayendoMensajes) return
        trayendoMensajes = true
        try {
            val desde = Chat.ultimoId(_estado.value.mensajes)
            val nuevos = repo.mensajes(grupo.id, desde)
            // Mientras llegaba la respuesta se pudo cambiar de grupo: entonces no vale.
            if (_estado.value.abierto?.id != grupo.id) return
            _estado.update {
                it.copy(mensajes = Chat.fusionar(it.mensajes, nuevos), mensajesCargados = true)
            }
            val conocidos = _estado.value.miembros.map { it.usuario }.toSet()
            if (nuevos.any { it.autor !in conocidos }) traerMiembros()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Un fallo en la consulta periódica no merece un error en pantalla cada 4 s:
            // solo se enseña si aún no se había cargado nada.
            if (!_estado.value.mensajesCargados) _estado.update { it.copy(error = mensajeDe(e)) }
        } finally {
            trayendoMensajes = false
        }
    }

    suspend fun traerExamenes() {
        val repo = repositorio ?: return
        val grupo = _estado.value.abierto ?: return
        try {
            val examenes = repo.examenes(grupo.id)
            if (_estado.value.abierto?.id != grupo.id) return
            _estado.update { it.copy(examenes = examenes, examenesCargados = true) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!_estado.value.examenesCargados) _estado.update { it.copy(error = mensajeDe(e)) }
        }
    }

    /**
     * Si este móvil ya no figura entre los miembros, es que le han expulsado o el grupo ya
     * no existe: se cierra en vez de dejar un chat que nunca va a recibir nada.
     */
    private suspend fun traerMiembros() {
        val repo = repositorio ?: return
        val grupo = _estado.value.abierto ?: return
        try {
            val miembros = repo.miembros(grupo.id)
            if (_estado.value.abierto?.id != grupo.id) return
            val yo = _estado.value.yo.ifEmpty { repo.yo() }
            if (miembros.none { it.usuario == yo }) {
                _estado.update {
                    it.copy(
                        abierto = null,
                        grupos = it.grupos.filterNot { g -> g.id == grupo.id },
                        aviso = "Ya no estás en «${grupo.nombre}»."
                    )
                }
                return
            }
            _estado.update { it.copy(miembros = miembros, yo = yo) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin miembros el chat se ve igual, solo que sin apodos: no es para parar nada.
        }
    }

    fun enviar(texto: String, alEnviar: () -> Unit) {
        val repo = repositorio ?: return
        val grupo = _estado.value.abierto ?: return
        if (_estado.value.enviando || texto.isBlank()) return
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true) }
            try {
                val enviado = repo.enviar(grupo.id, texto)
                _estado.update { it.copy(mensajes = Chat.fusionar(it.mensajes, listOf(enviado))) }
                alEnviar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(error = mensajeDe(e)) }
            } finally {
                _estado.update { it.copy(enviando = false) }
            }
        }
    }

    fun anadirExamen(
        asignatura: String,
        fecha: LocalDate,
        hora: LocalTime?,
        notas: String,
        alTerminar: () -> Unit
    ) = operar(alTerminar) { repo ->
        val grupo = _estado.value.abierto ?: return@operar
        val nuevo = repo.anadirExamen(grupo.id, asignatura, fecha, hora, notas)
        _estado.update { it.copy(examenes = it.examenes + nuevo) }
    }

    fun borrarExamen(examen: Examen) = operar { repo ->
        repo.borrarExamen(examen.id)
        _estado.update { it.copy(examenes = it.examenes.filterNot { e -> e.id == examen.id }) }
    }

    fun cambiarApodo(apodo: String, alTerminar: () -> Unit) = operar(alTerminar) { repo ->
        val grupo = _estado.value.abierto ?: return@operar
        repo.cambiarApodo(grupo.id, apodo)
        traerMiembros()
    }

    fun renovarCodigo() = operar { repo ->
        val grupo = _estado.value.abierto ?: return@operar
        val codigo = repo.renovarCodigo(grupo.id)
        val nuevo = grupo.copy(codigo = codigo)
        _estado.update {
            it.copy(
                abierto = nuevo,
                grupos = it.grupos.map { g -> if (g.id == grupo.id) nuevo else g },
                aviso = "Código cambiado. El anterior ya no sirve para entrar."
            )
        }
    }

    fun expulsar(miembro: Miembro) = operar { repo ->
        val grupo = _estado.value.abierto ?: return@operar
        repo.expulsar(grupo.id, miembro.usuario)
        _estado.update {
            it.copy(
                miembros = it.miembros.filterNot { m -> m.usuario == miembro.usuario },
                aviso = "${miembro.apodo} ya no está en el grupo."
            )
        }
    }

    fun salir() = operar { repo ->
        val grupo = _estado.value.abierto ?: return@operar
        repo.salir(grupo.id)
        _estado.update {
            it.copy(
                abierto = null,
                grupos = it.grupos.filterNot { g -> g.id == grupo.id },
                aviso = "Has salido de «${grupo.nombre}»."
            )
        }
    }

    fun eliminar() = operar { repo ->
        val grupo = _estado.value.abierto ?: return@operar
        repo.eliminar(grupo.id)
        _estado.update {
            it.copy(
                abierto = null,
                grupos = it.grupos.filterNot { g -> g.id == grupo.id },
                aviso = "Grupo «${grupo.nombre}» eliminado."
            )
        }
    }

    fun avisar(texto: String) = _estado.update { it.copy(aviso = texto) }

    fun descartarAviso() = _estado.update { it.copy(aviso = null) }

    fun descartarError() = _estado.update { it.copy(error = null) }

    fun descartarIdentidadRenovada() = _estado.update { it.copy(identidadRenovada = false) }

    /** Operación que cambia algo: bloquea los botones mientras dura y enseña el error si falla. */
    private fun operar(alTerminar: () -> Unit = {}, bloque: suspend (RepositorioGrupos) -> Unit) {
        val repo = repositorio ?: return
        if (_estado.value.trabajando) return
        viewModelScope.launch {
            _estado.update { it.copy(trabajando = true, error = null) }
            try {
                bloque(repo)
                alTerminar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(error = mensajeDe(e)) }
            } finally {
                _estado.update { it.copy(trabajando = false) }
            }
        }
    }

    private fun mensajeDe(e: Exception): String =
        (e as? ErrorGrupos)?.message ?: "Algo ha fallado con los grupos. Vuelve a intentarlo."
}
