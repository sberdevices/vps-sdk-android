VPS-SDK for Android
====================================

![GitLab Maven](https://img.shields.io/static/v1?label=Gitlab%20Maven&message=v.0.2.0&color=success&style=flat)

## Add VPS-SDK to a project

*build.gradle* or *settings.gradle*
```gradle
repositories {
        …
        maven {
            url 'https://gitlab.arvr.sberlabs.com/api/v4/projects/58/packages/maven'
            credentials(HttpHeaderCredentials) {
                name = "Deploy-Token"
                value = "3VBiQyK9wwnkHmvd-gT7"
            }
            authentication {
                header(HttpHeaderAuthentication)
            }
        }
    }
```

*app/build.gradle*
```gradle
dependencies {
      …
      implementation "com.arvrlab.vps:vps-sdk:0.2.0"
}
```

<br/>

## Setup VPS-SDK

### Update your `AndroidManifest.xml`

<br/>

**If android min sdk less 24 then need add to `AndroidManifest.xml`**

```xml
<uses-sdk tools:overrideLibrary="com.arvrlab.vps_sdk, com.google.ar.sceneform.ux" />
```

<br/>

**By default `VPS-SDK` has limits visibility in the Google Play Store to ARCore supported devices**

```xml
<uses-feature
    android:name="android.hardware.camera.ar"
    android:required="true" />
```

*For override limits visibility adding in your app's `AndroidManifest.xml`*

```xml
<uses-feature
    android:name="android.hardware.camera.ar"
    android:required="false"
    tools:replace="android:required" />
```

<br/>

## Add the `VpsArFragment` to your `screen`
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

<br/>

## Work with `VpsService` if using `VpsArFragment`

*Create config for `VpsService`*

```kotlin
val vpsConfig = VpsConfig(
                    <vpsUrl>,
                    <location_ID>,
                    <onlyForce>,                //optional, default true
                    <intervalLocalizationMS>,   //optional, default 6000
                    <useGps>,                   //optional, default false
                    <localizationType>,         //optional, default Photo [Photo, MobileVps]
                    <countImages>,              //optional, default 1
                    <intervalImagesMS>          //optional, default 1000
                )
```

*Setup `VpsService`*

```kotlin
val vpsService = vpsArFragment.vpsService

vpsService.setVpsConfig(vpsConfig)

//optional
vpsService.setVpsCallback(object : VpsCallback {
                override fun onSuccess() {
                }

                override fun onFail() {
                }

                override fun onStateChange(isEnable: Boolean) {
                }

                override fun onError(error: Throwable) {
                }
            })
```

*Start `VpsService`*

```kotlin
vpsService.startVpsService()
```

*Stop `VpsService`*

```kotlin
vpsService.startVpsService()
```

<br/>

## Work with `VpsService` if using own `ArFragment`

*Create new instance `VpsService`*
```kotlin
VpsService.newInstance(context: Context): VpsService
```

*After need sync lifecycle of `VpsService` with lifecycle your `ArFragment`.*
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

override fun onDestroyView() {
    super.onDestroyView()
    vpsService.unbindArSceneView()
}

override fun onDestroy() {
    super.onDestroy()
    vpsService.destroy()
}
```

*And then `VpsService` can use as mentioned above.*

<br/>

## Additional information about `VpsService`

*You can set 3D model using `worldNode` in `VpsService`*

```kotlin
vpsService.worldNode
```

*If `VpsService` is running then field `isRun` return `true`*