package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.google.common.primitives.UnsignedLong
import cy.agorise.bitsybitshareswallet.database.entities.Transfer
import cy.agorise.bitsybitshareswallet.repositories.AssetRepository
import cy.agorise.bitsybitshareswallet.repositories.TransferRepository
import cy.agorise.graphenej.Asset
import cy.agorise.graphenej.AssetAmount
import cy.agorise.graphenej.Converter
import cy.agorise.graphenej.models.BucketObject


class TransferViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "TransferViewModel"
    private var mTransferRepository = TransferRepository(application)
    private var mAssetRepository = AssetRepository(application)

    internal fun setBlockTime(blockNumber: Long, timestamp: Long) {
        mTransferRepository.setBlockTime(blockNumber, timestamp)
    }

    internal fun getTransferBlockNumberWithMissingTime(): LiveData<Long> {
        return mTransferRepository.getTransferBlockNumberWithMissingTime()
    }

    fun getTransfersWithMissingBtsValue() : LiveData<Transfer> {
        return mTransferRepository.getTransfersWithMissingBtsValue()
    }

    fun updateBtsValue(transfer: Transfer, bucket: BucketObject) {
        val base = mAssetRepository.getAssetDetails(bucket.key.base.objectId) // Always BTS ?
        val quote = mAssetRepository.getAssetDetails(bucket.key.quote.objectId) // Any asset other than BTS
        val converter = Converter(Asset(base.id, base.symbol, base.precision), Asset(quote.id, quote.symbol, quote.precision), bucket)
        // The "base" amount is always the amount we have, and the quote would be the amount we want to obtain.
        // It can be strange that the second argument of the AssetAmount constructor here we pass the quote.id, quote.symbol and quote.precision
        // when building the "base" amount instance. But this is just because the full node will apparently always treat BTS as the base.
        val baseAmount = AssetAmount(UnsignedLong.valueOf(transfer.transferAmount), Asset(quote.id, quote.symbol, quote.precision))
        val quoteAmount = converter.convert(baseAmount, Converter.OPEN_VALUE)
        transfer.btsValue = quoteAmount
        mTransferRepository.update(transfer)
    }

    fun updateBtsValue(transfer: Transfer, value: Long) {
        transfer.btsValue = value
        mTransferRepository.update(transfer)
    }
}
