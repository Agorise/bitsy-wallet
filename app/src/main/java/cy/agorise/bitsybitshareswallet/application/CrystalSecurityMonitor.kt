package cy.agorise.bitsybitshareswallet.application

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import cy.agorise.bitsybitshareswallet.interfaces.OnResponse
import cy.agorise.bitsybitshareswallet.models.GeneralSetting
import cy.agorise.bitsybitshareswallet.viewmodels.GeneralSettingListViewModel

class CrystalSecurityMonitor : Application.ActivityLifecycleCallbacks {

    override fun onActivityStopped(p0: Activity?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivityStarted(p0: Activity?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var numStarted = 0
    private var passwordEncrypted: String? = null
    private var patternEncrypted: String? = null
    private var yubikeyOathTotpPasswordEncrypted: String? = null

    private var instance: CrystalSecurityMonitor? = null
    private lateinit var generalSettingListViewModel: GeneralSettingListViewModel


    constructor(fragmentActivity: FragmentActivity) {
        //For security reasons, this code is not asynchronous because the pattern or password data
        //must be retrieved before the principal activity starts
        generalSettingListViewModel =
                ViewModelProviders.of(fragmentActivity).get(GeneralSettingListViewModel::class.java)

        this.passwordEncrypted = ""
        this.patternEncrypted = ""
        this.yubikeyOathTotpPasswordEncrypted = ""
        val passwordGeneralSetting =
            generalSettingListViewModel.getGeneralSettingByName(GeneralSetting.SETTING_PASSWORD)
        val patternGeneralSetting = generalSettingListViewModel.getGeneralSettingByName(GeneralSetting.SETTING_PATTERN)
        val yubikeyOathTotpPasswordSetting =
            generalSettingListViewModel.getGeneralSettingByName(GeneralSetting.SETTING_YUBIKEY_OATH_TOTP_PASSWORD)

        if (passwordGeneralSetting != null) {
            this.passwordEncrypted = passwordGeneralSetting.value
        }
        if (patternGeneralSetting != null) {
            this.patternEncrypted = patternGeneralSetting.value
        }
        if (yubikeyOathTotpPasswordSetting != null) {
            this.yubikeyOathTotpPasswordEncrypted = yubikeyOathTotpPasswordSetting.value
        }
    }


    fun getInstance(fragmentActivity: FragmentActivity): CrystalSecurityMonitor {
        if (instance == null) {
            instance = CrystalSecurityMonitor(fragmentActivity)
        }

        return this!!.instance!!
    }

    fun getServiceName(): String {
        return "cy.agorise.crystalwallet"
    }


    fun clearSecurity() {
        this.patternEncrypted = ""
        this.passwordEncrypted = ""

        generalSettingListViewModel.deleteGeneralSettingByName(GeneralSetting.SETTING_PASSWORD)
        generalSettingListViewModel.deleteGeneralSettingByName(GeneralSetting.SETTING_PATTERN)
    }

    fun clearSecurity2ndFactor() {
        this.yubikeyOathTotpPasswordEncrypted = ""

        generalSettingListViewModel.deleteGeneralSettingByName(GeneralSetting.SETTING_YUBIKEY_OATH_TOTP_PASSWORD)
    }

    fun setPasswordSecurity(password: String) {
        clearSecurity()
        this.passwordEncrypted = password
        val passwordGeneralSetting = GeneralSetting()
        passwordGeneralSetting.name = GeneralSetting.SETTING_PASSWORD
        passwordGeneralSetting.value = password

        generalSettingListViewModel.saveGeneralSetting(passwordGeneralSetting)
    }

    fun setPatternEncrypted(pattern: String) {
        clearSecurity()
        this.patternEncrypted = pattern
        val patternGeneralSetting = GeneralSetting()
        patternGeneralSetting.name = GeneralSetting.SETTING_PATTERN
        patternGeneralSetting.value = pattern

        generalSettingListViewModel.saveGeneralSetting(patternGeneralSetting)
    }


    fun actualSecurity(): String {
        if (this.patternEncrypted != null && this.patternEncrypted != "") {
            return GeneralSetting.SETTING_PATTERN
        } else if (this.passwordEncrypted != null && this.passwordEncrypted != "") {
            return GeneralSetting.SETTING_PASSWORD
        }

        return ""
    }

    fun is2ndFactorSet(): Boolean {
        return this.yubikeyOathTotpPasswordEncrypted != ""
    }

    fun setYubikeyOathTotpSecurity(name: String, password: String) {
        this.yubikeyOathTotpPasswordEncrypted = password
        val yubikeyOathTotpSetting = GeneralSetting()
        yubikeyOathTotpSetting.name = GeneralSetting.SETTING_YUBIKEY_OATH_TOTP_PASSWORD
        yubikeyOathTotpSetting.value = password

        generalSettingListViewModel.saveGeneralSetting(yubikeyOathTotpSetting)
    }


    override fun onActivityCreated(activity: Activity, bundle: Bundle) {
        //
    }

    override fun onActivityResumed(activity: Activity) {
        //
    }

    override fun onActivityPaused(activity: Activity) {
        //
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        //
    }

    override fun onActivityDestroyed(activity: Activity) {
        //
    }

}