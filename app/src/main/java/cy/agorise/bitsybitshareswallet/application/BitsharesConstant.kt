package cy.agorise.bitsybitshareswallet.application.constant

import android.content.Context
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.bitsybitshareswallet.models.BitsharesAsset
import cy.agorise.bitsybitshareswallet.models.BitsharesAssetInfo

object BitsharesConstant {
    val BITSHARES_URL = arrayOf(
            "wss://de.palmpay.io/ws",
            "wss://nl.palmpay.io/ws",
            "wss://mx.palmpay.io/ws",
            "wss://us.nodes.bitshares.ws/ws",
            "wss://eu.nodes.bitshares.ws/ws",
            "wss://sg.nodes.bitshares.ws/ws",
            "wss://dallas.bitshares.apasia.tech/ws"
    )


    val FAUCET_URL = "https://faucet.palmpay.io"

    val SMARTCOINS = arrayOf<BitsharesAsset>(
            BitsharesAsset("BTS", 5, "1.3.0", BitsharesAsset.Type.UIA),
        BitsharesAsset("USD", 4, "1.3.121", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("EUR", 4, "1.3.120", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("CNY", 4, "1.3.113", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("RUBLE", 5, "1.3.1325", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("AUD", 4, "1.3.117", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("SILVER", 4, "1.3.105", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("GOLD", 6, "1.3.106", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("JPY", 2, "1.3.119", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("CAD", 4, "1.3.115", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("MXN", 4, "1.3.114", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("GBP", 4, "1.3.118", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("ARS", 4, "1.3.1017", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("KRW", 4, "1.3.102", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("CHF", 4, "1.3.116", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("SEK", 4, "1.3.111", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("RUB", 4, "1.3.110", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("NZD", 4, "1.3.112", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("XCD", 4, "1.3.2650", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("TRY", 4, "1.3.107", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("HKD", 4, "1.3.109", BitsharesAsset.Type.SMART_COIN),
        BitsharesAsset("SGD", 4, "1.3.108", BitsharesAsset.Type.SMART_COIN)
    )

    fun addSmartCoins(context: Context) {
        val db = BitsyDatabase.getAppDatabase(context)
        for (smartcoin in SMARTCOINS) {
            if (db!!.cryptoCurrencyDao().getByName(smartcoin.name!!, CryptoNet.BITSHARES.name) == null) {
                db!!.cryptoCurrencyDao().insertCryptoCurrency(smartcoin)
            }
            val idCurrency = db!!.cryptoCurrencyDao().getByName(smartcoin.name!!, CryptoNet.BITSHARES.name).id
            val info = BitsharesAssetInfo(smartcoin)
            info.cryptoCurrencyId = idCurrency
            db.bitsharesAssetDao().insertBitsharesAssetInfo(info)
        }

    }

}



