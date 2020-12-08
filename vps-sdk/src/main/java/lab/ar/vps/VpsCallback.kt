package lab.ar.vps

import lab.ar.network.dto.ResponseDto
import java.lang.Exception

interface VpsCallback {
    fun onPositionVps(responseDto: ResponseDto)
    fun onError(error: Exception)
    fun onVpsServiceWasNotStarted()
}