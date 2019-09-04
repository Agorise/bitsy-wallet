package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import cy.agorise.bitsybitshareswallet.database.entities.Asset
import cy.agorise.bitsybitshareswallet.database.entities.UserAccount
import cy.agorise.bitsybitshareswallet.repositories.AssetRepository
import cy.agorise.bitsybitshareswallet.repositories.UserAccountRepository
import cy.agorise.graphenej.Invoice
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.HashMap

class ReceiveTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private var mUserAccountRepository = UserAccountRepository(application)
    private var mAssetRepository = AssetRepository(application)

    private val _qrCodeBitmap = MutableLiveData<Bitmap>()
    val qrCodeBitmap: LiveData<Bitmap>
        get() = _qrCodeBitmap

    internal fun getUserAccount(id: String): LiveData<UserAccount> {
        return mUserAccountRepository.getUserAccount(id)
    }

    internal fun getAllNonZero(): LiveData<List<Asset>> {
        return mAssetRepository.getAllNonZero()
    }

    internal fun updateInvoice(invoice: Invoice, size: Int) {
        viewModelScope.launch {
            try {
                _qrCodeBitmap.value = encodeAsBitmap(Invoice.toQrCode(invoice), "#139657", size) // PalmPay green
            } catch (e: Exception) {
                Log.d("ReceiveTransactionVM", e.message)
            }
        }
    }

    /**
     * Encodes the provided data as a QR-code. Used to provide payment requests.
     * @param data: Data containing payment request data as the recipient's address and the requested amount.
     * @param color: The color used for the QR-code
     * @param size: The size in pixels of the QR-code to generate
     * @return Bitmap with the QR-code encoded data
     * @throws WriterException if QR Code cannot be generated
     * @throws IllegalArgumentException IllegalArgumentException
     */
    @Throws(WriterException::class, IllegalArgumentException::class)
    private suspend fun encodeAsBitmap(data: String, color: String, size: Int): Bitmap? =
        withContext(Default) {

            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 0
            val result = MultiFormatWriter().encode(
                data,
                BarcodeFormat.QR_CODE, size, size, hints
            )

            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                val offset = y * size
                for (x in 0 until size) {
                    pixels[offset + x] =
                        if (result.get(x, y)) Color.parseColor(color) else Color.WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
            bitmap
        }
}