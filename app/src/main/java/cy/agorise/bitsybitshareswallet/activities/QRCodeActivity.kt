package cy.agorise.bitsybitshareswallet.activities

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.zxing.Result
import cy.agorise.bitsybitshareswallet.R
import me.dm7.barcodescanner.zxing.ZXingScannerView


class QRCodeActivity : CustomActivity(), ZXingScannerView.ResultHandler {
    internal var id: Int = 0
    internal var progressDialog: ProgressDialog? = null
    private var mScannerView: ZXingScannerView? = null

    /* Pin pinDialog */
    private val pinDialog: Dialog? = null

    /* Internal attribute used to keep track of the activity state */
    private val mRestarting: Boolean = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBackButton(true)
        setTitle(getResources().getString(R.string.qr_code_activity_name))

        verifyCameraPermissions(this)

        val intent = getIntent()
        id = intent.getIntExtra("id", -1)
        mScannerView = ZXingScannerView(this)   // Programmatically initialize the scanner view
        setContentView(mScannerView)                // Set the scanner view as the content view

        progressDialog = ProgressDialog(this)
    }

    fun setBackButton(isBackButton: Boolean?) {
        if (isBackButton!!) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onResume() {
        super.onResume()
        mScannerView!!.setResultHandler(this) // Register ourselves as a handler for scan results.
        mScannerView!!.startCamera()          // Start camera on resume
    }

    override fun onPause() {
        super.onPause()
        mScannerView!!.stopCamera()           // Stop camera on pause
    }

    override fun handleResult(rawResult: Result) {
        showDialog("", "")

        AsyncTask.execute {
            mScannerView!!.stopCamera()
            if (id == 0) {
                finishWithResult(rawResult.toString())
            } else if (id == 1) {
                StartWithfinishWithResult(rawResult.toString())
            }
        }
    }

    private fun showDialog(title: String, msg: String) {
        if (progressDialog != null) {
            if (!progressDialog!!.isShowing) {
                progressDialog!!.setTitle(title)
                progressDialog!!.setMessage(msg)
                progressDialog!!.show()
            }
        }
    }

    private fun hideDialog() {

        if (progressDialog != null) {
            if (progressDialog!!.isShowing) {
                progressDialog!!.cancel()
            }
        }

    }

    private fun finishWithResult(parseddata: String) {
        val conData = Bundle()
        conData.putSerializable("sResult", parseddata)
        val intent = Intent()
        intent.putExtras(conData)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun StartWithfinishWithResult(parseddata: String) {
        Log.d(TAG, "StartWithfinishWithResult")
        Log.d(TAG, "parsed data: $parseddata")
        val conData = Bundle()
        conData.putSerializable("sResult", parseddata)
        val intent = Intent(this@QRCodeActivity, SendTransactionActivity::class.java)
        intent.putExtras(conData)
        intent.putExtra("id", 5)
        startActivity(intent)
        finish()
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

    companion object {

        // Camera Permissions
        private val REQUEST_CAMERA_PERMISSION = 1
        private val TAG = "QRCodeActivity"
        private val PERMISSIONS_CAMERA = arrayOf(Manifest.permission.CAMERA)

        fun verifyCameraPermissions(activity: Activity) {
            // Check if we have write permission
            val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_CAMERA,
                    REQUEST_CAMERA_PERMISSION
                )
            }
        }
    }
}
