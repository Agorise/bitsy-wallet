package cy.agorise.bitsybitshareswallet.enums

import java.io.Serializable
import java.util.HashMap





enum class CryptoNet private constructor(label: String, confirmationsNeeded: Int, bip44Index: Int) : Serializable {
    UNKNOWN("UNKNOWN", 6, -1), BITSHARES("BITSHARES", 1, 6);

    var label: String
        protected set

    var confirmationsNeeded: Int = 0
        protected set

    var bip44Index: Int = 0
        protected set

    init {
        this.label = label
        this.confirmationsNeeded = confirmationsNeeded
        this.bip44Index = bip44Index
    }

    companion object {

        private val bip44Map = HashMap<Int, CryptoNet>()

        init {
            for (cryptoNetEnum in CryptoNet.values()) {
                bip44Map[cryptoNetEnum.bip44Index] = cryptoNetEnum
            }
        }

        fun fromBip44Index(index: Int): CryptoNet? {
            return if (bip44Map.containsKey(index)) {
                bip44Map[index]
            } else {
                CryptoNet.UNKNOWN
            }
        }
    }
}
