plugins {
    id(PluginName.application)
    id(PluginName.kotlinAndroid)
    id(PluginName.kotlinKapt)
    id(PluginName.navigationSafeargs)
}

android {
    compileSdk = Version.compileSdk

    defaultConfig {
        applicationId = "com.arvrlab.vps_android_prototype"
        minSdk = Version.minSdk
        targetSdk = Version.targetSdk
        versionCode = Version.versionCode
        versionName = Version.versionName
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
    viewBinding.isEnabled = true
}

dependencies {
    implementation(project(":vps-sdk"))

    implementation(Lib.coreKtx)
    implementation(Lib.fragmentKtx)
    implementation(Lib.appcompat)
    implementation(Lib.material)

    implementation(Lib.viewBindingPropertyDelegate)

    implementation(Lib.navigationRuntimeKtx)
    implementation(Lib.navigationFragmentKtx)
    implementation(Lib.navigationUiKtx)

    implementation(Lib.lifecycleRuntimeKtx)
    implementation(Lib.lifecycleViewmodelKtx)
    implementation(Lib.lifecycleExtensions)
}