plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    compileSdk = Version.compileSdk

    defaultConfig {
        applicationId = "com.arvrlab.vps_android_prototype"
        minSdk = Version.minSdk
        targetSdk = Version.targetSdk
        versionCode = 1
        versionName = "1.0"
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
    implementation(Lib.appcompat)
    implementation(Lib.material)

    implementation(Lib.viewBindingPropertyDelegate)

    implementation(Lib.lifecycleRuntimeKtx)
    implementation(Lib.lifecycleViewmodelKtx)
    implementation(Lib.lifecycleExtensions)

    implementation(Lib.osmdroid)
}