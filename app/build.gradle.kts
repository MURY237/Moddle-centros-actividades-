// En un script de Gradle, «java» es la extensión java { } del proyecto, no el paquete:
// java.util.Base64 no resuelve, así que se importa.
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Servidor de los grupos (Supabase). Se lee del entorno —los secretos de GitHub en CI— o de
// gradle.properties. Sin él la app compila igual y la pestaña de grupos explica qué falta.
fun ajusteSupabase(entorno: String, propiedad: String): String =
    System.getenv(entorno)?.takeIf { it.isNotBlank() }
        ?: (project.findProperty(propiedad) as String?).orEmpty()

val supabaseUrl = ajusteSupabase("SUPABASE_URL", "supabase.url").trim().trimEnd('/')
val supabaseClave = ajusteSupabase("SUPABASE_CLAVE", "supabase.clave").trim()

// Van a parar a código Java generado: solo se aceptan si tienen la forma esperada.
if (supabaseUrl.isNotEmpty() && !Regex("""^https://[A-Za-z0-9.-]+(:\d+)?$""").matches(supabaseUrl)) {
    throw GradleException("supabase.url no parece la URL de un proyecto de Supabase: $supabaseUrl")
}
if (supabaseClave.isNotEmpty() && !Regex("""^[A-Za-z0-9._-]+$""").matches(supabaseClave)) {
    throw GradleException("supabase.clave tiene caracteres que no son de una clave de Supabase")
}

// La clave secreta se salta todas las reglas de la base de datos. Dentro de un APK, que
// cualquiera puede abrir, dejaría leer y borrar todos los grupos: con ella no se compila.
val claveSupabaseSecreta = supabaseClave.startsWith("sb_secret_") ||
    supabaseClave.split('.').let { partes ->
        partes.size == 3 && runCatching {
            String(Base64.getUrlDecoder().decode(partes[1]), Charsets.UTF_8)
        }.getOrDefault("").contains(Regex(""""role"\s*:\s*"service_role""""))
    }
if (claveSupabaseSecreta) {
    throw GradleException(
        "Esa es la clave SECRETA de Supabase (service_role). En la app va la pública: " +
            "«anon» o «publishable»."
    )
}

android {
    namespace = "com.asir.moodleactividades"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.asir.moodleactividades"
        minSdk = 26
        targetSdk = 35
        versionCode = 50
        versionName = "1.49"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_CLAVE", "\"$supabaseClave\"")
    }

    // Firma fija en el repositorio: sin ella cada compilación firmaría distinto y Android
    // rechazaría instalar la actualización sobre la versión anterior.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.retrofit)
    implementation(libs.retrofit.scalars)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work)
    implementation(libs.androidx.security.crypto)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
