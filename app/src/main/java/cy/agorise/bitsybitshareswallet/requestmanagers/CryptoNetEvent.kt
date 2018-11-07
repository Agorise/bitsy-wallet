package cy.agorise.bitsybitshareswallet.requestmanagers

import cy.agorise.bitsybitshareswallet.enums.CryptoCoin

abstract class CryptoNetEvent protected constructor(
    /**
     * The cryptocoin this events belongs to
     */
    protected var coin: CryptoCoin
)
