package cy.agorise.bitsybitshareswallet.models.seed

import cy.agorise.bitsybitshareswallet.enums.SeedType
import cy.agorise.bitsybitshareswallet.models.AccountSeed
import cy.agorise.graphenej.crypto.SecureRandomGenerator
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicCode
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.experimental.and

class BIP39 : AccountSeed {
    /**
     * Teh amount of words for this seed
     */
    private val wmWordNumber = 12

    val seed: ByteArray
        get() = MnemonicCode.toSeed(this.masterSeed!!.split(" "), "")

    /**
     * Constructor from the dataabse
     * @param id The id on the database of this seed
     */
    constructor(id: Long, words: String) {
        this.id = id
        this.type = SeedType.BIP39
        this.masterSeed = words
    }

    /**
     * Constructor that generates the list of words
     * @param wordList Dictionary to be used
     */
    constructor(wordList: Array<String>) {
        try {
            this.type = SeedType.BIP39
            val entropySize = this.wmWordNumber * 11 / 8 * 8
            // We get a true random number
            val secureRandom = SecureRandomGenerator.getSecureRandom()
            val entropy = ByteArray(entropySize / 8)
            secureRandom.nextBytes(entropy)
            val md = MessageDigest.getInstance("SHA-256")
            val shaResult = md.digest(entropy)
            var mask = 0x80
            var cheksum = 0
            for (i in 0 until entropySize / 32) {
                cheksum = cheksum xor ((shaResult[0] and mask.toByte()).toInt())
                mask = mask / 2
            }
            val wordsIndex = IntArray(entropySize / 11 + 1)
            for (i in wordsIndex.indices) {
                wordsIndex[i] = 0
            }

            var lastIndex = 0
            var lastBit = 0
            for (i in entropy.indices) {
                for (j in 7 downTo 0) {
                    if (lastBit == 11) {
                        lastBit = 0
                        ++lastIndex
                    }
                    wordsIndex[lastIndex] = wordsIndex[lastIndex] xor
                            (Math.pow(2.0, (11 - (lastBit + 1)).toDouble()).toInt() * (entropy[i] and Math.pow(
                                2.0,
                                j.toDouble()
                            ).toInt().toByte()) shr j)
                    ++lastBit
                }
            }
            for (j in 7 downTo 0) {
                if (lastBit == 11) {
                    break
                }
                wordsIndex[lastIndex] = wordsIndex[lastIndex] xor
                        (Math.pow(2.0, (11 - (lastBit + 1)).toDouble()).toInt() * (cheksum and Math.pow(
                            2.0,
                            j.toDouble()
                        ).toInt()) shr j)
                ++lastBit
            }
            val words = StringBuilder()
            for (windex in wordsIndex) {
                words.append(wordList[windex]).append(" ")
            }
            words.deleteCharAt(words.length - 1)
            this.masterSeed = words.toString()
        } catch (ex: NoSuchAlgorithmException) {
        }

    }

    fun getBitsharesActiveKey(number: Int): ECKey {
        val masterKey = HDKeyDerivation.createMasterPrivateKey(this.seed)
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
            ChildNumber(number, false)
        )  //TODO implement multiple Address and accounts
        return org.bitcoinj.core.ECKey.fromPrivate(address.privKeyBytes)
    }
}
