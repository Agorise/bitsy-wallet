package cy.agorise.bitsybitshareswallet.requestmanagers

import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.models.CryptoCurrency
import cy.agorise.bitsybitshareswallet.models.CryptoNetAccount

class ReceivedFundsCryptoNetEvent(var account: CryptoNetAccount?, var currency: CryptoCurrency?, var amount: Long) :
    CryptoNetEvent(CryptoCoin.BITSHARES)

