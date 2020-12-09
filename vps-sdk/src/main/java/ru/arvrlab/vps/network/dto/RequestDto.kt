package ru.arvrlab.vps.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
data class RequestDto(
    @Json(name = "data")
    var data: RequestDataDto
)

@JsonClass(generateAdapter = true)
data class RequestDataDto(
    @Json(name = "attributes")
    var attributes: RequestAttributesDto = RequestAttributesDto(),
    @Json(name = "id")
    var jobId: String = UUID.randomUUID().toString(),
    @Json(name = "type")
    var type: String = "job"
)

@JsonClass(generateAdapter = true)
data class RequestAttributesDto(
    @Json(name = "forced_localization")
    var forcedLocalisation: Boolean = true,
    @Json(name = "imageTransform")
    var imageTransform: RequestImageTransformDto = RequestImageTransformDto(),
    @Json(name = "intrinsics")
    var intrinsics: RequestIntrinsicsDto = RequestIntrinsicsDto(),
    @Json(name = "location")
    var location: RequestLocationDto = RequestLocationDto()
)

@JsonClass(generateAdapter = true)
data class RequestImageTransformDto(
    @Json(name = "mirrorX")
    var mirrorX: Boolean = false,
    @Json(name = "mirrorY")
    var mirrorY: Boolean = false,
    @Json(name = "orientation")
    var orientation: Int = 0
)

@JsonClass(generateAdapter = true)
data class RequestIntrinsicsDto(
    @Json(name = "cx")
    var cx: Float = 0.0f,
    @Json(name = "cy")
    var cy: Float = 0.0f,
    @Json(name = "fx")
    var fx: Float = 0.0f,
    @Json(name = "fy")
    var fy: Float = 0.0f
)

@JsonClass(generateAdapter = true)
data class RequestLocationDto(
    @Json(name = "clientCoordinateSystem")
    var clientCoordinateSystem: String = "arkit",
    @Json(name = "compass")
    var compass: RequestCompassDto = RequestCompassDto(),
    @Json(name = "gps")
    var gps: RequestGpsDto? = null,
    @Json(name = "localPos")
    var localPos: RequestLocalPosDto = RequestLocalPosDto(),
    @Json(name = "location_id")
    var locationId: String = "Polytech",
    @Json(name = "type")
    var type: String = "relative"
)

@JsonClass(generateAdapter = true)
data class RequestCompassDto(
    @Json(name = "accuracy")
    var accuracy: Float = 0.0f,
    @Json(name = "heading")
    var heading: Float = 0.0f,
    @Json(name = "timestamp")
    var timestamp: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class RequestGpsDto(
    @Json(name = "accuracy")
    var accuracy: Double = 0.0,
    @Json(name = "altitude")
    var altitude: Double = 0.0,
    @Json(name = "latitude")
    var latitude: Double = 0.0,
    @Json(name = "longitude")
    var longitude: Double = 0.0,
    @Json(name = "timestamp")
    var timestamp: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class RequestLocalPosDto(
    @Json(name = "pitch")
    var pitch: Float = 0.0f,
    @Json(name = "roll")
    var roll: Float = 0.0f,
    @Json(name = "x")
    var x: Float = 0.0f,
    @Json(name = "y")
    var y: Float = 0.0f,
    @Json(name = "yaw")
    var yaw: Float = 0.0f,
    @Json(name = "z")
    var z: Float = 0.0f
)

//{
//    "data":
//    {
//        "id": "1234556",
//        "type": "job",
//        "attributes":
//        {
//            "location":
//            {
//                "type": "absolute",
//                "location_id": "eeb38592-4a3c-4d4b-b4c6-38fd68331521",
//                "gps":
//                {
//                    "latitude": 0.0,
//                    "longitude": 0.0,
//                    "altitude": 0.0,
//                    "accuracy": 0.0,
//                    "timestamp": 0.0
//                },
//                "compass":
//                {
//                    "heading": 0.6,
//                    "accuracy": 0.6,
//                    "timestamp": 0.6
//                },
//                "clientCoordinateSystem": "unity",
//                "localPos":
//                {
//                    "x": 0.0,
//                    "y": 0.0,
//                    "z": 0.0,
//                    "roll": 0.0,
//                    "pitch": 0.0,
//                    "yaw": 0.0
//                }
//            },
//            "imageTransform":
//            {
//                "orientation": 0,
//                "mirrorX" : false,
//                "mirrorY" : true
//            },
//            "intrinsics":
//            {
//                "fx": 0.0,
//                "fy": 0.0,
//                "cx": 0.0,
//                "cy": 0.0
//            },
//            "forced_localization": true
//        }
//    }
//}