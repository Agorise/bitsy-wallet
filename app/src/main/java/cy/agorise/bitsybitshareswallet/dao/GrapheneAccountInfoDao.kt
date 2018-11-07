package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.GrapheneAccountInfo

@Dao
interface GrapheneAccountInfoDao {

    @get:Query("SELECT * FROM graphene_account")
    val all: LiveData<List<GrapheneAccountInfo>>

    @Query("SELECT * FROM graphene_account WHERE crypto_net_account_id = :cryptoNetAccountId")
    fun getGrapheneAccountInfo(cryptoNetAccountId: Long): LiveData<GrapheneAccountInfo>

    @Query("SELECT * FROM graphene_account WHERE crypto_net_account_id = :cryptoNetAccountId")
    fun getByAccountId(cryptoNetAccountId: Long): GrapheneAccountInfo

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGrapheneAccountInfo(vararg accounts: GrapheneAccountInfo): LongArray

}
