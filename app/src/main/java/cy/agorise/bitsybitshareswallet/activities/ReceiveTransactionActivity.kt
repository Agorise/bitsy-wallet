package de.bitshares_munich.smartcoinswallet

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

import cy.agorise.bitsybitshareswallet.utils.BuildConfig
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.activities.RequestActivity
import cy.agorise.graphenej.Invoice
import cy.agorise.graphenej.LineItem
import kotlinx.android.synthetic.main.activity_receive.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.HashMap

import java.util.UUID

/**
 * Created by Syed Muhammad Muzzammil on 5/16/16.
 */
class ReceiveTransactionActivity : AppCompatActivity() {

    // Storage Permissions
    private val REQUEST_EXTERNAL_STORAGE = 1

    private val PERMISSIONS_STORAGE =
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    internal var price = ""
    internal var currency = ""
    internal var to = ""
    internal var account_id = ""
    internal var orderId = ""




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_receive)

        setBackButton(true)

        title = resources.getString(R.string.rcv_screen_name)

        orderId = UUID.randomUUID().toString()
        val intent = intent

        if (intent.hasExtra(getString(R.string.to))) {
            to = intent.getStringExtra(getString(R.string.to))
            val concate = this.getString(R.string.pay_to) + " : " + to
            username.setText(concate)
        }
        if (intent.hasExtra(getString(R.string.account_id))) {
            account_id = intent.getStringExtra(getString(R.string.account_id))
        }

        if (intent.hasExtra(getString(R.string.price))) {
            price = intent.getStringExtra(getString(R.string.price))
        } else {
            price = "0"
        }
        if (intent.hasExtra(getString(R.string.currency))) {
            currency = intent.getStringExtra(getString(R.string.currency))
        } else {
            currency = "BTS"
        }

        if (price.isEmpty()) {
            notfound.text = getString(R.string.no_amount_requested)
        } else {
            val concate =
                this.getString(R.string.amount) + ": " + price + " " + currency + " " + this.getString(R.string.requested)
            notfound.text = concate

            qrimage.post {
                qrimage.setImageBitmap(null)
                createQR()
            }
        }

        tvAppVersion_rcv_screen_activity.setText("v" + BuildConfig.VERSION_NAME + getString(R.string.beta))
        //updateBlockNumberHead()

        ivGotoKeypad.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {

                val intent = Intent(baseContext, RequestActivity::class.java)
                intent.putExtra(getString(R.string.to), to)
                intent.putExtra(getString(R.string.account_id), account_id)
                startActivity(intent)
                finish()
            }
        })

        var activity:Activity = this

        sharebtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {

                verifyStoragePermissions(activity)
                qrimage.buildDrawingCache()
                val bitmap = qrimage.drawingCache
                val mFile = savebitmap(bitmap)
                try {

                    var shareText = ""

                    if (!price.isEmpty() && price !== "0") {
                        shareText = activity.getString(R.string.please_pay) + " " + price + " " + currency + " " +
                                activity.getString(R.string.to) + " " + to
                    } else {
                        shareText = activity.getString(R.string.please_pay) + ": " + to
                    }

                    val sharingIntent = Intent(Intent.ACTION_SEND)
                    var uri: Uri? = null
                    if (Build.VERSION.SDK_INT < 24) {
                        uri = Uri.fromFile(mFile)
                    } else {
                        uri = Uri.parse(mFile!!.path);
                    }
                    sharingIntent.data = uri
                    sharingIntent.type = "*/*"
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareText)
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, shareText)
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    startActivity(Intent.createChooser(sharingIntent, activity.getString(R.string.share_qr_code)))

                } catch (e: Exception) {
                    Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        })

        backbutton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {

                onBackButtonPressed()
            }
        })
    }


    private fun savebitmap(bmp: Bitmap): File? {
        val extStorageDirectory =
            Environment.getExternalStorageDirectory().toString() + File.separator + resources.getString(R.string.folder_name)
        var outStream: OutputStream? = null
        var file = File(extStorageDirectory, "QrImage" + ".png")
        if (file.exists()) {
            file.delete()
            file = File(extStorageDirectory, "QrImage" + ".png")
        }

        try {
            outStream = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
            outStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return file
    }


    @Throws(WriterException::class)
    internal fun encodeAsBitmap(str: String, qrColor: String): Bitmap? {
        val result: BitMatrix
        try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 0
            result = MultiFormatWriter().encode(
                str,
                BarcodeFormat.QR_CODE, qrimage.width, qrimage.height, hints
            )
        } catch (iae: IllegalArgumentException) {
            // Unsupported format
            return null
        }

        val w = qrimage.width
        val h = qrimage.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result.get(x, y)) Color.parseColor(qrColor) else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }


    fun createQR(){

        val items = arrayOf<LineItem>(LineItem("transfer", 1, java.lang.Double.valueOf(price)))
        val invoice = Invoice(to, "", "", currency.replace("bit", ""), items, "", "")
        try {
            val bitmap = encodeAsBitmap(Invoice.toQrCode(invoice), "#006500")
            qrimage.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            Log.e("error_bitsy", "WriterException while trying to encode QR-code data. Msg: " + e.message)
        }

    }

    internal fun onBackButtonPressed() {
        super.onBackPressed()
    }


    fun verifyStoragePermissions(activity: Activity) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    /*private fun updateBlockNumberHead() {
        val handler = Handler()

        val myActivity = this

        val updateTask = object : Runnable {
            override fun run() {
                if (Application.isConnected()) {
                    ivSocketConnected.setImageResource(R.drawable.icon_connecting)
                    tvBlockNumberHead.setText(Application.blockHead)
                    ivSocketConnected.clearAnimation()
                } else {
                    ivSocketConnected.setImageResource(R.drawable.icon_disconnecting)
                    val myFadeInAnimation = AnimationUtils.loadAnimation(myActivity.applicationContext, R.anim.flash)
                    ivSocketConnected.startAnimation(myFadeInAnimation)
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(updateTask, 1000)
    }*/

    fun setBackButton(isBackButton: Boolean?) {
        if (isBackButton!!) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home ->{
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}
