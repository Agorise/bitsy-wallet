package cy.agorise.bitsybitshareswallet.models

import android.content.Context
import cy.agorise.bitsybitshareswallet.dao.CrystalDatabase
import cy.agorise.bitsybitshareswallet.enums.CryptoNetAccount
import cy.agorise.bitsybitshareswallet.enums.SeedType
import cy.agorise.graphenej.BrainKey
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation

class GrapheneAccount : CryptoNetAccount {
    lateinit var accountId: String
    var upgradedToLtm: Boolean = false

    constructor() {}

    constructor(account: CryptoNetAccount) : super(
        account.id,
        account.seedId,
        account.accountIndex,
        account.cryptoNet!!
    ) {
    }

    fun loadInfo(info: GrapheneAccountInfo?) {
        if (info != null) {
            this.name = info.name
            this.accountId = info.accountId
            this.upgradedToLtm = info.upgradedToLtm
        } else {
            this.name = ""
            this.accountId = "-1"
            this.upgradedToLtm = false
        }
    }

    /**
     * Return the owner key, generates from the seed if it has not been generated. null if it can't be generated
     */
    fun getOwnerKey(context: Context): ECKey? {
        val seed = CrystalDatabase.getAppDatabase(context)!!.accountSeedDao().findById(this.seedId)
        if (seed == null) {
            System.out.println("Error: Seed null " + this.seedId)
            return null
        }
        if (seed!!.type!!.equals(SeedType.BRAINKEY)) {
            println("Seed type barinkey")
            return seed!!.privateKey
        } else {
            println("Seed type bip39")
            val masterKey = seed!!.privateKey as DeterministicKey
            val purposeKey = HDKeyDerivation.deriveChildKey(
                masterKey,
                ChildNumber(48, true)
            )
            val networkKey = HDKeyDerivation.deriveChildKey(
                purposeKey,
                ChildNumber(1, true)
            )
            val accountIndexKey = HDKeyDerivation.deriveChildKey(
                networkKey,
                ChildNumber(0, true)
            )
            val permission = HDKeyDerivation.deriveChildKey(
                accountIndexKey,
                ChildNumber(0, true)
            )
            val address = HDKeyDerivation.deriveChildKey(
                permission,
                ChildNumber(0, false)
            )
            return ECKey.fromPrivate(address.privKeyBytes)
        }
    }

    /**
     * Return the active key, generates from the seed if it has not been generated. null if it can't be generated
     */
    fun getActiveKey(context: Context): ECKey? {
        val seed = CrystalDatabase.getAppDatabase(context)!!.accountSeedDao().findById(this.seedId) ?: return null
        if (seed.type!!.equals(SeedType.BRAINKEY)) {
            return BrainKey(seed.masterSeed, 0).privateKey
        } else {
            println("calculating activekey from bip39")
            val masterKey = seed.privateKey as DeterministicKey
            val purposeKey = HDKeyDerivation.deriveChildKey(
                masterKey,
                ChildNumber(48, true)
            )
            val networkKey = HDKeyDerivation.deriveChildKey(
                purposeKey,
                ChildNumber(1, true)
            )
            val accountIndexKey = HDKeyDerivation.deriveChildKey(
                networkKey,
                ChildNumber(0, true)
            )
            val permission = HDKeyDerivation.deriveChildKey(
                accountIndexKey,
                ChildNumber(1, true)
            )
            val address = HDKeyDerivation.deriveChildKey(
                permission,
                ChildNumber(0, false)
            )  //TODO implement multiple Address and accounts
            return ECKey.fromPrivate(address.privKeyBytes)
        }
    }

    /**
     * Return the memo key, generates from the seed if it has not been generated. null if it can't be generated
     */
    fun getMemoKey(context: Context): ECKey? {
        val seed = CrystalDatabase.getAppDatabase(context)!!.accountSeedDao().findById(this.seedId) ?: return null
        if (seed.type!!.equals(SeedType.BRAINKEY)) {
            return BrainKey(seed.masterSeed, 0).privateKey
        } else {
            val masterKey = seed.privateKey as DeterministicKey
            val purposeKey = HDKeyDerivation.deriveChildKey(
                masterKey,
                ChildNumber(48, true)
            )
            val networkKey = HDKeyDerivation.deriveChildKey(
                purposeKey,
                ChildNumber(1, true)
            )
            val accountIndexKey = HDKeyDerivation.deriveChildKey(
                networkKey,
                ChildNumber(0, true)
            )
            val permission = HDKeyDerivation.deriveChildKey(
                accountIndexKey,
                ChildNumber(3, true)
            )
            val address = HDKeyDerivation.deriveChildKey(
                permission,
                ChildNumber(0, false)
            )  //TODO implement multiple Address and accounts
            return ECKey.fromPrivate(address.privKeyBytes)
        }
    }

    companion object {

        var subclass = 1
    }
}
