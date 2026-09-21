# Actividades Moodle

Desarrollada por **Mury237**.

Aplicación Android que muestra las actividades de un Moodle de centro educativo
(Moodle Centros, Moodle propio del instituto, etc.) clasificadas en **pendientes**,
**entregadas** y **no entregadas**, agrupadas por plazo: hoy, próximos 7 días,
próximos 30 días y más adelante.

## Cómo accede a los datos

Moodle **no permite consultar las actividades de alguien solo con su nombre de usuario**.
La app usa los Web Services oficiales de Moodle, que exigen un token asociado a la cuenta:

| Modo de acceso | Cuándo usarlo |
|---|---|
| iDEA/Séneca por navegador | Centros con SSO (Moodle Centros de Andalucía) |
| Correo (o usuario) + contraseña | Centros con login propio de Moodle |
| Token manual | Centros donde el perfil muestra «Claves de seguridad» |

En el modo SSO la app abre `admin/tool/mobile/launch.php` en el navegador del
sistema; el usuario se autentica en iDEA y Moodle devuelve el token a la app
mediante el esquema `moodleactividades://`. La app valida que la firma
(`md5(wwwroot + passport)`) corresponda al sitio solicitado antes de aceptar el
token, de modo que otra aplicación no pueda inyectar uno ajeno. La contraseña
nunca pasa por la app.

El campo de acceso admite tanto el **correo electrónico** como el nombre de usuario:
ambos viajan en el parámetro `username` y es Moodle quien los resuelve. Entrar con
el correo solo funciona si el centro tiene activada la opción
«Permitir identificarse mediante el email» (`authloginviaemail`); si falla, usa el
nombre de usuario.

El token se obtiene contra `/login/token.php` con el servicio `moodle_mobile_app`.
La contraseña **no se almacena**: solo se guarda el token en las `SharedPreferences`
privadas de la app, con copia de seguridad del sistema desactivada
(`allowBackup="false"`).

Para generar un token manualmente en Moodle:
*Perfil → Preferencias → Claves de seguridad → «Moodle mobile web service»*.

## Qué hace

- Clasifica las actividades en pendientes, entregadas y no entregadas.
- Las agrupa por plazo y permite filtrar por estado, por rango de fechas y por asignatura.
- Muestra la calificación de las tareas ya corregidas.
- Guarda la última carga correcta: al abrir la app se ven las actividades al
  instante, y si el centro no responde sale un aviso indicando de cuándo son los
  datos, en lugar de una pantalla vacía.
- Avisa con notificaciones de las entregas próximas, de las actividades recién
  publicadas y de las notas que acaba de subir el profesorado, con la antelación,
  la frecuencia y la hora que elija el usuario.
- Guarda todos esos avisos en la pestaña «Avisos», para poder consultarlos aunque
  se haya descartado la notificación del sistema.
- Guarda el horario de clase en PDF o imagen y lo muestra con zoom, sin conexión.
- Guarda los horarios del autobús y dice cuánto falta para el siguiente.
- Abre la ficha de cada tarea con su enunciado y sus documentos adjuntos, que se
  descargan y se abren desde la propia app, sin entrar en Moodle.

## Funciones de la API usadas

| Función | Uso |
|---|---|
| `core_webservice_get_site_info` | Validar el token y leer el nombre del usuario |
| `mod_assign_get_assignments` | Listar las tareas, su enunciado y sus adjuntos |
| `mod_assign_get_submission_status` | Saber si cada tarea está entregada y calificada |
| `gradereport_user_get_grade_items` | Leer la calificación de cada tarea |
| `core_calendar_get_action_events_by_timesort` | Añadir cuestionarios y otras actividades con fecha |

## Enunciados y adjuntos

`mod_assign_get_assignments` devuelve, junto a cada tarea, su enunciado en HTML y la
lista de archivos que el profesorado adjuntó. La app pinta el enunciado tal cual y
descarga los adjuntos a su caché para abrirlos con el visor del móvil.

Los archivos se sirven por `webservice/pluginfile.php`, que exige el token en la
propia URL. Ese token se añade **solo en el momento de la descarga**: en la copia
guardada de las actividades queda la URL limpia, sin credencial alguna.

## Avisos

Hay tres canales de notificación independientes, para poder silenciar uno sin perder
los otros:

| Canal | Cuándo avisa |
|---|---|
| Entregas próximas | La tarea sigue sin entregar y su plazo entra en la antelación elegida |
| Actividades nuevas | Aparece una actividad que no estaba en la comprobación anterior |
| Notas publicadas | Una actividad ya conocida pasa a tener nota, o esa nota cambia |

La nota nueva se detecta comparando la consulta de calificaciones con la copia de la
anterior. Lo que aparece por primera vez no genera aviso de nota: eso es una actividad
nueva, y si contara, la primera sincronización avisaría del curso entero de golpe. El
total del curso también queda fuera, porque se mueve con cada nota y duplicaría el aviso.

Todo lo notificado queda en la pestaña «Avisos», con un contador de no leídos en la barra
inferior. Se guardan los últimos cien y cada uno abre su actividad en Moodle.

## Faltas de asistencia

En los centros de Andalucía las faltas se llevan en **Séneca**, no en Moodle, y Séneca
no publica ninguna API: ni oficial ni documentada. Lo único que existe son los endpoints
internos de iPASEN, obtenidos por ingeniería inversa, que no están soportados, cambian
sin aviso y obligarían a guardar la contraseña de Séneca en el móvil.

Antes de dar eso por perdido, Ajustes incluye una comprobación que pregunta al Moodle del
centro qué funciones abre a la app (`core_webservice_get_site_info` devuelve la lista) y
busca las del módulo de asistencia. Hay institutos que lo tienen instalado, y en ese caso
las faltas se pueden leer por la misma vía que todo lo demás, con el token que ya existe
y sin credenciales nuevas.

El resultado se puede copiar como texto plano. El diagnóstico lleva el nombre del sitio,
la versión de Moodle y las funciones encontradas: **ningún token ni dato personal**.

### Leerlas de Séneca

Que no haya API no significa que no haya datos: la página de faltas de Séneca es una tabla
HTML corriente dentro de la sesión que el alumno abre. La app aprovecha eso sin pedir
credenciales:

1. Carga Séneca en un navegador incrustado **que no se muestra**. Si la sesión sigue viva
   —lo normal salvo la primera vez—, el alumno no llega a ver ninguna página web: solo un
   «Consultando tus faltas» y, acto seguido, sus tarjetas.
2. Busca en el menú la entrada «Faltas de asistencia» por su texto y la pulsa. Si no está,
   despliega «Seguimiento del curso»; y si tampoco está, abre el menú lateral, que en móvil
   viene plegado tras el icono de las tres rayas y cuyas entradas ni siquiera existen en la
   página hasta abrirlo. La hamburguesa no tiene texto, así que se reconoce por sus
   atributos, descartando los elementos grandes para no pulsar la barra entera. Séneca monta
   su interfaz con marcos, así que se recorren todos, no solo el documento principal.
3. Pone el desplegable «Mostrar» en «Todas»: sin eso se leerían solo algunas faltas.
4. Busca una tabla cuyos encabezados hablen de fecha y de ausencia. Se localiza por sus
   encabezados, nunca por su posición ni por un identificador interno: Séneca los genera
   por sesión.
5. Guarda las filas y las muestra agrupadas por asignatura, separando justificadas de
   injustificadas, con las mismas tarjetas que el resto de la app.

**Séneca solo se enseña cuando hay que identificarse**, que es lo que ocurre si no quedan ni
tabla ni menú que pulsar.

### La sesión caduca, y eso no lo arregla ninguna cookie

Séneca cierra la sesión por su cuenta y lo dice con todas las letras: «se ha agotado el
tiempo de sesión». Por eso la app puede, opcionalmente, guardar la cuenta y rellenar el
formulario igual que haría el alumno: cierra el aviso, escribe usuario y contraseña y envía.

Es lo único de la app que guarda una contraseña, así que:

- Se cifra con el **almacén de claves de Android**, no en preferencias normales. Si ese
  almacén no está disponible, no se guarda nada: antes eso que dejarla sin cifrar.
- Es **opcional** y se borra desde la misma tarjeta, o con «Borrar faltas y cerrar la sesión».
- Se intenta **como mucho dos veces por apertura**: una para cerrar el aviso y otra para
  enviar el formulario. Si Séneca vuelve a pedir acceso, se avisa y se para, porque insistir
  con una contraseña que no vale acaba bloqueando la cuenta del alumno.
- Los datos se inyectan en el guion **codificados como literal de JavaScript**, nunca pegados
  tal cual: una comilla o una barra en la contraseña romperían el código, y lo que rompe el
  código también puede ejecutarlo.

Séneca ata la sesión a dos cosas: sus cookies y la ruta `/seneca/nav/<algo>` que genera al
entrar. Por eso se guarda también la última página donde se encontró la tabla y se vuelve
directamente a ella, en lugar de empezar por la portada.

Las cookies de Séneca no llevan caducidad, así que el navegador incrustado las tira al
cerrarse la app y habría que identificarse en cada arranque. Para evitarlo se guardan en las
preferencias privadas y se reponen antes de cargar la página: la sesión continúa donde
estaba hasta que Séneca la caduca por su cuenta, y entonces —solo entonces— se vuelve a
pedir el acceso. Lo guardado equivale a una sesión abierta, **nunca a la contraseña**, y se
borra al desconectar.

## Al volver a la aplicación

Entrar en la app es el momento en que se quiere ver lo último, así que ahí se refrescan
las actividades, las notas, las faltas y los avisos. Cada pantalla decide si le toca: si su
última carga correcta tiene menos de un minuto, no repite la consulta. Ese tope no es
cosmético —los cortafuegos de los centros cortan por exceso de peticiones seguidas y la
pantalla se quedaría vacía—, pero es lo bastante corto para que al abrir la app se vea el
estado real.

Las faltas solo se refrescan solas si hay una sesión de Séneca guardada. El refresco
trabaja en un navegador diminuto detrás de la lista: el alumno sigue viendo sus faltas
mientras tanto, con un indicador en la cabecera, y lo nuevo aparece solo cuando llega.

**El refresco automático nunca abre Séneca.** Si la sesión ha caducado, se deja un aviso con
un botón para entrar y ahí decide el alumno; quitarle de delante lo que estaba mirando para
plantarle un formulario es peor que no actualizar. Séneca solo ocupa la pantalla cuando el
acceso se pide a mano. Lo que aparezca de nuevo
se apunta además en «Avisos».

La app **nunca ve ni guarda la contraseña**: la sesión vive en las cookies del navegador
incrustado, y el botón «Borrar faltas y cerrar la sesión de Séneca» las elimina junto con
los datos guardados. Nada sale del teléfono.

Si Séneca cambia la página, el guion deja de encontrar la tabla y lo dice, mostrando los
encabezados que sí ha visto —solo los encabezados, nunca el contenido— para poder ajustarlo.

### Diagnóstico

Séneca no se puede probar sin un móvil con sesión abierta, así que la pantalla de faltas
lleva un desplegable con lo que la app vio en su último intento: cuántas cookies guardó y
cuántas seguían vivas, cuántas páginas cargó, hasta qué ruta llegó y si acabó pidiendo el
acceso. Se puede copiar como texto.

El diagnóstico lleva **solo nombres de cookies, nunca sus valores** —que son la sesión—, y la
ruta con su parte variable recortada, porque el identificador de sesión de Séneca viaja
dentro de la propia URL.

## De dónde salen las asignaturas

La lista de asignaturas no se deduce de las actividades, sino de la matrícula
(`core_enrol_get_users_courses`). La diferencia importa cuando el centro añade una
asignatura nueva: hasta que alguien publique su primera tarea no aparecería en ninguna
actividad, y deducirla de ahí la haría invisible justo cuando más se quiere comprobar que
está.

Una asignatura sin nada dentro sale igual: en «Tareas» se puede elegir en el filtro y
explica que aún no tiene actividades, y en «Notas» aparece como «Todavía sin actividades
evaluables». Si el centro no responde a esa consulta se conserva la lista anterior, en vez
de quedarse sin asignaturas.

## El autobús

Para quien viene de un pueblo, la pregunta diaria no es solo qué hay que entregar, sino si
da tiempo a coger el autobús. La pestaña «Horario» tiene por eso dos vistas: las clases y el
autobús.

Los horarios los escribe el alumno —no hay ninguna API de transporte que valga para todos
los pueblos— y se guardan en el móvil, así que funciona sin conexión. Se apuntan por
trayecto, porque la ida y la vuelta suelen tener horas y días distintos.

Arriba sale la próxima salida de todas las líneas, con cuánto falta, y en ámbar cuando
quedan quince minutos o menos. Debajo, las salidas de hoy de cada línea: las que ya han
pasado se apagan pero no se ocultan, porque sirven para hacerse una idea de la frecuencia.

Las horas se guardan como minutos desde medianoche, no como texto: así se ordenan y se
comparan sin volver a interpretarlas. Al escribirlas se acepta «7:15», «07.15» o «0715», y
lo que no sea una hora válida no llega a guardarse.

## Cómo se clasifica cada actividad

1. **Entregada**: la entrega existe y su estado es `submitted`.
2. **No entregada**: no hay entrega y la fecha límite ya pasó.
3. **Pendiente**: el resto, incluidos los borradores (`draft`) sin enviar.

El filtro temporal recorta solo lo que aún no ha vencido, y tiene dos excepciones, las dos
por el mismo motivo: lo que sigue estando por hacer no debe esconderse.

- Una tarea **caducada sin entregar** aparece aunque filtres por «7 días».
- Una tarea **sin fecha límite** aparece con cualquier filtro. No vence nunca, así que
  ningún recorte temporal puede dejarla fuera; va a su propio grupo, «Sin fecha límite»,
  al final de la lista.

## Compilar

### Con Android Studio

1. Clona el repositorio y ábrelo en Android Studio (Ladybug o superior).
2. Espera a que Gradle sincronice.
3. Ejecuta con `Run > Run 'app'` o genera el APK con `Build > Build APK(s)`.

### Desde la terminal

```bash
./gradlew test          # tests unitarios
./gradlew assembleDebug # genera app/build/outputs/apk/debug/app-debug.apk
```

### Sin instalar nada

Cada push a `main` compila el APK y lo publica como
[release](../../releases/latest). La propia app comprueba al abrirse si hay una
versión más reciente y ofrece descargarla e instalarla sin salir de ella.

El APK se firma con la `debug.keystore` incluida en el repositorio. No protege
nada —su contraseña es pública—, pero mantiene la firma estable entre
compilaciones: sin ella Android rechazaría instalar una actualización sobre la
versión anterior.

## Requisitos

- Android 8.0 (API 26) o superior
- JDK 17 para compilar
- El centro debe tener habilitados los servicios web de Moodle y el servicio móvil

## Estructura

```
app/src/main/java/com/asir/moodleactividades/
├── data/
│   ├── net/              Cliente Retrofit, DTOs y errores de Moodle
│   ├── SesionStore.kt    Persistencia del token
│   └── ActividadesRepository.kt
├── domain/
│   ├── Modelos.kt        Actividad, estados, grupos y filtros
│   └── Clasificador.kt   Lógica pura de clasificación (cubierta por tests)
└── ui/
    ├── login/            Pantalla de acceso
    ├── actividades/      Listado, filtros y resumen
    └── theme/
```

## Limitaciones conocidas

- Los estados «entregada/no entregada» solo son exactos para tareas (`mod_assign`).
  Los cuestionarios y otras actividades del calendario aparecen como pendientes
  mientras Moodle los marque como acción requerida.
- Si el centro usa SSO y no permite generar tokens, la app no puede acceder.
- Las tareas de grupo usan la entrega del grupo cuando existe.
