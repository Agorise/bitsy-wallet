package cy.agorise.bitsybitshareswallet.requestmanagers

import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.enums.CryptoNetAccount
import cy.agorise.bitsybitshareswallet.models.CryptoCurrency

class ReceivedFundsCryptoNetEvent(var account: CryptoNetAccount?, var currency: CryptoCurrency?, var amount: Long) :
    CryptoNetEvent(CryptoCoin.BITSHARES)

