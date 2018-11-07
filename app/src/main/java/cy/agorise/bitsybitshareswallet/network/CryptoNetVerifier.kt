package cy.agorise.bitsybitshareswallet.network

import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.enums.CryptoNet

abstract class CryptoNetVerifier {

    abstract val chainId: String

    abstract fun checkURL(url: String)

    companion object {

        internal fun getNetworkVerify(cryptoNet: CryptoNet): CryptoNetVerifier? {
            if (cryptoNet.label.equals(CryptoNet.BITSHARES.label)) {
                return BitsharesCryptoNetVerifier()
            } else if (cryptoNet.label.equals(CryptoNet.BITCOIN.label)) {
                return BitcoinCryptoNetVerifier(CryptoCoin.BITCOIN)
            }
            return null
        }
    }
}