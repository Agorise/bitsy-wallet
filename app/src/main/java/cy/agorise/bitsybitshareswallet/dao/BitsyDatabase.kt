package cy.agorise.bitsybitshareswallet.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cy.agorise.bitsybitshareswallet.models.*

@Database(entities = arrayOf(AccountSeed::class,
        CryptoNetAccount::class,
 CryptoCoinTransaction::class,
         Contact::class,
ContactAddress::class,
CryptoCurrency::class,
CryptoCoinBalance::class,
GrapheneAccountInfo::class,
BitsharesAssetInfo::class,
BitsharesAccountNameCache::class,
CryptoCurrencyEquivalence::class,
GeneralSetting::class), version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class BitsyDatabase: RoomDatabase() {

abstract fun accountSeedDao():AccountSeedDao
abstract fun cryptoNetAccountDao():CryptoNetAccountDao
abstract fun grapheneAccountInfoDao():GrapheneAccountInfoDao
abstract fun transactionDao():TransactionDao
abstract fun contactDao():ContactDao
abstract fun cryptoCoinBalanceDao():CryptoCoinBalanceDao
abstract fun cryptoCurrencyDao():CryptoCurrencyDao
abstract fun bitsharesAssetDao():BitsharesAssetDao
abstract fun bitsharesAccountNameCacheDao():BitsharesAccountNameCacheDao
abstract fun cryptoCurrencyEquivalenceDao():CryptoCurrencyEquivalenceDao
 abstract fun generalSettingDao(): GeneralSettingDao

 companion object {

private var instance:BitsyDatabase? = null

 fun getAppDatabase(context: Context):BitsyDatabase? {
if (instance == null)
{
instance = Room.databaseBuilder(context,
BitsyDatabase::class.java, "CrystalWallet.db")
.allowMainThreadQueries()
.addMigrations(MIGRATION_2_3)
.addMigrations(MIGRATION_3_4)
.build()
}
return instance
}

internal val MIGRATION_2_3:Migration = object:Migration(2, 3) {
 override fun migrate(database:SupportSQLiteDatabase) {
database.execSQL("ALTER TABLE graphene_account ADD COLUMN upgraded_to_ltm INTEGER NOT NULL DEFAULT 0")
}
}

internal val MIGRATION_3_4:Migration = object:Migration(3, 4) {
 override fun migrate(database:SupportSQLiteDatabase) {
database.execSQL(
    "CREATE TABLE bitshares_account_name_cache ("
    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
    + "account_id TEXT UNIQUE NOT NULL,"
    + "name TEXT)"
)

database.execSQL("CREATE UNIQUE INDEX index_bitshares_account_name_cache_account_id ON bitshares_account_name_cache (account_id)")


}
}

internal val MIGRATION_4_5:Migration = object: Migration(4, 5) {
 override fun migrate(database: SupportSQLiteDatabase) {
database.execSQL(("CREATE TABLE bitcoin_transaction ("
+ "crypto_coin_transaction_id INTEGER PRIMARY KEY NOT NULL,"
+ "tx_id TEXT NOT NULL,"
+ "block INTEGER NOT NULL,"
+ "fee INTEGER NOT NULL,"
+ "confirmations INTEGER NOT NULL,"
+ "FOREIGN KEY (crypto_coin_transaction_id) REFERENCES crypto_coin_transaction(id) ON DELETE CASCADE)"))

database.execSQL(("CREATE TABLE bitcoin_transaction_gt_io ("
+ "bitcoin_transaction_id INTEGER NOT NULL,"
+ "io_index INTEGER NOT NULL,"
+ "address TEXT,"
+ "is_output INTEGER NOT NULL,"
+ "PRIMARY KEY (bitcoin_transaction_id, io_index, is_output),"
+ "FOREIGN KEY (bitcoin_transaction_id) REFERENCES bitcoin_transaction(crypto_coin_transaction_id) ON DELETE CASCADE)"))
}
}

internal val MIGRATION_5_6:Migration = object:Migration(5, 6) {
 override fun migrate(database:SupportSQLiteDatabase) {
database.execSQL(("CREATE TABLE bitcoin_address ("
+ "account_id INTEGER NOT NULL,"
+ "address_index INTEGER NOT NULL,"
+ "is_change INTEGER NOT NULL,"
+ "address TEXT NOT NULL,"
+ "PRIMARY KEY (account_id, address_index),"
+ "FOREIGN KEY (account_id) REFERENCES crypto_net_account(id) ON DELETE CASCADE)"))
}
}
}
}
