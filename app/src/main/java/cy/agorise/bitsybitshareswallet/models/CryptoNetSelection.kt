package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import cy.agorise.bitsybitshareswallet.enums.CryptoNet

class CryptoNetSelection(var cryptoNet: CryptoNet, var selected: Boolean?) {
    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<CryptoNetSelection> =
            object : DiffUtil.ItemCallback<CryptoNetSelection>() {
                override fun areItemsTheSame(
                    @NonNull oldCryptoNetSelection: CryptoNetSelection, @NonNull newCryptoNetSelection: CryptoNetSelection
                ): Boolean {
                    return oldCryptoNetSelection.cryptoNet === newCryptoNetSelection.cryptoNet
                }

                override fun areContentsTheSame(
                    @NonNull oldCryptoNetSelection: CryptoNetSelection, @NonNull newCryptoNetSelection: CryptoNetSelection
                ): Boolean {
                    return oldCryptoNetSelection == newCryptoNetSelection
                }
            }
    }
}
