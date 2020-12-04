package lab.ar.vps

import lab.ar.network.dto.ResponseDto
import java.lang.Exception

interface VpsCallback {
    fun positionVps(responseDto: ResponseDto)
    fun error(error: Exception)
}