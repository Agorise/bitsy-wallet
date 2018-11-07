package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.models.CryptoCurrency

class CryptoNetEquivalentRequest
/**
 * Basic Constructor
 */
    (
    coin: CryptoCoin,
    /**
     * The android context of this application
     */
    var context: Context?,
    /**
     * The base currency
     */
    var fromCurrency: CryptoCurrency?,
    /**
     * The to currency
     */
    var toCurrency: CryptoCurrency?
) : CryptoNetInfoRequest(coin) {
    /**
     * The answer, less than 0 is an error, or no answer
     */
    private var price: Long = -1

    /**
     * Answer of the apigenerator
     * @param price The fetched equivalent value
     */
    fun setPrice(price: Long) {
        this.price = price
        this._fireOnCarryOutEvent()
    }
}

