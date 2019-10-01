package cy.agorise.bitsybitshareswallet.utils

import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import androidx.core.os.ConfigurationCompat
import com.pdfjet.*

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import java.io.*
import java.lang.Exception
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

/**
 * AsyncTask subclass used to move the PDF generation procedure to a background thread
 * and inform the UI of the progress.
 */
class PDFGeneratorTask(context: Context) : AsyncTask<List<TransferDetail>, Int, String>() {

    companion object {
        private const val TAG = "PDFGeneratorTask"
    }

    private val mContextRef: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(vararg params: List<TransferDetail>): String {
        return createPDFDocument(params[0])
    }

    private fun combinePath(path1: String, path2: String): String {
        val file1 = File(path1)
        val file2 = File(file1, path2)
        return file2.path
    }

    /** Creates an empty file with the given name, in case it does not exist */
    private fun createEmptyFile(path: String) {
        try {
            val file = File(path)
            val writer = FileWriter(file)
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun createPDFDocument(transferDetails: List<TransferDetail>): String {
        return try {
            // Create the PDF file name
            val fileName = mContextRef.get()?.resources?.let {
                "${it.getString(R.string.app_name)}-${it.getString(R.string.title_transactions)}"} + ".pdf"

            // Obtains the path to store the PDF
            val externalStorageFolder = Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    Constants.EXTERNAL_STORAGE_FOLDER
            val filePath = combinePath(externalStorageFolder, fileName)
            createEmptyFile(filePath)

            // Creates a new PDF object
            val pdf = PDF(
                BufferedOutputStream(
                    FileOutputStream(filePath)), Compliance.PDF_A_1B)


            // Font used for the table headers
            val f1 = Font(pdf, CoreFont.HELVETICA_BOLD)
            f1.size = 7f

            // Font used for the table contents
            val f2 = Font(pdf, CoreFont.HELVETICA)
            f2.size = 7f

            // Creates a new PDF table
            val table = Table()

            // 2D array of cells used to populate the PDF table
            val tableData = ArrayList<List<Cell>>()

            // Add column names/headers
            val columnNames = intArrayOf(R.string.title_from, R.string.title_to, R.string.title_memo, R.string.title_date,
                                    R.string.title_time, R.string.title_amount, R.string.title_equivalent_value)

            val header = ArrayList<Cell>()

            for (columnName in columnNames) {
                val cell = Cell(f1, mContextRef.get()?.getString(columnName))
                cell.setTopPadding(2f)
                cell.setBottomPadding(2f)
                cell.setLeftPadding(2f)
                cell.setRightPadding(2f)
                header.add(cell)
            }

            // Add the table headers
            tableData.add(header)

            // Add the table contents
            mContextRef.get()?.let { context ->
                val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
                tableData.addAll(getData(transferDetails, f2, locale))
            }

            // Configure the PDF table
            table.setData(tableData, Table.DATA_HAS_1_HEADER_ROWS)
            table.setCellBordersWidth(0.2f)
            // The A4 size has 595 points of width, with the below we are trying to assign the same
            // width to all cells and also keep them centered.
            for (i in 0..6) {
                table.setColumnWidth(i, 65f)
            }
            table.wrapAroundCellText()
            table.mergeOverlaidBorders()

            // Populate the PDF table
            while (table.hasMoreData()) {
                // Configures the PDF page
                val page = Page(pdf, Letter.PORTRAIT)
                table.setLocation(45f, 30f)
                table.drawOn(page)
            }

            pdf.close()

            "PDF generated and saved: $filePath"
        } catch (e: Exception) {
            Log.e(TAG, "Exception while trying to generate a PDF. Msg: " + e.message)
            "Unable to generate PDF. Please retry. Error: ${e.message}"
        }
    }

    private fun getData(transferDetails: List<TransferDetail>, font: Font, locale: Locale): List<List<Cell>> {

        val tableData = ArrayList<List<Cell>>()

        // Configure date and time formats to reuse in all the transfers
        val dateFormat = SimpleDateFormat("MM-dd-yyyy", locale)
        val timeFormat = SimpleDateFormat("HH:mm:ss", locale)
        var date: Date

        // Save all the transfers information
        for ( (index, transferDetail) in transferDetails.withIndex()) {
            val row = ArrayList<Cell>()

            val cols = ArrayList<String>()

            date = Date(transferDetail.date * 1000)

            cols.add(transferDetail.from ?: "")                 // From
            cols.add(transferDetail.to ?: "")                   // To
            cols.add(transferDetail.memo)                       // Memo
            cols.add(dateFormat.format(date))                   // Date
            cols.add(timeFormat.format(date))                   // Time

            // Asset Amount
            val assetPrecision = transferDetail.assetPrecision
            val assetAmount = transferDetail.assetAmount.toDouble() / Math.pow(10.0, assetPrecision.toDouble())
            cols.add(String.format("%.${assetPrecision}f %s", assetAmount, transferDetail.assetSymbol))

            // Fiat Equivalent
            if (transferDetail.fiatAmount != null && transferDetail.fiatSymbol != null) {
                val currency = Currency.getInstance(transferDetail.fiatSymbol)
                val fiatAmount = transferDetail.fiatAmount.toDouble() /
                        Math.pow(10.0, currency.defaultFractionDigits.toDouble())
                cols.add(String.format("%.${currency.defaultFractionDigits}f %s",
                    fiatAmount, currency.currencyCode))
            }

            for (col in cols) {
                val cell = Cell(font, col)
                cell.setTopPadding(2f)
                cell.setBottomPadding(2f)
                cell.setLeftPadding(2f)
                cell.setRightPadding(2f)
                row.add(cell)
            }
            tableData.add(row)

            appendMissingCells(tableData, font)

            // TODO update progress
        }

        return tableData
    }

    private fun appendMissingCells(tableData: List<List<Cell>>, font: Font) {
        val firstRow = tableData[0]
        val numOfColumns = firstRow.size
        for (i in tableData.indices) {
            val dataRow = tableData[i] as ArrayList<Cell>
            val dataRowColumns = dataRow.size
            if (dataRowColumns < numOfColumns) {
                for (j in 0 until numOfColumns - dataRowColumns) {
                    dataRow.add(Cell(font))
                }
                dataRow[dataRowColumns - 1].colSpan = numOfColumns - dataRowColumns + 1
            }
        }
    }


    override fun onProgressUpdate(values: Array<Int>) {
        // TODO show progress
    }

    override fun onPostExecute(message: String) {
        mContextRef.get()?.toast(message)
    }
}