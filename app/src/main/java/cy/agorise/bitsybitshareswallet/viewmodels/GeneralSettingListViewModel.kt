package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.models.GeneralSetting

class GeneralSettingListViewModel(application: Application) : AndroidViewModel(application) {

    val generalSettingList: LiveData<List<GeneralSetting>>
    private val db: BitsyDatabase

    init {
        this.db = BitsyDatabase.getAppDatabase(application.applicationContext)!!
        generalSettingList = this.db.generalSettingDao().all
    }

    fun saveGeneralSetting(generalSetting: GeneralSetting) {
        this.db.generalSettingDao().insertGeneralSetting(generalSetting)
    }

    fun saveGeneralSettings(generalSettings: GeneralSetting) {
        this.db.generalSettingDao().insertGeneralSettings(generalSettings)
    }

    fun getGeneralSettingByName(name: String): GeneralSetting {
        return this.db.generalSettingDao().getSettingByName(name)
    }

    fun getGeneralSettingLiveDataByName(name: String): LiveData<GeneralSetting> {
        return this.db.generalSettingDao().getByName(name)
    }

    fun deleteGeneralSettings(generalSettings: GeneralSetting) {
        this.db.generalSettingDao().deleteGeneralSettings(generalSettings)
    }

    fun deleteGeneralSettingByName(name: String) {
        this.db.generalSettingDao().deleteByName(name)
    }

    //public void addGeneralSetting(String name, String value){
    //    this.db.generalSettingDao().addGeneralSetting(name,value);
    //}
}
