package cy.agorise.bitsybitshareswallet.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.common.primitives.UnsignedLong
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.jakewharton.rxbinding2.widget.RxTextView
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.AssetsAdapter
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.AssetViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.UserAccountViewModel
import cy.agorise.graphenej.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_receive_transaction.*
import java.lang.Exception
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.TimeUnit

class ReceiveTransactionFragment : Fragment() {
    private val TAG = this.javaClass.simpleName

    private val OTHER_ASSET = "other_asset"

    private lateinit var mUserAccountViewModel: UserAccountViewModel
    private lateinit var mAssetViewModel: AssetViewModel

    /** Current user account */
    private var mUserAccount: UserAccount? = null

    private var mDisposables = CompositeDisposable()

    private var mAsset: Asset? = null

    private var mAssetsAdapter: AssetsAdapter? = null

    private var mAssets = ArrayList<cy.agorise.bitsybitshareswallet.database.entities.Asset>()

    private var selectedAssetSymbol = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_receive_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure UserAccountViewModel to show the current account
        mUserAccountViewModel = ViewModelProviders.of(this).get(UserAccountViewModel::class.java)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")

        mUserAccountViewModel.getUserAccount(userId!!).observe(this,
            Observer<cy.agorise.bitsybitshareswallet.database.entities.UserAccount>{ user ->
                mUserAccount = UserAccount(user.id, user.name)
        })

        // Configure Assets spinner to show Assets already saved into the db
        mAssetViewModel = ViewModelProviders.of(this).get(AssetViewModel::class.java)

        mAssetViewModel.getAll().observe(this,
            Observer<List<cy.agorise.bitsybitshareswallet.database.entities.Asset>> { assets ->
                mAssets.clear()
                mAssets.addAll(assets)

                // Add an option at the end so the user can search for an asset other than the ones saved in the db
                val asset = cy.agorise.bitsybitshareswallet.database.entities.Asset(
                    OTHER_ASSET, "Other...", 0, "", ""
                )
                mAssets.add(asset)

                mAssetsAdapter = AssetsAdapter(context!!, android.R.layout.simple_spinner_item, mAssets)
                spAsset.adapter = mAssetsAdapter

                // Try to select the selectedAssetSymbol
                for (i in 0 until mAssetsAdapter!!.count) {
                    if (mAssetsAdapter!!.getItem(i)!!.symbol == selectedAssetSymbol) {
                        spAsset.setSelection(i)
                        break
                    }
                }
        })

        spAsset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) { }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val asset = mAssetsAdapter!!.getItem(position)!!

                if (asset.id == OTHER_ASSET) {
                    tilAsset.visibility = View.VISIBLE
                    mAsset = null
                } else {
                    tilAsset.visibility = View.GONE
                    selectedAssetSymbol = asset.symbol

                    mAsset = Asset(asset.id, asset.symbol, asset.precision)
                }
                updateQR()
            }
        }

        // Use RxJava Debounce to create QR code only after the user stopped typing an amount
        mDisposables.add(
            RxTextView.textChanges(tietAmount)
                .debounce(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateQR() }
        )
    }

    private fun updateQR() {
        if (mAsset == null) {
            ivQR.setImageDrawable(null)
            return
        }

        // Try to obtain the amount from the Amount Text Field or make it zero otherwise
        val amount: Long = try {
            val tmpAmount = tietAmount.text.toString().toDouble()
            (tmpAmount * Math.pow(10.0, mAsset!!.precision.toDouble())).toLong()
        }catch (e: Exception) {
            0
        }

        val total = AssetAmount(UnsignedLong.valueOf(amount), mAsset!!)
        val totalInDouble = Util.fromBase(total)
        val items = arrayOf(LineItem("transfer", 1, totalInDouble))
        val invoice = Invoice(mUserAccount!!.name, "", "#bitsy", mAsset!!.symbol, items, "", "")
        Log.d(TAG, "invoice: " + invoice.toJsonString())
        try {
            val bitmap = encodeAsBitmap(Invoice.toQrCode(invoice), "#139657") // PalmPay green
            ivQR.setImageBitmap(bitmap)
            updateAmountAddressUI(total, mUserAccount!!.name)
        } catch (e: WriterException) {
            Log.e(TAG, "WriterException. Msg: " + e.message)
        }
    }

    /**
     * Encodes the provided data as a QR-code. Used to provide payment requests.
     * @param data: Data containing payment request data as the recipient's address and the requested amount.
     * @param color: The color used for the QR-code
     * @return Bitmap with the QR-code encoded data
     * @throws WriterException if QR Code cannot be generated
     */
    @Throws(WriterException::class)
    internal fun encodeAsBitmap(data: String, color: String): Bitmap? {
        val result: BitMatrix

        // Get measured width and height of the ImageView where the QR code will be placed
        var w = ivQR.width
        var h = ivQR.height

        // Gets minimum side length and sets both width and height to that value so the final
        // QR code has a squared shape
        val minSide = if (w < h) w else h
        h = minSide
        w = h

        try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 0
            result = MultiFormatWriter().encode(
                data,
                BarcodeFormat.QR_CODE, w, h, hints
            )
        } catch (iae: IllegalArgumentException) {
            // Unsupported format
            return null
        }

        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result.get(x, y)) Color.parseColor(color) else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    /**
     * Updates the UI to show amount and account to send the payment
     *
     * @param total Total Amount in crypto to be paid
     * @param account Account to pay total
     */
    private fun updateAmountAddressUI(total: AssetAmount, account: String) {
        val df = DecimalFormat("####."+("#".repeat(total.asset.precision)))
        df.roundingMode = RoundingMode.CEILING
        df.decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())

        val amount = total.amount.toDouble() / Math.pow(10.toDouble(), total.asset.precision.toDouble())
        val strAmount = df.format(amount)

        val txtAmount = getString(R.string.template__please_pay, strAmount, total.asset.symbol)
        val txtAccount = getString(R.string.template__to, account)

        tvPleasePay.text = txtAmount
        tvTo.text = txtAccount
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!mDisposables.isDisposed) mDisposables.dispose()
    }
}