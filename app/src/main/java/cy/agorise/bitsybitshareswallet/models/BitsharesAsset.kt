package cy.agorise.bitsybitshareswallet.models

import cy.agorise.bitsybitshareswallet.enums.CryptoNet

class BitsharesAsset : CryptoCurrency {

    // The bitshares internal id
    var bitsharesId: String? = null
    // The bitshares type see the enum below
    var assetType: Type? = null

    /**
     * For representing each type of asset in the bitshares network
     */
    enum class Type
    /**
     * The code is used for be stored in the database
     */
    private constructor(val code: Int) {
        // The core asset aka BTS
        CORE(0),
        // the smartcoin assets, like bitEUR, bitUSD
        SMART_COIN(1),
        // The UIA assets type
        UIA(2),
        //THe prediction market type
        PREDICTION_MARKET(3)
    }

    constructor(symbol: String, precision: Int, bitsharesId: String, assetType: Type) {
        this.bitsharesId = bitsharesId
        this.assetType = assetType
        this.cryptoNet = CryptoNet.BITSHARES
        this.name = symbol
        this.precision = precision
    }

    constructor(currency: CryptoCurrency) {
        this.id  = currency.id
        this.precision = currency.precision
        this.cryptoNet = currency.cryptoNet
        this.name = currency.name
    }

    fun loadInfo(info: BitsharesAssetInfo) {
        this.bitsharesId = info.bitsharesId
        this.assetType = info.assetType
    }
}
