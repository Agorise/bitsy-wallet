package cy.agorise.bitsybitshareswallet.dao.converters

import androidx.room.TypeConverter
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.bitsybitshareswallet.enums.CryptoNetAccount
import cy.agorise.bitsybitshareswallet.enums.SeedType
import cy.agorise.bitsybitshareswallet.models.BitsharesAsset
import java.util.*

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun cryptoNetAccountToId(account: CryptoNetAccount?): Long {
        return if (account == null) {
            -1
        } else {
            account!!.id
        }
    }

    @TypeConverter
    fun fromCryptoNetAccountId(value: Long): CryptoNetAccount? {
        if (value as Int == -1) {
            return null
        } else {
            val account = CryptoNetAccount()
            account.id = value
            return account
        }
    }

    @TypeConverter
    fun cryptoCoinToName(coin: CryptoCoin?): String {
        return if (coin == null) {
            ""
        } else {
            coin!!.name
        }
    }

    @TypeConverter
    fun nameToCryptoCoin(value: String): CryptoCoin? {
        return if (value == "") {
            null
        } else {
            CryptoCoin.valueOf(value)
        }
    }

    @TypeConverter
    fun cryptoNetToName(net: CryptoNet?): String {
        return if (net == null) {
            ""
        } else {
            net!!.name
        }
    }

    @TypeConverter
    fun nameToCryptoNet(value: String): CryptoNet? {
        return if (value == "") {
            null
        } else {
            CryptoNet.valueOf(value)
        }
    }

    @TypeConverter
    fun seedTypeToName(value: SeedType?): String {
        return if (value == null) {
            ""
        } else {
            value!!.name
        }
    }

    @TypeConverter
    fun nameToSeedType(value: String): SeedType? {
        return if (value == "") {
            null
        } else {
            SeedType.valueOf(value)
        }
    }

    @TypeConverter
    fun cryptoNetToAccountNumber(value: CryptoNet?): Int {
        return if (value == null) {
            -1
        } else {
            value!!.bip44Index
        }
    }

    @TypeConverter
    fun accountNumberToCryptoNet(value: Int): CryptoNet {
        return CryptoNet.fromBip44Index(value)!!
    }

    @TypeConverter
    fun assetTypeToName(type: BitsharesAsset.Type?): String {
        return if (type == null) {
            ""
        } else type!!.name
    }

    @TypeConverter
    fun nameToAssetType(value: String): BitsharesAsset.Type {
        return BitsharesAsset.Type.valueOf(value)
    }
}
