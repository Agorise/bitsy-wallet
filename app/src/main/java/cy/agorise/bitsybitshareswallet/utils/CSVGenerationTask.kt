package cy.agorise.bitsybitshareswallet.utils

import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import com.opencsv.CSVWriter
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import java.io.File
import java.io.FileWriter
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

/**
 * AsyncTask subclass used to move the CSV generation procedure to a background thread
 * and inform the UI of the progress.
 */
class CSVGenerationTask(context: Context) : AsyncTask<List<TransferDetail>, Int, String>() {

    companion object {
        private const val TAG = "CSVGenerationTask"
    }

    private val mContext: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(vararg params: List<TransferDetail>): String {
        return createCSVDocument(params[0])
    }

    private fun createCSVDocument(transferDetails: List<TransferDetail>): String {
        return try {
            // Create and configure a new CSV file to save the transfers list
            val externalStorageFolder = Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    Constants.EXTERNAL_STORAGE_FOLDER
            val fileName = mContext.get()?.resources?.let {
                "${it.getString(R.string.app_name)}-${it.getString(R.string.title_transactions)}"} + ".csv"
            val file = File(externalStorageFolder, fileName)
            file.createNewFile()
            val csvWriter = CSVWriter(FileWriter(file))

            // Add the table header
            val row = Array(7) {""}     // Array initialized with empty strings
            val columnNames = arrayOf(R.string.title_from, R.string.title_to, R.string.title_memo, R.string.title_date,
                R.string.title_time, R.string.title_amount, R.string.title_equivalent_value)
            for ((i, columnName) in columnNames.withIndex()) {
                row[i] = mContext.get()?.getString(columnName) ?: ""
            }
            csvWriter.writeNext(row)

            // Configure date and time formats to reuse in all the transfers
            val locale = mContext.get()?.resources?.configuration?.locale
            val dateFormat = SimpleDateFormat("MM-dd-yyyy", locale)
            val timeFormat = SimpleDateFormat("HH:mm:ss", locale)

            // Save all the transfers information
            for ( (index, transferDetail) in transferDetails.withIndex()) {
                val date = Date(transferDetail.date * 1000)

                row[0] = transferDetail.from ?: ""          // From
                row[1] = transferDetail.to ?: ""            // To
                row[2] = transferDetail.memo                // Memo
                row[3] = dateFormat.format(date)            // Date
                row[4] = timeFormat.format(date)            // Time

                // Asset Amount
                val assetPrecision = transferDetail.assetPrecision
                val assetAmount = transferDetail.assetAmount.toDouble() / Math.pow(10.0, assetPrecision.toDouble())
                row[5] = String.format("%.${assetPrecision}f %s", assetAmount, transferDetail.assetSymbol)

                // Fiat Equivalent
                row[6] = if (transferDetail.fiatAmount != null && transferDetail.fiatSymbol != null) {
                    val currency = Currency.getInstance(transferDetail.fiatSymbol)
                    val fiatAmount = transferDetail.fiatAmount.toDouble() /
                            Math.pow(10.0, currency.defaultFractionDigits.toDouble())
                    String.format("%.${currency.defaultFractionDigits}f %s", fiatAmount, currency.currencyCode)
                } else {
                    ""
                }

                csvWriter.writeNext(row)

                // TODO update progress
            }

            csvWriter.close()
            "CSV generated and saved: ${file.absolutePath}"
        } catch (e: Exception) {
            Log.e(TAG, "Exception while trying to generate a CSV. Msg: " + e.message)
            "Unable to generate CSV. Please retry. Error: ${e.message}"
        }
    }

    override fun onProgressUpdate(values: Array<Int>) {
        // TODO show progress
    }

    override fun onPostExecute(message: String) {
        mContext.get()?.toast(message)
    }
}