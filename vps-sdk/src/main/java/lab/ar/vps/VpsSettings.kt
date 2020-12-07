package lab.ar.vps

data class VpsSettings(
    val url: String,
    val locationID: String,
    var onlyForce: Boolean = true,
    val timerInterval: Long,
    val needLocation: Boolean
)