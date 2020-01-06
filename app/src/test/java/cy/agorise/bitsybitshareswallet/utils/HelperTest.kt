package cy.agorise.bitsybitshareswallet.utils

import org.junit.Test
import org.junit.Assert.*
import java.util.*


class HelperTest {

    @Test
    fun getCoingeckoSupportedCurrency_InvalidInput_ReturnsUSD() {
        val locale = Locale("es")

        val currencyCode = Helper.getCoingeckoSupportedCurrency(locale)
        assertEquals("USD", currencyCode)
    }

    @Test
    fun getCoingeckoSupportedCurrency_UnsupportedInput_ReturnsUSD() {
        val locale = Locale("es", "PE")

        val currencyCode = Helper.getCoingeckoSupportedCurrency(locale)
        assertEquals("USD", currencyCode)
    }

    @Test
    fun getCoingeckoSupportedCurrency_SupportedInput_ReturnsItself() {
        val locale = Locale("es", "MX")

        val currencyCode = Helper.getCoingeckoSupportedCurrency(locale)
        assertEquals("MXN", currencyCode)
    }
}