package ru.arvrlab.vps.service

import ru.arvrlab.vps.network.dto.ResponseDto
import java.lang.Exception

interface VpsCallback {
    fun onPositionVps(responseDto: ResponseDto)
    fun onError(error: Exception)
}