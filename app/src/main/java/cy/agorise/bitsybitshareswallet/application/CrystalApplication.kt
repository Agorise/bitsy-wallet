package cy.agorise.bitsybitshareswallet.application

import android.app.Application
import android.content.Intent
import cy.agorise.bitsybitshareswallet.dao.CrystalDatabase
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.bitsybitshareswallet.models.BitsharesAsset
import cy.agorise.bitsybitshareswallet.models.BitsharesAssetInfo
import cy.agorise.bitsybitshareswallet.models.CryptoCurrency
import cy.agorise.bitsybitshareswallet.models.GeneralSetting
import cy.agorise.bitsybitshareswallet.network.CryptoNetManager
import cy.agorise.bitsybitshareswallet.notifiers.CrystalWalletNotifier
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetEvents
import cy.agorise.bitsybitshareswallet.service.CrystalWalletService
import java.util.*

class CrystalApplication : Application() {
    private val locale: Locale? = null

    override fun onCreate() {
        super.onCreate()

        //initialize the database
        val db = CrystalDatabase.getAppDatabase(this.applicationContext)
        //SqlScoutServer.create(this, packageName)

        //Using Bitshares Agorise Testnet
        //CryptoNetManager.addCryptoNetURL(CryptoNet.BITSHARES,BITSHARES_TESTNET_URL);

        //This is for testing the equivalent values on the testnet TODO remove
        if (db!!.bitsharesAssetDao().getBitsharesAssetInfoById(bitEURAsset.bitsharesId!!) == null) {
            if (db!!.cryptoCurrencyDao().getByName(bitEURAsset.name!!, bitEURAsset.cryptoNet!!.name) == null) {
                db.cryptoCurrencyDao().insertCryptoCurrency(bitEURAsset)
            }
            val idCurrency =
                db.cryptoCurrencyDao().getByName(bitEURAsset.name!!, bitEURAsset.cryptoNet!!.name).id
            val info = BitsharesAssetInfo(bitEURAsset)
            info.cryptoCurrencyId = idCurrency
            db.bitsharesAssetDao().insertBitsharesAssetInfo(info)

        }

        //This is for testing the equivalent values on the testnet TODO remove
        if (db.bitsharesAssetDao().getBitsharesAssetInfoById(bitUSDAsset.bitsharesId!!) == null) {
            if (db.cryptoCurrencyDao().getByName(bitUSDAsset.name!!, bitUSDAsset.cryptoNet!!.name) == null) {
                db.cryptoCurrencyDao().insertCryptoCurrency(bitUSDAsset)
            }
            val idCurrency =
                db.cryptoCurrencyDao().getByName(bitUSDAsset.name!!, bitUSDAsset.cryptoNet!!.name).id
            val info = BitsharesAssetInfo(bitUSDAsset)
            info.cryptoCurrencyId = idCurrency
            db.bitsharesAssetDao().insertBitsharesAssetInfo(info)

        }

        //The crystal notifier is initialized
        val crystalWalletNotifier = CrystalWalletNotifier(this)
        CryptoNetEvents.instance!!.addListener(crystalWalletNotifier)

        //Next line is for use the bitshares main net
        // TODO fix, the following line accepts one string not an array it needs to accept an arrey
        // TODO and hoop over the urls if no connection can be established
        CryptoNetManager.addCryptoNetURL(CryptoNet.BITSHARES, BITSHARES_URL)

        //Adding Bitcoin info
        CryptoNetManager.addCryptoNetURL(CryptoNet.BITCOIN, BITCOIN_SERVER_URLS)

        if (db.cryptoCurrencyDao().getByName(
                BITCOIN_CURRENCY.name!!,
                BITCOIN_CURRENCY.cryptoNet!!.name
            ) == null
        ) {
            db.cryptoCurrencyDao().insertCryptoCurrency(BITCOIN_CURRENCY)
        }


        val generalSettingPreferredLanguage =
            db.generalSettingDao().getSettingByName(GeneralSetting.SETTING_NAME_PREFERRED_LANGUAGE)

        if (generalSettingPreferredLanguage != null) {
            val resources = baseContext.resources
            val locale = Locale(generalSettingPreferredLanguage!!.value)
            Locale.setDefault(locale)
            val dm = resources.displayMetrics
            val configuration = resources.configuration
            configuration.locale = locale
            resources.updateConfiguration(configuration, dm)
        }

        val intent = Intent(applicationContext, CrystalWalletService::class.java)
        startService(intent)
    }

    companion object {

        var BITSHARES_URL = arrayOf(
            "wss://de.palmpay.io/ws",
            "wss://nl.palmpay.io/ws",
            "wss://mx.palmpay.io/ws",
            "wss://us.nodes.bitshares.ws/ws",
            "wss://eu.nodes.bitshares.ws/ws",
            "wss://sg.nodes.bitshares.ws/ws",
            "wss://dallas.bitshares.apasia.tech/ws"
        )

        //This is for testing the equivalent values on the testnet TODO remove
        var bitUSDAsset = BitsharesAsset("USD", 4, "1.3.121", BitsharesAsset.Type.SMART_COIN)
        //This is for testing the equivalent values on the testnet TODO remove
        var bitEURAsset = BitsharesAsset("EUR", 4, "1.3.120", BitsharesAsset.Type.SMART_COIN)


        val BITCOIN_SERVER_URLS = arrayOf("https://insight.bitpay.com/")

        val BITCOIN_CURRENCY = CryptoCurrency("BTC", CryptoNet.BITCOIN, 8)
    }
}
