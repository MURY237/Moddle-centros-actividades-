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
- Avisa con notificaciones de las entregas próximas y de las actividades recién
  publicadas, con la antelación, la frecuencia y la hora que elija el usuario.
- Guarda el horario de clase en PDF o imagen y lo muestra con zoom, sin conexión.

## Funciones de la API usadas

| Función | Uso |
|---|---|
| `core_webservice_get_site_info` | Validar el token y leer el nombre del usuario |
| `mod_assign_get_assignments` | Listar las tareas de todos los cursos matriculados |
| `mod_assign_get_submission_status` | Saber si cada tarea está entregada y calificada |
| `gradereport_user_get_grade_items` | Leer la calificación de cada tarea |
| `core_calendar_get_action_events_by_timesort` | Añadir cuestionarios y otras actividades con fecha |

## Cómo se clasifica cada actividad

1. **Entregada**: la entrega existe y su estado es `submitted`.
2. **No entregada**: no hay entrega y la fecha límite ya pasó.
3. **Pendiente**: el resto, incluidos los borradores (`draft`) sin enviar.

El filtro temporal recorta solo lo que aún no ha vencido: una tarea caducada sin
entregar sigue apareciendo aunque filtres por «7 días», porque sigue siendo
accionable.

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
