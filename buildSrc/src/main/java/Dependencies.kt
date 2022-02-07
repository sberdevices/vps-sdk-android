const val groupName = "com.sberlabs"

object Version {
    const val gradle = "7.0.2"
    const val kotlin = "1.5.30"

    const val compileSdk = 30
    const val minSdk = 24
    const val targetSdk = 30
}

object Plugin {
    const val gradle = "com.android.tools.build:gradle:${Version.gradle}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Version.kotlin}"
}

object Lib {
    const val coreKtx = "androidx.core:core-ktx:1.6.0"
    const val fragmentKtx = "androidx.fragment:fragment-ktx:1.3.6"
    const val appcompat = "androidx.appcompat:appcompat:1.3.1"
    const val material = "com.google.android.material:material:1.4.0"

    const val sceneform = "com.gorisse.thomas.sceneform:sceneform:1.20.2"

    private const val coroutines = "1.5.0"
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines"
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines"

    private const val lifecycle = "2.3.1"
    const val lifecycleRuntimeKtx = "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle"
    const val lifecycleViewmodelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle"
    const val lifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:2.2.0"

    const val koinAndroid = "io.insert-koin:koin-android:3.1.2"

    const val viewBindingPropertyDelegate =
        "com.github.kirich1409:viewbindingpropertydelegate-noreflection:1.5.0-beta01"

    private const val _okhttp3 = "4.6.0"
    const val okhttp = "com.squareup.okhttp3:okhttp:$_okhttp3"
    const val loggingInterceptor = "com.squareup.okhttp3:logging-interceptor:$_okhttp3"

    private const val _retrofit = "2.9.0"
    const val retrofit = "com.squareup.retrofit2:retrofit:$_retrofit"
    const val converterMoshi = "com.squareup.retrofit2:converter-moshi:$_retrofit"

    private const val moshi = "1.12.0"
    const val moshiKotlin = "com.squareup.moshi:moshi-kotlin:$moshi"
    const val moshiKotlinCodegen = "com.squareup.moshi:moshi-kotlin-codegen:$moshi"

    const val playServicesLocation = "com.google.android.gms:play-services-location:18.0.0"

    private const val tensorflow = "2.6.0"
    const val tensorflowLite = "org.tensorflow:tensorflow-lite:$tensorflow"
    const val tensorflowLiteGpu = "org.tensorflow:tensorflow-lite-gpu:$tensorflow"
    const val tensorflowLiteSupport = "org.tensorflow:tensorflow-lite-support:0.2.0"

    const val osmdroid = "org.osmdroid:osmdroid-android:6.1.11"

    const val junit = "junit:junit:4.13.2"
    const val robolectric = "org.robolectric:robolectric:4.6"
}