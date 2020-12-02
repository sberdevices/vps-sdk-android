package lab.ar.vps

import kotlinx.coroutines.CoroutineScope
import lab.ar.network.dto.ResponseDto

class Vpsservice(coroutineScope: CoroutineScope) {

    private val vps: Vps = Vps(coroutineScope)

    fun start() {

    }

    fun stop() {

    }

    fun enableForceLocalization(enabled: Boolean) {

    }

    fun localizeWithMockData(mockData: ResponseDto) {

    }


}