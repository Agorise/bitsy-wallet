package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "general_setting")
class GeneralSetting {

    /**
     * The id on the database
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0

    /**
     * The name of this setting
     */
    @ColumnInfo(name = "name")
    var name: String? = null

    /**
     * The value of this setting
     */
    @ColumnInfo(name = "value")
    var value: String? = null

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as GeneralSetting?

        if (id != that!!.id) return false
        return if (name !== that.name) false else value == that.value

    }

    companion object {

        val SETTING_NAME_PREFERRED_COUNTRY = "PREFERRED_COUNTRY"
        val SETTING_NAME_PREFERRED_CURRENCY = "PREFERRED_CURRENCY"
        val SETTING_NAME_PREFERRED_LANGUAGE = "PREFERRED_LANGUAGE"
        val SETTING_NAME_TIME_ZONE = "TIME_ZONE"
        val SETTING_PASSWORD = "PASSWORD"
        val SETTING_PATTERN = "PATTERN"
        val SETTING_NAME_RECEIVED_FUNDS_SOUND_PATH = "RECEIVED_FUNDS_SOUND_PATH"
        val SETTING_LAST_LICENSE_READ = "LAST_LICENSE_READ"
        val SETTING_YUBIKEY_OATH_TOTP_NAME = "YUBIKEY_OATH_TOTP_NAME"
        val SETTING_YUBIKEY_OATH_TOTP_PASSWORD = "YUBIKEY_OATH_TOTP_PASSWORD"

        val DIFF_CALLBACK: DiffUtil.ItemCallback<GeneralSetting> = object : DiffUtil.ItemCallback<GeneralSetting>() {
            override fun areItemsTheSame(
                @NonNull oldSetting: GeneralSetting, @NonNull newSetting: GeneralSetting
            ): Boolean {
                return oldSetting.id == newSetting.id
            }

            override fun areContentsTheSame(
                @NonNull oldSetting: GeneralSetting, @NonNull newSetting: GeneralSetting
            ): Boolean {
                return oldSetting == newSetting
            }
        }
    }
}
