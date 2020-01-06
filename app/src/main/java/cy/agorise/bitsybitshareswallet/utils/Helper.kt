package cy.agorise.bitsybitshareswallet.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * Contains methods that are helpful in different parts of the app
 */
object Helper {
    private const val TAG = "Helper"

    /**
     * Creates and returns a Bitmap from the contents of a View, does not matter
     * if it is a simple view or a ViewGroup like a ConstraintLayout or a LinearLayout.
     *
     * @param view The view that is gonna be pictured.
     * @return The generated image from the given view.
     */
    fun loadBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

    fun saveTemporalBitmap(context: Context, bitmap: Bitmap): Uri {
        // save bitmap to cache directory
        try {
            val cachePath = File(context.cacheDir, "images")
            if (!cachePath.mkdirs())
            // don't forget to make the directory
                Log.d(TAG, "shareBitmapImage creating cache images folder")

            val stream = FileOutputStream("$cachePath/image.png") // overwrites this image every time
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
        } catch (e: IOException) {
            Log.d(TAG, "shareBitmapImage error: " + e.message)
        }

        // Send intent to share image+text
        val imagePath = File(context.cacheDir, "images")
        val newFile = File(imagePath, "image.png")

        // Create and return image uri
        return FileProvider.getUriForFile(context, "cy.agorise.bitsybitshareswallet.FileProvider", newFile)
    }

    /**
     * Verifies that the locale has a valid currency, else uses the default one. Then if
     * the given currency code is supported, returns it, else returns the default one.
     */
    fun getCoingeckoSupportedCurrency(locale: Locale): String {
        val currency = try {
            Currency.getInstance(locale)
        } catch (e: IllegalArgumentException) {
            Currency.getInstance(Locale.US)
        }

        val currencyCode = currency.currencyCode

        val supportedCurrencies = setOf("usd", "aed", "ars", "aud", "bdt", "bhd", "bmd", "brl", "cad",
            "chf", "clp", "cny", "czk", "dkk", "eur", "gbp", "hkd", "huf", "idr", "ils", "inr", "jpy",
            "krw", "kwd", "lkr", "mmk", "mxn", "myr", "nok", "nzd", "php", "pkr", "pln", "rub", "sar",
            "sek", "sgd", "thb", "try", "twd", "uah", "vef", "vnd", "zar", "xdr", "xag", "xau")

        return if (currencyCode.toLowerCase(Locale.ROOT) in supportedCurrencies)
            currencyCode
        else
            "USD"
    }
}