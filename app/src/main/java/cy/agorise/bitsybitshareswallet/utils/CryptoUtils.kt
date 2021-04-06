package cy.agorise.bitsybitshareswallet.utils

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.preference.PreferenceManager

import com.moldedbits.r2d2.R2d2
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

import javax.crypto.AEADBadTagException


/**
 * Class that provides encryption/decryption support by using the key management framework provided
 * by the KeyStore system.
 *
 * The implemented scheme was taken from [this](https://medium.com/@ericfu/securely-storing-secrets-in-an-android-application-501f030ae5a3)> blog post.
 *
 * @see [Android Keystore System](https://developer.android.com/training/articles/keystore.html)
 */

object CryptoUtils {

    /**
     * Encrypts and stores a key-value pair in the shared preferences
     * @param context The application context
     * @param key The key to be used to reference the data
     * @param value The actual value to be stored
     */
    fun put(context: Context, key: String, value: String) {
        val r2d2 = R2d2(context)
        val encrypted = r2d2.encryptData(value)
        PreferenceManager.getDefaultSharedPreferences(context).edit {
                putString(key, encrypted)
            }
    }

    /**
     * Retrieves and decrypts an encrypted value from the shared preferences
     * @param context The application context
     * @param key The key used to reference the data
     * @return The plaintext version of the encrypted data
     */
    operator fun get(context: Context, key: String): String {
        val r2d2 = R2d2(context)
        val encrypted = PreferenceManager.getDefaultSharedPreferences(context).getString(key, null)
        return r2d2.decryptData(encrypted)
    }

    /**
     * Encrypts some data
     * @param context The application context
     * @param plaintext The plaintext version of the data
     * @return Encrypted data
     */
    fun encrypt(context: Context, plaintext: String): String {
        val r2d2 = R2d2(context)
        return r2d2.encryptData(plaintext)
    }

    /**
     * Decrypts some data
     * @param context The application context
     * @param ciphertext The ciphertext version of the data
     * @return Decrypted data
     */
    @Throws(AEADBadTagException::class)
    fun decrypt(context: Context, ciphertext: String): String {
        val r2d2 = R2d2(context)
        return r2d2.decryptData(ciphertext)
    }

    /**
     * Generates a random salt string
     */
    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.DEFAULT).trim()
    }

    /**
     * Creates a SHA-256 hash of the given string
     */
    @Throws(NoSuchAlgorithmException::class)
    fun createSHA256Hash(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")

        md.update(text.toByteArray())
        val digest = md.digest()

        return Base64.encodeToString(digest, Base64.DEFAULT).trim()
    }
}

