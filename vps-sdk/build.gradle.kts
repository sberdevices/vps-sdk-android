plugins {
    id(PluginName.library)
    id(PluginName.kotlinAndroid)
    id(PluginName.kotlinKapt)
}

android {
    compileSdk = Version.compileSdk

    defaultConfig {
        minSdk = Version.minSdk
        targetSdk = Version.targetSdk

        consumerProguardFile("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    api(project(":sceneformux"))

    implementation(Lib.coreKtx)
    implementation(Lib.fragmentKtx)
    implementation(Lib.lifecycleRuntimeKtx)

    implementation(Lib.coroutinesCore)
    implementation(Lib.coroutinesAndroid)

    implementation(Lib.okhttp)
    implementation(Lib.loggingInterceptor)

    implementation(Lib.retrofit)
    implementation(Lib.converterMoshi)
    implementation(Lib.retrofit2CoroutinesAdapter)

    implementation(Lib.moshiKotlin)
    implementation(Lib.moshiKotlinCodegen)

    implementation(Lib.playServicesLocation)

    implementation(Lib.tensorflowLite)
    implementation(Lib.tensorflowLiteGpu)
    implementation(Lib.tensorflowLiteSupport)
}