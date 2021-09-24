VPS-SDK for Android
====================================

![GitLab Maven](https://img.shields.io/static/v1?label=Gitlab%20Maven&message=v.0.0.1&color=success&style=flat)

## Add VPS-SDK to a project

*build.gradle* or *settings.gradle*
```gradle
repositories {
        …
        maven {
            url 'https://gitlab.com/api/v4/projects/29278122/packages/maven'
            credentials(HttpHeaderCredentials) {
                name = "Deploy-Token"
                value = "khGe1_kixW-8sLtUBqgU"
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
      implementation "com.arvrlab.vps:vps-sdk:0.0.1"
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

### Add the `VpsArFragment` to your `screen`
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

### Work with `VpsService`

<br/>

*Create config for `VpsService`*

```kotlin
val vpsConfig = VpsConfig(
                    <url>,
                    <location_ID>,
                    <force>,            //optional
                    <request_interval>, //optional
                    <use_location>,     //optional
                    <use_neuro>         //optional
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

*You can set 3D model using `worldNode` in `VpsService`*

```kotlin
vpsService.worldNode
```