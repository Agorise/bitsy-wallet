package cy.agorise.bitsybitshareswallet.network

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFrame
import cy.agorise.graphenej.RPC
import cy.agorise.graphenej.api.BaseGrapheneHandler
import cy.agorise.graphenej.interfaces.WitnessResponseListener
import cy.agorise.graphenej.models.ApiCall
import cy.agorise.graphenej.models.WitnessResponse
import java.io.Serializable
import java.util.ArrayList

class GetChainId(private val mListener_: WitnessResponseListener) : BaseGrapheneHandler(mListener_) {

    @Throws(Exception::class)
    override fun onConnected(websocket: WebSocket?, headers: Map<String, List<String>>?) {
        val getAccountByName = ApiCall(0, "get_chain_id", ArrayList(), RPC.VERSION, 1)
        websocket!!.sendText(getAccountByName.toJsonString())
    }

    @Throws(Exception::class)
    override fun onTextFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        println("<<< " + frame!!.payloadText)
        val response = frame.payloadText

        val GetChainIdResponse = object : TypeToken<WitnessResponse<String>>() {

        }.type
        val builder = GsonBuilder()
        val witnessResponse = builder.create().fromJson<WitnessResponse<List<String>>>(response, GetChainIdResponse)
        if (witnessResponse.error != null) {
            this.mListener_.onError(witnessResponse.error)
        } else {
            this.mListener_.onSuccess(witnessResponse)
        }

        websocket!!.disconnect()
    }

    @Throws(Exception::class)
    override fun onFrameSent(websocket: WebSocket?, frame: WebSocketFrame?) {
        if (frame!!.isTextFrame)
            println(">>> " + frame.payloadText)
    }
}
