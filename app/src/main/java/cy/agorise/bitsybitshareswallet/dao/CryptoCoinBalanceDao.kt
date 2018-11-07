package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.CryptoCoinBalance
import cy.agorise.bitsybitshareswallet.models.CryptoNetBalance

@Dao
interface CryptoCoinBalanceDao {

    @get:Query("SELECT * FROM crypto_coin_balance")
    val all: List<CryptoCoinBalance>

    @get:Query("SELECT id as account_id, crypto_net FROM crypto_net_account")
    val allBalances: LiveData<List<CryptoNetBalance>>

    @get:Query("SELECT id FROM crypto_net_account WHERE crypto_net = 'BITSHARES' ORDER BY id ASC LIMIT 1")
    val firstBitsharesAccountId: Long

    @get:Query("SELECT * FROM crypto_coin_balance WHERE account_id IN (SELECT id FROM crypto_net_account WHERE crypto_net = 'BITSHARES')")
    val balancesFromBitsharesAccount: LiveData<List<CryptoCoinBalance>>

    @Query("SELECT * FROM crypto_coin_balance WHERE account_id = :accountId")
    fun getBalancesFromAccount(accountId: Long): LiveData<List<CryptoCoinBalance>>

    @Query("SELECT * FROM crypto_coin_balance WHERE account_id = :accountId AND crypto_currency_id = :assetId")
    fun getBalanceFromAccount(accountId: Long, assetId: Long): CryptoCoinBalance

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCryptoCoinBalance(vararg balances: CryptoCoinBalance): LongArray

}
