package com.asir.moodleactividades

import android.app.Application
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.asir.moodleactividades.data.ActividadesRepository
import com.asir.moodleactividades.data.AlmacenBus
import com.asir.moodleactividades.data.AlmacenFaltas
import com.asir.moodleactividades.data.AlmacenHorario
import com.asir.moodleactividades.data.AlmacenNetacad
import com.asir.moodleactividades.data.CacheActividades
import com.asir.moodleactividades.data.CacheCalificaciones
import com.asir.moodleactividades.data.Conectividad
import com.asir.moodleactividades.data.CredencialesCifradas
import com.asir.moodleactividades.data.DescargaAdjuntos
import com.asir.moodleactividades.data.HistorialAvisos
import com.asir.moodleactividades.data.PreferenciasAvisos
import com.asir.moodleactividades.data.SesionNetacad
import com.asir.moodleactividades.data.SesionSeneca
import com.asir.moodleactividades.data.SesionStore
import com.asir.moodleactividades.data.grupos.ClienteSupabase
import com.asir.moodleactividades.data.grupos.ConfigSupabase
import com.asir.moodleactividades.data.grupos.GuardaSesionPrefs
import com.asir.moodleactividades.data.grupos.RepositorioGrupos
import com.asir.moodleactividades.notificaciones.AvisosEnviados
import com.asir.moodleactividades.notificaciones.RecordatoriosWorker
import com.asir.moodleactividades.ui.actividades.ActividadesViewModel
import com.asir.moodleactividades.ui.ajustes.AjustesViewModel
import com.asir.moodleactividades.ui.asistencia.AsistenciaViewModel
import com.asir.moodleactividades.ui.avisos.AvisosViewModel
import com.asir.moodleactividades.ui.bus.BusViewModel
import com.asir.moodleactividades.ui.faltas.FaltasViewModel
import com.asir.moodleactividades.ui.grupos.GruposViewModel
import com.asir.moodleactividades.ui.horario.HorarioViewModel
import com.asir.moodleactividades.ui.login.LoginViewModel
import com.asir.moodleactividades.ui.netacad.NetacadViewModel
import com.asir.moodleactividades.ui.notas.NotasViewModel

/** La aplicación solo existe para guardar las dependencias durante toda su vida. */
class AppActividades : Application() {
    val dependencias: Dependencias by lazy { Dependencias(this) }
}

/**
 * Todo lo que la app necesita, construido en un solo sitio y una sola vez. Antes cada pantalla
 * montaba sus piezas dentro de MainActivity; ahora las pantallas piden su ViewModel y aquí se
 * decide con qué se construye.
 */
class Dependencias(contexto: Context) {

    private val app = contexto.applicationContext

    val repositorio by lazy {
        ActividadesRepository(SesionStore(app), CacheActividades(app), CacheCalificaciones(app))
    }
    val conectividad by lazy { Conectividad(app) }
    val historial by lazy { HistorialAvisos(app) }
    val sesionSeneca by lazy { SesionSeneca(app) }
    val sesionNetacad by lazy { SesionNetacad(app) }

    /** Null si la app se compiló sin servidor de grupos: la pantalla lo explica. */
    private val repositorioGrupos: RepositorioGrupos? by lazy {
        val config = ConfigSupabase(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_CLAVE)
        if (config.configurado) RepositorioGrupos(ClienteSupabase(config, GuardaSesionPrefs(app))) else null
    }

    // ------------------------------------------------------------------ ViewModels

    fun login() = LoginViewModel(repositorio)
    fun actividades() = ActividadesViewModel(repositorio, conectividad, DescargaAdjuntos(app, repositorio))
    fun notas() = NotasViewModel(repositorio, conectividad)
    fun asistencia() = AsistenciaViewModel(repositorio)
    fun avisos() = AvisosViewModel(historial)
    fun ajustes() = AjustesViewModel(PreferenciasAvisos(app))
    fun faltas() = FaltasViewModel(
        AlmacenFaltas(app),
        sesionSeneca,
        historial,
        CredencialesCifradas(app, CredencialesCifradas.SENECA)
    )
    fun netacad() = NetacadViewModel(
        AlmacenNetacad(app),
        sesionNetacad,
        historial,
        CredencialesCifradas(app, CredencialesCifradas.NETACAD)
    )
    fun grupos() = GruposViewModel(repositorioGrupos)
    fun horario() = HorarioViewModel(AlmacenHorario(app))

    /** El papel de la parada va en otro almacén: es un documento distinto del horario de clase. */
    fun documentoBus() = HorarioViewModel(AlmacenHorario(app, "horario_bus"))
    fun bus() = BusViewModel(AlmacenBus(app))

    // ------------------------------------------------------------------ salir

    /**
     * El alumno cierra sesión: se borra todo lo de su cuenta de Moodle —token, copias,
     * adjuntos— y también el historial de avisos y las notificaciones que haya en la barra,
     * que hablan de sus notas y entregas. Antes quien entrase después las veía.
     *
     * Séneca, NetAcad y los grupos son cuentas aparte y se desconectan desde su pantalla.
     */
    fun cerrarSesion() {
        RecordatoriosWorker.cancelar(app)
        repositorio.cerrarSesion()
        DescargaAdjuntos(app, repositorio).borrarTodo()
        historial.borrar()
        AvisosEnviados(app).borrarTodo()
        runCatching { NotificationManagerCompat.from(app).cancelAll() }
    }

    /**
     * Moodle ha dado el token por caducado: se olvida el token y se paran las comprobaciones
     * de fondo, que sin él solo fallarían. Las copias de tareas y notas se conservan.
     */
    fun sesionCaducada() {
        RecordatoriosWorker.cancelar(app)
        repositorio.caducarSesion()
    }
}

/** Una fábrica de ViewModel a partir de una función: evita una clase por pantalla. */
fun fabrica(crear: () -> ViewModel) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = crear() as T
}
