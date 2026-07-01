import java.net.Inet4Address
import java.net.NetworkInterface

plugins {
    alias(libs.plugins.android.application)
}

/**
 * Detecta automáticamente la IP local (WiFi/Ethernet) de esta PC al COMPILAR.
 * Así la app queda apuntando a la IP actual sin escribir nada: cada vez que le
 * das Run en Android Studio se hornea la IP vigente. Si cambias de red, solo
 * vuelves a compilar. Ignora adaptadores virtuales (VirtualBox/VMware/etc.).
 */
fun detectarIpLocal(): String {
    return try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .filter {
                val n = it.displayName.lowercase()
                listOf("virtualbox", "vmware", "hyper-v", "loopback", "vethernet", "docker")
                    .none { v -> n.contains(v) }
            }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull {
                it.isSiteLocalAddress &&
                    it.hostAddress?.startsWith("192.168.56.") == false // salta la red por defecto de VirtualBox
            }
            ?.hostAddress ?: "10.0.2.2"
    } catch (e: Exception) {
        "10.0.2.2" // respaldo: localhost del emulador
    }
}

android {
    namespace = "com.petfeeder.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.petfeeder.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // IP de la PC detectada automáticamente al compilar -> la app la usa por defecto
        buildConfigField("String", "API_BASE_URL", "\"http://${detectarIpLocal()}:5172/\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Retrofit + OkHttp (red y API)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Corrutinas (para llamadas asincronas sin congelar la UI)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}