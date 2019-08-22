package cy.agorise.bitsybitshareswallet.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import cy.agorise.bitsybitshareswallet.fragments.TransactionsFragment

/**
 * Model that includes all the options to filter the transactions in the [TransactionsFragment]
 */
@Parcelize
data class FilterOptions (
    var query: String = "",
    var transactionsDirection: Int = 0,
    var dateRangeAll: Boolean = true,
    var startDate: Long = 0L,
    var endDate: Long = 0L,
    var assetAll: Boolean = true,
    var asset: String = "BTS",
    var equivalentValueAll: Boolean = true,
    var fromEquivalentValue: Long = 0L,
    var toEquivalentValue: Long = 5000L,
    var agoriseFees: Boolean = true
) : Parcelable