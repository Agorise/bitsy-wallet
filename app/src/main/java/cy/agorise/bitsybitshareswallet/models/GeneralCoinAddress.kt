package cy.agorise.bitsybitshareswallet.models

import cy.agorise.graphenej.Util
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.DeterministicKey
import java.util.*

class GeneralCoinAddress(
    generalCoinAccount: GeneralCoinAccount,
    b: Boolean,
    i: Int,
    deriveChildKey: DeterministicKey
) {

    /**
     * The id on the database
     */
    private var mId: Long = -1
    /**
     * The account that this address belongs
     */
    private var mAccount: GeneralCoinAccount? = null
    /**
     * If this is change or external
     */
    private var mIsChange: Boolean = false
    /**
     * The index fo this address in the account
     */
    private var mIndex: Int = 0
    /**
     * The ky used to calculate the address
     */
    private var mKey: ECKey? = null
    /**
     * The list of the transactions that used this address as input
     */
    private var mTransactionInput: List<GTxIO>? = ArrayList()
    /**
     * The list of the transactions that used this address as output
     */
    private var mTransactionOutput: List<GTxIO>? = ArrayList()


    /**
     * Contrsutcotr used from the database
     * @param id The id on the database
     * @param account The account of this address
     * @param isChange if it is change or external address
     * @param index the index on the account of this address
     * @param publicHexKey The public Address String
     */
    fun GeneralCoinAddress(
        id: Long,
        account: GeneralCoinAccount,
        isChange: Boolean,
        index: Int,
        publicHexKey: String
    )  {
        this.mId = id
        this.mAccount = account
        this.mIsChange = isChange
        this.mIndex = index
        this.mKey = ECKey.fromPublicOnly(Util.hexToBytes(publicHexKey))
    }

    /**
     * Basic constructor
     * @param account The account of this address
     * @param isChange if it is change or external address
     * @param index The index on the account of this address
     * @param key The key to generate the private and the public key of this address
     */
    fun GeneralCoinAddress(account: GeneralCoinAccount, isChange: Boolean, index: Int, key: DeterministicKey) {
        this.mId = -1
        this.mAccount = account
        this.mIsChange = isChange
        this.mIndex = index
        this.mKey = key
    }

    /**
     * Getter of the database id
     */
    fun getId(): Long {
        return mId
    }

    /**
     * Setter of the database id
     */
    fun setId(id: Long) {
        this.mId = id
    }

    /**
     * Getter for he account
     */
    fun getAccount(): GeneralCoinAccount {
        return this!!.mAccount!!
    }

    /**
     * Indicates if this addres is change, if not is external
     */
    fun isIsChange(): Boolean {
        return mIsChange
    }

    /**
     * Getter for the index on the account of this address
     */
    fun getIndex(): Int {
        return mIndex
    }

    /**
     * Getter for the key of this address
     */
    fun getKey(): ECKey? {
        return this!!.mKey
    }

    /**
     * Set the key for generate private key, this is used when this address is loaded from the database
     * and want to be used to send transactions
     * @param key The key that generates the private and the public key
     */
    fun setKey(key: DeterministicKey) {
        this.mKey = key
    }

    /**
     * Get the address as a String
     * @param param The network param of this address
     */
    fun getAddressString(param: NetworkParameters): String {
        return mKey!!.toAddress(param).toString()
    }

    /**
     * Returns the bitcoinj Address representing this address
     * @param param The network parameter of this address
     */
    fun getAddress(param: NetworkParameters): Address {
        return mKey!!.toAddress(param)
    }

    /**
     * Gets the list of transaction that this address is input
     */
    fun getTransactionInput(): List<GTxIO> {
        return this!!.mTransactionInput!!
    }

    /**
     * Set the transactions that this address is input
     */
    fun setTransactionInput(transactionInput: List<GTxIO>) {
        this.mTransactionInput = transactionInput
    }

    /**
     * Find if this address is input of a transaction
     * @param inputToFind The GTxIO to find
     * @param param The network parameter of this address
     * @return if this address belongs to the transaction
     */
    fun hasTransactionInput(inputToFind: GTxIO, param: NetworkParameters): Boolean {
        for (input in this!!.mTransactionInput!!) {
            if (input.transaction!!.txid.equals(inputToFind.transaction!!.txid) && input.address!!.getAddressString(
                    param
                ).equals(
                    inputToFind.address!!.getAddressString(param)
                )
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Gets the list of transaction that this address is output
     */
    fun getTransactionOutput(): List<GTxIO> {
        return this!!.mTransactionOutput!!
    }

    /**
     * Find if this address is output of a transaction
     * @param outputToFind The GTxIO to find
     * @param param the network parameter of this address
     * @return if this address belongs to the transaction
     */
    fun hasTransactionOutput(outputToFind: GTxIO, param: NetworkParameters): Boolean {
        for (output in this!!.mTransactionOutput!!) {
            if (output.transaction!!.txid.equals(outputToFind.transaction!!.txid) && output.address!!.getAddressString(
                    param
                ).equals(
                    outputToFind.address!!.getAddressString(param)
                )
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Sets the list of transaction that this address is output
     */
    fun setTransactionOutput(outputTransaction: List<GTxIO>) {
        this.mTransactionOutput = outputTransaction
    }

    /**
     * Get the amount of uncofirmed balance
     */
    fun getUnconfirmedBalance(): Long {
        var answer: Long = 0
        for (input in this!!.mTransactionInput!!) {
            if (input.transaction!!.confirm < mAccount!!.cryptoNet!!.confirmationsNeeded) {
                answer += input.amount
            }
        }

        for (output in this!!.mTransactionOutput!!) {
            if (output.transaction!!.confirm < mAccount!!.cryptoNet!!.confirmationsNeeded) {
                answer -= output.amount
            }
        }

        return answer
    }

    /**
     * Get the amount of confirmed balance
     */
    fun getConfirmedBalance(): Long {
        var answer: Long = 0
        for (input in this!!.mTransactionInput!!) {
            if (input.transaction!!.confirm >= mAccount!!.cryptoNet!!.confirmationsNeeded) {
                answer += input.amount
            }
        }

        for (output in this!!.mTransactionOutput!!) {
            if (output.transaction!!.confirm >= mAccount!!.cryptoNet!!.confirmationsNeeded) {
                answer -= output.amount
            }
        }

        return answer
    }

    /**
     * Get the date of the last transaction or null if there is no transaction
     */
    fun getLastDate(): Date? {
        var lastDate: Date? = null
        for (input in this!!.mTransactionInput!!) {
            if (lastDate == null || lastDate.before(input.transaction!!.date)) {
                lastDate = input.transaction!!.date
            }
        }
        for (output in this!!.mTransactionOutput!!) {
            if (lastDate == null || lastDate.before(output.transaction!!.date)) {
                lastDate = output.transaction!!.date
            }
        }
        return lastDate
    }

    /**
     * Get the amount of the less cofnirmed transaction, this is used to set how confirmations are
     * left
     */
    fun getLessConfirmed(): Int {
        var lessConfirm = -1
        for (input in this!!.mTransactionInput!!) {
            if (lessConfirm == -1 || input.transaction!!.confirm < lessConfirm) {
                lessConfirm = input.transaction!!.confirm
            }
        }

        for (output in this!!.mTransactionOutput!!) {
            if (lessConfirm == -1 || output.transaction!!.confirm < lessConfirm) {
                lessConfirm = output.transaction!!.confirm
            }
        }
        return lessConfirm
    }

    /**
     * Gets the unspend transactions input
     * @return The list with the unspend transasctions
     */
    fun getUTXos(): List<GTxIO> {
        val utxo = ArrayList<GTxIO>()
        for (gitx in this!!.mTransactionInput!!) {
            var find = false
            for (gotx in this!!.mTransactionOutput!!) {
                if (gitx.transaction!!.txid.equals(gotx.originalTxid)) {
                    find = true
                    break
                }
            }
            if (!find) {
                utxo.add(gitx)
            }
        }
        return utxo
    }

    /**
     * Fire the onBalanceChange event
     */
    fun BalanceChange() {
        this.getAccount().balanceChange()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as GeneralCoinAddress?

        return (mIsChange == that!!.mIsChange && mIndex == that.mIndex && mId as Int == -1 && mAccount?.equals(that.mAccount) ?: (that.mAccount == null && if (mKey != null)
            mKey == that.mKey
        else
            that.mKey == null && if (mTransactionInput != null)
                mTransactionInput == that.mTransactionInput
            else
                that.mTransactionInput == null && if (mTransactionOutput != null)
                    mTransactionOutput == that.mTransactionOutput
                else
                    that.mTransactionOutput == null))

    }

    override fun hashCode(): Int {
        var result = mId.toInt()
        result = 31 * result + (mAccount?.hashCode() ?: 0)
        result = 31 * result + if (mIsChange) 1 else 0
        result = 31 * result + mIndex
        result = 31 * result + if (mKey != null) mKey.hashCode() else 0
        result = 31 * result + if (mTransactionInput != null) mTransactionInput.hashCode() else 0
        result = 31 * result + if (mTransactionOutput != null) mTransactionOutput.hashCode() else 0
        return result
    }

    /**
     * Update the transactions of this Address
     * @param transaction The transaction to update
     * @return true if this address has the transaction false otherwise
     */
    fun updateTransaction(transaction: GeneralTransaction): Boolean {
        for (gitx in this!!.mTransactionInput!!) {
            if (gitx.transaction!!.equals(transaction)) {
                gitx.transaction!!.confirm = transaction.confirm
                gitx.transaction!!.block = transaction.block
                gitx.transaction!!.blockHeight = transaction.blockHeight
                gitx.transaction!!.date = transaction.date
                gitx.transaction!!.memo = transaction.memo
                return true
            }
        }

        for (gotx in this!!.mTransactionOutput!!) {
            if (gotx.transaction!!.equals(transaction)) {
                gotx.transaction!!.confirm = transaction.confirm
                gotx.transaction!!.block = transaction.block
                gotx.transaction!!.blockHeight = transaction.blockHeight
                gotx.transaction!!.date = transaction.date
                gotx.transaction!!.memo = transaction.memo
                return true
            }
        }
        return false
    }
}