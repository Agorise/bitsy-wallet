package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import cy.agorise.bitsybitshareswallet.models.GeneralSetting

@Dao
interface GeneralSettingDao {

    @get:Query("SELECT * FROM general_setting")
    val all: LiveData<List<GeneralSetting>>

    @Query("SELECT * FROM general_setting WHERE name = :name")
    fun getByName(name: String): LiveData<GeneralSetting>

    @Query("SELECT * FROM general_setting WHERE name = :name")
    fun getSettingByName(name: String): GeneralSetting

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGeneralSettings(vararg generalSettings: GeneralSetting): LongArray

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGeneralSetting(generalSetting: GeneralSetting): Long

    @Delete
    fun deleteGeneralSettings(vararg generalSettings: GeneralSetting)

    @Query("DELETE FROM general_setting WHERE name = :name")
    fun deleteByName(name: String)
}
