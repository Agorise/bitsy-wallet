package cy.agorise.bitsybitshareswallet.models

import android.content.Context
import com.google.gson.JsonObject
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.bitsybitshareswallet.enums.CryptoNetAccount
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import java.util.*

abstract class GeneralCoinAccount(
    mId: Long,
    seed: AccountSeed,
    mAccountIndex: Int,
    mCryptoNet: CryptoNet,
    mAccountNumber: Int,
    mLastExternalIndex: Int,
    mLastChangeIndex: Int,
    /**
     * is the coin number defined by the SLIP-44
     */
    private val mCoinNumber: Int
) : CryptoNetAccount(mId, seed.id, mAccountIndex, mCryptoNet) {
    /**
     * The account number of the BIP-44
     */
    //TODO save address
    /*public void saveAddresses(SCWallDatabase db) {
        for (GeneralCoinAddress externalAddress : this.mExternalKeys.values()) {
            if (externalAddress.getId() == -1) {
                long id = db.putGeneralCoinAddress(externalAddress);
                if(id != -1)
                    externalAddress.setId(id);
            } else {
                db.updateGeneralCoinAddress(externalAddress);
            }
        }

        for (GeneralCoinAddress changeAddress : this.mChangeKeys.values()) {
            if (changeAddress.getId() == -1) {
                Log.i("SCW","change address id " + changeAddress.getId());
                long id = db.putGeneralCoinAddress(changeAddress);
                if(id != -1)
                    changeAddress.setId(id);
            } else {
                db.updateGeneralCoinAddress(changeAddress);
            }
        }

        db.updateGeneralCoinAccount(this);
    }*/

    /**
     * Getter of the account number
     */
    var accountNumber: Int = 0
        protected set
    /**
     * The index of the last used external address
     */
    /**
     * Getter of the last external address used index
     */
    var lastExternalIndex: Int = 0
        protected set
    /**
     * The indes of the last used change address
     */
    /**
     * Getter of the last change address used index
     */
    var lastChangeIndex: Int = 0
        protected set
    /**
     * The account key, this is calculated as a cache
     */
    protected var mAccountKey: DeterministicKey? = null
    /**
     * With this key we can calculate the external addresses
     */
    lateinit var externalKey: DeterministicKey
        protected set
    /**
     * With this key we can calculate the change address
     */
    lateinit var changeKey: DeterministicKey
        protected set
    /**
     * The keys for externals addresses
     */
    protected var mExternalKeys = HashMap<Int, GeneralCoinAddress>()
    /**
     * The keys for the change addresses
     */
    protected var mChangeKeys = HashMap<Int, GeneralCoinAddress>()

    /**
     * The list of transaction that involves this account
     */
    protected var mTransactions: List<GeneralTransaction> = ArrayList()

    //TODO check init address
    /*public List<GeneralCoinAddress> getAddresses(SCWallDatabase db) {
        //TODO check for used address
        this.getNextReceiveAddress();
        this.getNextChangeAddress();
        this.calculateGapExternal();
        this.calculateGapChange();

        List<GeneralCoinAddress> addresses = new ArrayList();
        addresses.addAll(this.mChangeKeys.values());
        addresses.addAll(this.mExternalKeys.values());
        this.saveAddresses(db);
        return addresses;
    }*/

    /**
     * Get the list of all the address, external and change addresses
     * @return a list with all the addresses of this account
     */
    val addresses: List<GeneralCoinAddress>
        get() {
            val addresses = ArrayList<GeneralCoinAddress>()
            addresses.addAll(this.mChangeKeys.values)
            addresses.addAll(this.mExternalKeys.values)
            return addresses
        }

    /**
     * Getter of the next receive address
     * @return The next unused receive address to be used
     */
    abstract val nextReceiveAddress: String

    /**
     * Getter of the next change address
     * @return The next unused change address to be used
     */
    abstract val nextChangeAddress: String

    /**
     * Getter of the list of transactions
     */
    /**
     * Setter for the transactions of this account, this is used from the database
     */
    var transactions: List<GeneralTransaction>
        get() {
            val transactions = ArrayList<GeneralTransaction>()
            for (address in this.mExternalKeys.values) {
                for (giotx in address.getTransactionInput()) {
                    if (!transactions.contains(giotx.transaction)) {
                        transactions.add(giotx.transaction!!)
                    }
                }
                for (giotx in address.getTransactionOutput()) {
                    if (!transactions.contains(giotx.transaction)) {
                        transactions.add(giotx.transaction!!)
                    }
                }
            }

            for (address in this.mChangeKeys.values) {
                for (giotx in address.getTransactionInput()) {
                    if (!transactions.contains(giotx.transaction)) {
                        transactions.add(giotx.transaction!!)
                    }
                }
                for (giotx in address.getTransactionOutput()) {
                    if (!transactions.contains(giotx.transaction)) {
                        transactions.add(giotx.transaction!!)
                    }
                }
            }

            Collections.sort(transactions, TransactionsCustomComparator())
            return transactions
        }
        set(transactions) {
            this.mTransactions = transactions
        }

    val cryptoCoin: CryptoCoin
        get() = CryptoCoin.valueOf(this.cryptoNet!!.name)

    /**
     * Return the network parameters, this is used for the bitcoiinj library
     */
    abstract val networkParam: NetworkParameters

    abstract val balance: List<CryptoNetBalance>

    init {
        this.accountNumber = mAccountNumber
        this.lastExternalIndex = mLastExternalIndex
        this.lastChangeIndex = mLastChangeIndex
        calculateAddresses(seed.privateKey as DeterministicKey)
    }

    /**
     * Calculates each basic key, not the addresses keys using the BIP-44
     */
    private fun calculateAddresses(masterKey: DeterministicKey) {
        val purposeKey = HDKeyDerivation.deriveChildKey(
            masterKey,
            ChildNumber(44, true)
        )
        val coinKey = HDKeyDerivation.deriveChildKey(
            purposeKey,
            ChildNumber(this.mCoinNumber, true)
        )
        this.mAccountKey = HDKeyDerivation.deriveChildKey(
            coinKey,
            ChildNumber(this.accountNumber, true)
        )
        this.externalKey = HDKeyDerivation.deriveChildKey(
            this.mAccountKey!!,
            ChildNumber(0, false)
        )
        this.changeKey = HDKeyDerivation.deriveChildKey(
            this.mAccountKey!!,
            ChildNumber(1, false)
        )
    }

    /**
     * Calculate the external address keys until the index + gap
     */
    fun calculateGapExternal() {
        for (i in 0 until this.lastExternalIndex + sAddressGap) {
            if (!this.mExternalKeys.containsKey(i)) {
                this.mExternalKeys[i] = GeneralCoinAddress(
                    this, false, i,
                    HDKeyDerivation.deriveChildKey(
                        this.externalKey,
                        ChildNumber(i, false)
                    )
                )
            }
        }
    }

    /**
     * Calculate the change address keys until the index + gap
     */
    fun calculateGapChange() {
        for (i in 0 until this.lastChangeIndex + sAddressGap) {
            if (!this.mChangeKeys.containsKey(i)) {
                this.mChangeKeys[i] = GeneralCoinAddress(
                    this, true, i,
                    HDKeyDerivation.deriveChildKey(
                        this.changeKey,
                        ChildNumber(i, false)
                    )
                )
            }
        }
    }

    /**
     * Charges the list of addresse of this account, this is used from the database
     */
    fun loadAddresses(addresses: List<GeneralCoinAddress>) {
        for (address in addresses) {
            if (address.isIsChange()) {
                this.mChangeKeys[address.getIndex()] = address
            } else {
                this.mExternalKeys[address.getIndex()] = address
            }
        }
    }

    /**
     * Transfer coin amount to another address
     *
     * @param toAddress The destination address
     * @param coin the coin
     * @param amount the amount to send in satoshi
     * @param memo the memo, this can be empty
     * @param context the android context
     */
    abstract fun send(
        toAddress: String, coin: CryptoCoin, amount: Long, memo: String,
        context: Context
    )

    /**
     * Transform this account into json object to be saved in the bin file, or any other file
     */
    fun toJson(): JsonObject {
        val answer = JsonObject()
        answer.addProperty("type", this.cryptoNet!!.name)
        answer.addProperty("name", this.name)
        answer.addProperty("accountNumber", this.accountNumber)
        answer.addProperty("changeIndex", this.lastChangeIndex)
        answer.addProperty("externalIndex", this.lastExternalIndex)
        return answer
    }

    /**
     * Get the address as string of an adrees index
     * @param index The index of the address
     * @param change if it is change addres or is a external address
     * @return The Address as string
     */
    abstract fun getAddressString(index: Int, change: Boolean): String

    /**
     * Get the GeneralCoinAddress object of an address
     * @param index the index of the address
     * @param change if it is change addres or is a external address
     * @return The GeneralCoinAddress of the address
     */
    abstract fun getAddress(index: Int, change: Boolean): GeneralCoinAddress

    /**
     * Triggers the event onBalanceChange
     */
    fun balanceChange() {
        this._fireOnChangeBalance(this.balance[0]) //TODO make it more genertic
    }

    /**
     * Compare the transaction, to order it for the list of transaction
     */
    inner class TransactionsCustomComparator : Comparator<GeneralTransaction> {
        override fun compare(o1: GeneralTransaction, o2: GeneralTransaction): Int {
            return o1.date!!.compareTo(o2.date)
        }
    }

    /**
     * Add listener for the onChangebalance Event
     */
    /*public void addChangeBalanceListener(ChangeBalanceListener listener) {
        this.mChangeBalanceListeners.add(listener);
    }*/

    /**
     * Fire the onChangeBalance event
     */
    protected fun _fireOnChangeBalance(balance: CryptoNetBalance) {
        /*for (ChangeBalanceListener listener : this.mChangeBalanceListeners) {
            listener.balanceChange(balance);
        }*/
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || GeneralCoinAccount.javaClass !== o!!.javaClass) return false

        val that = o as GeneralCoinAccount?

        if (this.cryptoNet !== that!!.cryptoNet) return false
        if (this.accountNumber != that!!.accountNumber) return false
        return if (this.mAccountKey != null)
            this.mAccountKey == that.mAccountKey
        else
            that.mAccountKey == null

    }

    override fun hashCode(): Int {
        var result = this.accountNumber
        result = 31 * result + if (this.mAccountKey != null) this.mAccountKey!!.hashCode() else 0
        return result
    }

    /**
     * Updates a transaction
     *
     * @param transaction The transaction to update
     */
    fun updateTransaction(transaction: GeneralTransaction) {
        // Checks if it has an external address
        for (address in this.mExternalKeys.values) {
            if (address.updateTransaction(transaction)) {
                return
            }
        }

        for (address in this.mChangeKeys.values) {
            if (address.updateTransaction(transaction)) {
                return
            }
        }
    }

    companion object {

        /**
         * The Limit gap define in the BIP-44
         */
        private val sAddressGap = 20
    }
}
