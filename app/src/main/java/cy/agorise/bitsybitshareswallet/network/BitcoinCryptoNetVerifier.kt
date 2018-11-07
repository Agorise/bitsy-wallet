package cy.agorise.bitsybitshareswallet.network

import cy.agorise.bitsybitshareswallet.apigenerator.insightapi.GetGenesisBlock
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin

class BitcoinCryptoNetVerifier(bitcoin: CryptoCoin) : CryptoNetVerifier() {


    override val chainId: String
        get() {
            return if (cryptoCoin == null || cryptoCoin!!.parameters == null) {
                null.toString()
            } else cryptoCoin!!.parameters.getGenesisBlock().getHashAsString()
        }


    internal var cryptoCoin: CryptoCoin? = null

    fun BitcoinCryptoNetVerifier(cryptoCoin: CryptoCoin) {
        this.cryptoCoin = cryptoCoin
    }


    override fun checkURL(url: String) {

        val startTime = System.currentTimeMillis()
        val genesisBloc = GetGenesisBlock(url, object : GetGenesisBlock.genesisBlockListener {
            override fun genesisBlock(value: String) {
                if (cryptoCoin!!.parameters != null) {
                    if (value == cryptoCoin!!.parameters.getGenesisBlock().getHashAsString()) {

                        CryptoNetManager.verifiedCryptoNetURL(
                            cryptoCoin!!.cryptoNet,
                            url,
                            System.currentTimeMillis() - startTime
                        )
                    }
                    //TODO bad genesis block
                } else {
                    CryptoNetManager.verifiedCryptoNetURL(
                        cryptoCoin!!.cryptoNet,
                        url,
                        System.currentTimeMillis() - startTime
                    )
                }
            }

            override fun fail() {
                //TODO failed
            }
        })
    }
}