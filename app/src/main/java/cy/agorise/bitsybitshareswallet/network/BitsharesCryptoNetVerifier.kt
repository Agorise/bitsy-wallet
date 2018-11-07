package cy.agorise.bitsybitshareswallet.network

import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.graphenej.interfaces.WitnessResponseListener
import cy.agorise.graphenej.models.BaseResponse
import cy.agorise.graphenej.models.WitnessResponse

class BitsharesCryptoNetVerifier : CryptoNetVerifier() {
    private val cryptoNet = CryptoNet.BITSHARES
    override val chainId = "4018d7844c78f6a6c41c6a552b898022310fc5dec06da467ee7905a8dad512c8"//mainnet

    override fun checkURL(url: String) {
        val startTime = System.currentTimeMillis()
        val thread = WebSocketThread(GetChainId(object : WitnessResponseListener {
            override fun onSuccess(response: WitnessResponse<*>) {
                if (response.result is String) {
                    if (response.result == chainId) {
                        CryptoNetManager.verifiedCryptoNetURL(cryptoNet, url, System.currentTimeMillis() - startTime)
                    } else {
                        println(" BitsharesCryptoNetVerifier Error we are not in the net current chain id " + response.result + " excepted " + chainId)
                        //TODO handle error bad chain
                    }
                }
            }

            override fun onError(error: BaseResponse.Error) {
                //TODO handle error
                println("Bad server response $url")
            }
        }), url)
        thread.start()
    }
}