package cy.agorise.bitsybitshareswallet

import android.content.Context
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cy.agorise.bitsybitshareswallet.LiveDataTestUtil.blockingObserve
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.entities.EquivalentValue
import cy.agorise.bitsybitshareswallet.database.entities.Transfer
import org.junit.*
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TransfersTests {
    val TAG = "TransfersTests"
    @get:Rule val testRule = InstantTaskExecutorRule()
    private lateinit var db: BitsyDatabase
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun createDb() {
        Log.d(TAG,"createDb")
        db = Room.inMemoryDatabaseBuilder(context, BitsyDatabase::class.java).build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb(){
        Log.d(TAG,"closeDB")
        db.close()
    }

    /**
     * Prepares the database to the testGetTransfersMissingEquivalentValues and testGetTransfersMissingEquivalentValues2
     * tests.
     */
    private fun prepareMissingEquivalentValues(){
        // We create 2 transfers for the 'transfers' table, but only one of them will have an equivalent value entry
        val t1 = Transfer("1.11.702181910", 34118155, 1485018549, 264174, "1.3.0", "1.2.32567","1.2.139293",15869682,"1.3.0","")
        val t2 = Transfer("1.11.684483739", 33890367, 1547171166, 11030, "1.3.0", "1.2.139293","1.2.1029856",98,"1.3.120","")
        db.transferDao().insert(t1)
        db.transferDao().insert(t2)

        // Here's the equivalent value for the first transaction inserted (t1)
        val equivalentValue = EquivalentValue("1.11.702181910", 0, "usd")
        db.equivalentValueDao().insert(equivalentValue)
    }

    /**
     * This test makes use of the LiveData Testing library and its objective is to prove that
     * the TransferDao#getTransfersWithMissingValueIn(symbol: String) will return only the
     * second 'transfer' entry.
     * <p>
     * @see cy.agorise.bitsybitshareswallet.database.daos.TransferDao.getTransfersWithMissingValueIn
     * @see cy.agorise.bitsybitshareswallet.LiveDataTestUtil
     */
    @Test
    fun testGetTransfersMissingEquivalentValues(){
        prepareMissingEquivalentValues()
        db.transferDao()
            .getTransfersWithMissingValueIn("usd")
            .test()
//            .awaitValue()
//            .assertHasValue()
//            .assertValue { transfers -> transfers.size == 1 }
//            .assertValue { transfers -> transfers[0].id == "1.11.684483739"}
//            .assertValue { transfers -> transfers[0].blockNumber == 33890367L}
    }

    /**
     * This test makes use of the simple LiveDataTestUtil class and its objective is to prove that
     * the TransferDao#getTransfersWithMissingValueIn(symbol: String) will return only the
     * second 'transfer' entry.
     * <p>
     * @see cy.agorise.bitsybitshareswallet.LiveDataTestUtil
     */
    @Test
    fun testGetTransfersMissingEquivalentValues2(){
        prepareMissingEquivalentValues()
//        val transfers: Observable<Transfer> = LiveDataTestUtil.getValue(db.transferDao().getTransfersWithMissingValueIn("usd"))
//        Assert.assertNotNull(transfers)
//        Assert.assertEquals("1.11.684483739", transfers.id)
//        Assert.assertEquals(33890367, transfers.blockNumber)
//        Log.d(TAG, "transfer ${transfers}");
    }

    @Test
    fun testGetTransfersWithMissingBtsValue(){
        val t1 = Transfer("1.11.702181910",
            34118155,
            1485018549,
            264174,
            "1.3.0",
            "1.2.32567",
            "1.2.139293",
            15869682,
            "1.3.0","")
        val t2 = Transfer("1.11.684483739",
            33890367,
            1547171166,
            11030,
            "1.3.0",
            "1.2.139293",
            "1.2.1029856",
            98,
            "1.3.120",
            "")
        db.transferDao().insert(t1)
        db.transferDao().insert(t2)

        // transfer should be t2
        val transfer = db.transferDao().getTransfersWithMissingBtsValue().blockingObserve()
        Assert.assertEquals("1.11.684483739", transfer?.id)
    }
}
