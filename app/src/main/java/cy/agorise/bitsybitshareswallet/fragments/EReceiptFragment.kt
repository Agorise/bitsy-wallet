package cy.agorise.bitsybitshareswallet.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.databinding.FragmentEReceiptBinding
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.Helper
import cy.agorise.bitsybitshareswallet.utils.toast
import cy.agorise.bitsybitshareswallet.viewmodels.EReceiptViewModel
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class EReceiptFragment : Fragment() {

    companion object {
        private const val TAG = "EReceiptFragment"

        private const val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 100
    }

    private val args: EReceiptFragmentArgs by navArgs()

    private val viewModel: EReceiptViewModel by viewModels()

    private var _binding: FragmentEReceiptBinding? = null
    private val binding get() = _binding!!

    private lateinit var mLocale: Locale

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        _binding = FragmentEReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        mLocale = ConfigurationCompat.getLocales(resources.configuration)[0]

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "") ?: ""

        val transferId = args.transferId

        viewModel.get(userId, transferId).observe(viewLifecycleOwner, { transferDetail ->
            bindTransferDetail(transferDetail)
        })
    }

    private fun bindTransferDetail(transferDetail: TransferDetail) {
        context?.let { context ->
            val colorRes = if (transferDetail.direction) R.color.colorReceive else R.color.colorSend
            binding.vPaymentDirection.setBackgroundColor(ContextCompat.getColor(context, colorRes))
        }

        binding.tvFrom.text = transferDetail.from ?: ""
        binding.tvTo.text = transferDetail.to ?: ""

        // Show the crypto amount correctly formatted
        val df = DecimalFormat("####." + ("#".repeat(transferDetail.assetPrecision)))
        df.roundingMode = RoundingMode.CEILING
        df.decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())

        val amount = transferDetail.assetAmount.toDouble() /
                Math.pow(10.toDouble(), transferDetail.assetPrecision.toDouble())
        val assetAmount = "${df.format(amount)} ${transferDetail.getUIAssetSymbol()}"
        binding.tvAmount.text = assetAmount

        // Fiat equivalent
        if (transferDetail.fiatAmount != null && transferDetail.fiatSymbol != null) {
            val numberFormat = NumberFormat.getNumberInstance()
            val currency = Currency.getInstance(transferDetail.fiatSymbol)
            val fiatEquivalent = transferDetail.fiatAmount.toDouble() /
                    Math.pow(10.0, currency.defaultFractionDigits.toDouble())

            val equivalentValue = "${numberFormat.format(fiatEquivalent)} ${currency.currencyCode}"
            binding.tvEquivalentValue.text = equivalentValue
        } else {
            binding.tvEquivalentValue.text = "-"
        }

        // Memo
        if (transferDetail.memo != "")
            binding.tvMemo.text = getString(R.string.template__memo, transferDetail.memo)
        else
            binding.tvMemo.visibility = View.GONE

        // Date
        val dateFormat = SimpleDateFormat("dd MMM HH:mm:ss z", mLocale)
        binding.tvDate.text =
            getString(R.string.template__date, dateFormat.format(transferDetail.date * 1000))

        // Transaction #
        formatTransferTextView(transferDetail.id)
    }

    /** Formats the transfer TextView to show a link to explore the given transfer
     * in a BitShares explorer */
    private fun formatTransferTextView(transferId: String) {
        val html = getString(
            R.string.template__tx,
            "<a href=\"http://blocksights.info/#/operations/$transferId\">$transferId</a>"
        )
        val tx = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.tvTransferID.text = tx
        binding.tvTransferID.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_e_receipt, menu)

        // Animate the share icon
        val shareIcon = menu.findItem(R.id.menu_share).icon
        if (shareIcon is Animatable) {
            shareIcon.start()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_share) {
            verifyStoragePermission()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /** Verifies if the storage permission is already granted, if that is the case then it takes the screenshot and
     * shares it but if it is not then it asks the user for that permission */
    private fun verifyStoragePermission() {
        if (ContextCompat
                .checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not already granted
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
            )
        } else {
            // Permission is already granted
            shareEReceiptScreenshot()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                shareEReceiptScreenshot()
            } else {
                context?.toast(getString(R.string.msg__storage_permission_necessary_share))
            }
            return
        }
    }

    /** Takes a screenshot as a bitmap (hiding the tx hyperlink), saves it into a temporal cache image and then
     * sends an intent so the user can select the desired method to share the image. */
    private fun shareEReceiptScreenshot() {
        // Get Screenshot
        binding.tvTransferID.text = getString(R.string.template__tx, args.transferId)
        val screenshot = Helper.loadBitmapFromView(binding.container)
        formatTransferTextView(args.transferId)
        val imageUri = context?.let { Helper.saveTemporalBitmap(it, screenshot) }

        // Prepare information for share intent
        val subject = "${getString(R.string.app_name)} ${getString(R.string.title_e_receipt)}"

        // Create share intent and call it
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        shareIntent.putExtra(Intent.EXTRA_TEXT, subject)
        shareIntent.type = "*/*"
        startActivity(Intent.createChooser(shareIntent, getString(R.string.text__share_with)))
    }
}