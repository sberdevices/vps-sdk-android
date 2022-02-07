plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}
apply(from = rootProject.file("publishing.gradle"))

android {
    compileSdk = Version.compileSdk

    defaultConfig {
        minSdk = Version.minSdk
        targetSdk = Version.targetSdk

        consumerProguardFile("consumer-rules.pro")

        group = groupName
        version = project.property("vps_sdk_version") as String
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    aaptOptions.noCompress("tflite")
}

dependencies {
    api(Lib.sceneform)

    implementation(Lib.coreKtx)
    implementation(Lib.appcompat)
    implementation(Lib.material)
    implementation(Lib.fragmentKtx)
    implementation(Lib.lifecycleRuntimeKtx)

    implementation(Lib.coroutinesCore)
    implementation(Lib.coroutinesAndroid)

    implementation(Lib.koinAndroid)

    implementation(Lib.okhttp)
    implementation(Lib.loggingInterceptor)

    implementation(Lib.retrofit)
    implementation(Lib.converterMoshi)

    implementation(Lib.moshiKotlin)
    implementation(Lib.moshiKotlinCodegen)

    implementation(Lib.playServicesLocation)

    implementation(Lib.tensorflowLite)
    implementation(Lib.tensorflowLiteGpu)
    implementation(Lib.tensorflowLiteSupport)

    testImplementation(Lib.junit)
    testImplementation(Lib.robolectric)
}