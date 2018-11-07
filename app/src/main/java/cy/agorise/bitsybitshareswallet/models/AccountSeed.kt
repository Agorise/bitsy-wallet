package cy.agorise.bitsybitshareswallet.models

import android.content.Context
import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cy.agorise.bitsybitshareswallet.enums.SeedType
import cy.agorise.bitsybitshareswallet.models.seed.BIP39
import cy.agorise.graphenej.BrainKey
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.HDKeyDerivation
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

@Entity(tableName = "account_seed")
open class AccountSeed {

    /**
     * The id on the database
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0

    /**
     * The name or tag of this seed
     */
    @ColumnInfo(name = "name")
    var name: String? = null

    /**
     * The bytes of the master seed
     */
    @ColumnInfo(name = "master_seed")
    var masterSeed: String? = null

    /**
     * The type of this seed: BIP39, BRAINKEY
     */
    var type: SeedType? = null

    val privateKey: ECKey?
        get() {
            when (this.type) {
                SeedType.BRAINKEY -> return BrainKey(this.masterSeed, 0).privateKey
                SeedType.BIP39 -> return HDKeyDerivation.createMasterPrivateKey(BIP39(id, this!!.masterSeed!!).seed)
            }
            return null
        }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as AccountSeed?

        return if (id != that!!.id) false else masterSeed == that.masterSeed

    }

    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<AccountSeed> = object : DiffUtil.ItemCallback<AccountSeed>() {
            override fun areItemsTheSame(
                @NonNull oldAccountSeed: AccountSeed, @NonNull newAccountSeed: AccountSeed
            ): Boolean {
                return oldAccountSeed.id == newAccountSeed.id
            }

            override fun areContentsTheSame(
                @NonNull oldAccountSeed: AccountSeed, @NonNull newAccountSeed: AccountSeed
            ): Boolean {
                return oldAccountSeed == newAccountSeed
            }
        }

        fun getAccountSeed(type: SeedType, context: Context): AccountSeed? {
            var reader: BufferedReader? = null
            when (type) {
                SeedType.BRAINKEY -> {


                    try {
                        reader = BufferedReader(InputStreamReader(context.assets.open("brainkeydict.txt"), "UTF-8"))

                        val dictionary = reader.readLine()

                        val brainKeySuggestion = BrainKey.suggest(dictionary)
                        val seed = AccountSeed()
                        seed.masterSeed = brainKeySuggestion
                        seed.type = SeedType.BRAINKEY
                        return seed
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    try {
                        reader = BufferedReader(InputStreamReader(context.assets.open("bip39dict.txt"), "UTF-8"))
                        val dictionary = reader.readLine()
                        //TODO save in db
                        return BIP39(dictionary.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
                SeedType.BIP39 -> try {
                    reader = BufferedReader(InputStreamReader(context.assets.open("bip39dict.txt"), "UTF-8"))
                    val dictionary = reader.readLine()
                    return BIP39(dictionary.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            return null
        }
    }
}
