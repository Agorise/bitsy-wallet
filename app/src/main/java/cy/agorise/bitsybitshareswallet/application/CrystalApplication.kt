package cy.agorise.bitsybitshareswallet.application

import android.app.Application
import android.content.Intent
import cy.agorise.bitsybitshareswallet.application.constant.BitsharesConstant
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.bitsybitshareswallet.models.BitsharesAsset
import cy.agorise.bitsybitshareswallet.models.BitsharesAssetInfo
import cy.agorise.bitsybitshareswallet.models.CryptoCurrency
import cy.agorise.bitsybitshareswallet.models.GeneralSetting
import cy.agorise.bitsybitshareswallet.network.CryptoNetManager
import cy.agorise.bitsybitshareswallet.notifiers.CrystalWalletNotifier
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetEvents
import cy.agorise.bitsybitshareswallet.service.CrystalWalletService
import org.spongycastle.asn1.x500.style.RFC4519Style.name
import java.util.*

class CrystalApplication : Application() {
    private val locale: Locale? = null

    override fun onCreate() {
        super.onCreate()

        //initialize the database
        val db = BitsyDatabase.getAppDatabase(this.applicationContext)
        //SqlScoutServer.create(this, packageName)

        //The crystal notifier is initialized
        val crystalWalletNotifier = CrystalWalletNotifier(this)
        CryptoNetEvents.getInstance()!!.addListener(crystalWalletNotifier)

        //Next line is for use the bitshares main net
        // TODO fix, the following line accepts one string not an array it needs to accept an arrey
        // TODO and hoop over the urls if no connection can be established
        CryptoNetManager.addCryptoNetURL(CryptoNet.BITSHARES, BitsharesConstant.BITSHARES_URL)

        BitsharesConstant.addSmartCoins(this.applicationContext)

        val generalSettingPreferredLanguage =
            db!!.generalSettingDao().getSettingByName(GeneralSetting.SETTING_NAME_PREFERRED_LANGUAGE)

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
}
