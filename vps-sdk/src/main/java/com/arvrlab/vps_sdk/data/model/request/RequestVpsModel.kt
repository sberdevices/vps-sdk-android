package com.arvrlab.vps_sdk.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RequestVpsModel(
    @Json(name = "data")
    val data: RequestDataModel
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