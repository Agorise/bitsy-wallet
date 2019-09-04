package cy.agorise.bitsybitshareswallet.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cy.agorise.bitsybitshareswallet.database.daos.*
import cy.agorise.bitsybitshareswallet.database.entities.*
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetailDao
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetailDao

@Database(entities = [
        Asset::class,
        Authority::class,
        Balance::class,
        EquivalentValue::class,
        Transfer::class,
        UserAccount::class,
        Merchant::class,
        Teller::class,
        Node::class
    ],
    version = 5,
    exportSchema = true)
abstract class BitsyDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun authorityDao(): AuthorityDao
    abstract fun balanceDao(): BalanceDao
    abstract fun equivalentValueDao(): EquivalentValueDao
    abstract fun transferDao(): TransferDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun balanceDetailDao(): BalanceDetailDao
    abstract fun transferDetailDao(): TransferDetailDao
    abstract fun merchantDao(): MerchantDao
    abstract fun tellerDao(): TellerDao
    abstract fun nodeDao(): NodeDao

    companion object {

        // To make sure there is always only one instance of the database open
        @Volatile
        private var INSTANCE: BitsyDatabase? = null

        internal fun getDatabase(context: Context): BitsyDatabase? {
            if (INSTANCE == null) {
                synchronized(BitsyDatabase::class.java) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        BitsyDatabase::class.java, "BiTSyWallet.db"
                    ).addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .build()
                }
            }

            return INSTANCE
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS 'merchants' ('id' TEXT NOT NULL PRIMARY KEY, 'name' TEXT NOT NULL, 'address' TEXT, 'lat' REAL NOT NULL, 'lon' REAL NOT NULL, 'phone' TEXT, 'telegram' TEXT, 'website' TEXT)")
                database.execSQL("CREATE TABLE IF NOT EXISTS 'tellers' ('id' TEXT NOT NULL PRIMARY KEY, 'name' TEXT NOT NULL, 'address' TEXT, 'lat' REAL NOT NULL, 'lon' REAL NOT NULL, 'phone' TEXT, 'telegram' TEXT, 'website' TEXT)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE 'equivalent_values'")
                database.execSQL("CREATE TABLE IF NOT EXISTS 'equivalent_values' ('transfer_id' TEXT NOT NULL, 'value' INTEGER NOT NULL, 'symbol' TEXT NOT NULL, PRIMARY KEY(transfer_id, symbol), FOREIGN KEY (transfer_id) REFERENCES transfers(id))")

                database.execSQL("ALTER TABLE transfers ADD bts_value INTEGER")
                database.execSQL("UPDATE transfers SET bts_value = transfer_amount WHERE transfer_asset_id = '1.3.0'")
                database.execSQL("UPDATE transfers SET bts_value = -1 WHERE transfer_asset_id != '1.3.0'")

                database.execSQL("DROP TABLE assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS assets (`id` TEXT NOT NULL, `symbol` TEXT NOT NULL, `precision` INTEGER NOT NULL, `description` TEXT NOT NULL, `issuer` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3,4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE tellers")
                database.execSQL("CREATE TABLE IF NOT EXISTS tellers (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, address TEXT, lat REAL NOT NULL, lon REAL NOT NULL, phone TEXT, telegram TEXT, keybase TEXT, whatsapp TEXT, viber TEXT, email TEXT, website TEXT)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4,5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS nodes (url TEXT NOT NULL PRIMARY KEY, latency INTEGER NOT NULL, last_update INTEGER NOT NULL)")
            }
        }
    }
}
