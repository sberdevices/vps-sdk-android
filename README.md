![logo](./img/logo.png)

# VPS SDK (Android)

![MavenCentral](https://img.shields.io/static/v1?label=Maven%20Central&message=v.0.5.0&color=success&style=flat)

This is **Visual Positioning System** SDK for native Android apps. Main features are:
- High-precision global user position localization for your AR apps
- Easy to use public API and premade Fragments
- Integration in [SceneForm](https://github.com/google-ar/sceneform-android-sdk) fork

For more information visit [our page on SmartMarket](https://developers.sber.ru/portal/tools/visual-positioning-system-sdk). If you want access to other VPS locations or want to scan your own proprerty, please contact us at <arvrlab@sberbank.ru>.

## Requirements
- Android SDK 24+
- ARCore supported device

## Installation

1. Open your project's `build.gradle`. Add `mavenCentral` repository if it doesn't exist:
    ```gradle
    allprojects {
        repositories {
            mavenCentral()
            ...
        }
    }
    ```

2. In your module's `build.gradle` add dependency:
    ```gradle
    dependencies {
        ...
        implementation "com.sberlabs:vps-sdk:0.5.0"
    }
    ```

3. Sync Project with Gradle Files


## Example

There is an example project in this repository. 

Just clone the repository and build it as a regular Android app. Make sure that your device support ARCore.

## Usage

### Android Manifest

Add this in `AndroidManifest.xml`, if android min sdk less than 24: 

```xml
<uses-sdk tools:overrideLibrary="com.arvrlab.vps_sdk, com.google.ar.sceneform.ux" />
```

By default `VPS SDK` has limited visibility in the Google Play Store to ARCore supported devices

```xml
<uses-feature
    android:name="android.hardware.camera.ar"
    android:required="true" />
```

To override visibility add this in your app's `AndroidManifest.xml`

```xml
<uses-feature
    android:name="android.hardware.camera.ar"
    android:required="false"
    tools:replace="android:required" />
```

### Using VpsArFragment

You can use build-in `VpsArFragment`. You can add into xml or by code:

*res/layout/main_activity.xml*
```xml
<androidx.fragment.app.FragmentContainerView
    android:id="@+id/vFragmentContainer"
    android:name="com.arvrlab.vps_sdk.ui.VpsArFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```
or

*src/main/java/…/MainActivity.kt*
```kotlin
supportFragmentManager.beginTransaction()
            .replace(R.id.vFragmentContainer, VpsArFragment())
            .commit()
```

### Setup VpsService

Create a config for `VpsService`:

```kotlin
val vpsConfig = VpsConfig(
                    <vpsUrl>,
                    <location_ID>
                )
```

Setup a `VpsService`:

```kotlin
val vpsService = vpsArFragment.vpsService

vpsService.setVpsConfig(vpsConfig)

//optional
vpsService.setVpsCallback(object : VpsCallback {
                override fun onSuccess() {
                }

                override fun onFail() {
                }

                override fun onStateChange(state: State) {
                }

                override fun onError(error: Throwable) {
                }
            })
```

Start `VpsService`:

```kotlin
vpsService.startVpsService()
```

Stop `VpsService`:

```kotlin
vpsService.stopVpsService()
```

### VpsService in a custom ArFragment

Create a new instance of `VpsService`:
```kotlin
VpsService.newInstance(): VpsService
```

You will also need to sync lifecycle of `VpsService` with lifecycle your `ArFragment`:
```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    vpsService.bindArSceneView(arSceneView)
}

override fun onResume() {
    super.onResume()
    vpsService.resume()
}

override fun onPause() {
    super.onPause()
    vpsService.pause()
}

override fun onDestroy() {
    super.onDestroy()
    vpsService.destroy()
}
```

After that you can use `VpsService` as mentioned above.


### Additional information about `VpsService`

You can add a custom 3D model using `worldNode` in `VpsService`

```kotlin
vpsService.worldNode
```

If `VpsService` is running then field `isRun` return `true`

## License

This project is licensed under [Sber Public License at-nc-sa v.2](LICENSE).

Google SceneForm library is licensed under [Apache License 2.0](https://github.com/google-ar/sceneform-android-sdk/blob/master/LICENSE).
