package cy.agorise.bitsybitshareswallet.notifiers

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.models.GeneralSetting
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetEvent
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetEventsListener
import cy.agorise.bitsybitshareswallet.requestmanagers.ReceivedFundsCryptoNetEvent
import cy.agorise.bitsybitshareswallet.viewmodels.GeneralSettingListViewModel
import java.io.File
import java.io.IOException

class CrystalWalletNotifier(private val application: Application) : CryptoNetEventsListener {

    private val context: Context
    private var receivedFundsMediaPlayer: MediaPlayer? = null

    init {
        this.context = application.applicationContext
        loadReceivedFundsSound()
    }

    fun loadReceivedFundsSound() {
        val generalSettingListViewModel = GeneralSettingListViewModel(this.application)
        val receivedFundsSoundGeneralSetting =
            generalSettingListViewModel.getGeneralSettingByName(GeneralSetting.SETTING_NAME_RECEIVED_FUNDS_SOUND_PATH)

        var receivedFundsSoundFile: File? = null

        if (receivedFundsSoundGeneralSetting != null) {
            if (!receivedFundsSoundGeneralSetting!!.value.equals("")) {
                receivedFundsSoundFile = File(receivedFundsSoundGeneralSetting!!.value)

                if (!receivedFundsSoundFile!!.exists()) {
                    receivedFundsSoundFile = null
                }
            }
        }

        if (receivedFundsSoundFile != null) {
            receivedFundsMediaPlayer = MediaPlayer()
            try {
                receivedFundsMediaPlayer!!.setDataSource(receivedFundsSoundGeneralSetting!!.value)
                receivedFundsMediaPlayer!!.prepare()
            } catch (e: IOException) {
                receivedFundsMediaPlayer = MediaPlayer.create(this.context, R.raw.woohoo)
            }

        } else {
            receivedFundsMediaPlayer = MediaPlayer.create(this.context, R.raw.woohoo)
        }
    }

    override fun onCryptoNetEvent(event: CryptoNetEvent) {
        if (event is ReceivedFundsCryptoNetEvent) {
            playReceivedFundsSound()
        }
    }

    private fun playReceivedFundsSound() {
        receivedFundsMediaPlayer!!.start()
    }
}
