import com.android.build.api.artifact.SingleArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

/*
 * Firma del release (S9). Los datos se leen de variables de entorno o de `local.properties` (que no se sube al
 * repositorio); nunca se escriben aquí. Sin ellos, `assembleRelease` genera un APK sin firmar.
 */
val propiedadesLocales =
    Properties().apply {
        rootProject
            .file("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use { load(it) }
    }

fun secreto(nombre: String): String? = System.getenv(nombre) ?: propiedadesLocales.getProperty(nombre)

val almacenFirma = secreto("CINETRACK_KEYSTORE")?.let { rootProject.file(it) }

android {
    namespace = "com.cinesstar.cinetrack"
    compileSdk =
        libs.versions.sdkCompile
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.cinesstar.cinetrack"
        minSdk =
            libs.versions.sdkMin
                .get()
                .toInt()
        targetSdk =
            libs.versions.sdkTarget
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (almacenFirma != null) {
            create("release") {
                storeFile = almacenFirma
                storePassword = secreto("CINETRACK_KEYSTORE_PASSWORD")
                keyAlias = secreto("CINETRACK_KEY_ALIAS")
                keyPassword = secreto("CINETRACK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    // RNF-12: solo español; no se empaquetan traducciones de las bibliotecas.
    androidResources { localeFilters += listOf("es") }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.useJUnitPlatform()
                test.maxHeapSize = "3g"
                // Robolectric (SDK 36) accede a internos de FileDescriptor del JDK 21.
                test.jvmArgs(
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                )
                // Los tests de capturas (T0.08) guardan aquí la imagen generada para revisarla a mano.
                test.systemProperty(
                    "cinetrack.capturas",
                    layout.buildDirectory
                        .dir("capturas")
                        .get()
                        .asFile.path,
                )
                test.systemProperty("cinetrack.raiz", rootProject.projectDir.path)
                test.systemProperty("robolectric.graphicsMode", "NATIVE")
            }
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

/*
 * T9.18 / TX.01 (RES-03): el release no pide INTERNET ni permisos de red y no incluye librerías de red. Se comprueba en
 * el manifiesto combinado (con el de todas las dependencias) y en el classpath de ejecución; `assembleRelease` y
 * `bundleRelease` fallan si no se cumple.
 */
val permisosDeRed = listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE", "android.permission.ACCESS_WIFI_STATE")
val libreriasDeRed =
    listOf("com.squareup.okhttp", "com.squareup.retrofit", "io.ktor", "com.android.volley", "org.chromium.net", "com.google.firebase")

androidComponents {
    onVariants(selector().withBuildType("release")) { variante ->
        val manifiesto = variante.artifacts.get(SingleArtifact.MERGED_MANIFEST)
        val clases = configurations.named("${variante.name}RuntimeClasspath")
        val comprobar =
            tasks.register("verificarSinRed${variante.name.replaceFirstChar { it.uppercase() }}") {
                group = "verification"
                description = "T9.18: el ${variante.name} no tiene permisos ni librerías de red."
                inputs.file(manifiesto)
                doLast {
                    val texto = manifiesto.get().asFile.readText()
                    val permisos = permisosDeRed.filter { "\"$it\"" in texto }
                    check(permisos.isEmpty()) { "El manifiesto del ${variante.name} pide permisos de red: $permisos" }
                    val modulos =
                        clases
                            .get()
                            .incoming.resolutionResult.allComponents
                            .map { it.id.displayName }
                    val red = modulos.filter { m -> libreriasDeRed.any { m.startsWith(it) } }
                    check(red.isEmpty()) { "El ${variante.name} incluye librerías de red: $red" }
                    println("T9.18 ${variante.name}: sin permisos de red y sin librerías de red (${modulos.size} módulos revisados)")
                }
            }
        afterEvaluate {
            listOf("assemble", "bundle").forEach { accion ->
                tasks.named("$accion${variante.name.replaceFirstChar { it.uppercase() }}") { dependsOn(comprobar) }
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.haze)
    implementation(libs.reorderable)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}
