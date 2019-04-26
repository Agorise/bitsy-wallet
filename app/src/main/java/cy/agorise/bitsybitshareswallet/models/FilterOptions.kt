package cy.agorise.bitsybitshareswallet.models

import android.os.Parcel
import android.os.Parcelable

/**
 * Model that includes all the options to filter the transactions in the [TransactionsFragment]
 */
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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(query)
        parcel.writeInt(transactionsDirection)
        parcel.writeByte(if (dateRangeAll) 1 else 0)
        parcel.writeLong(startDate)
        parcel.writeLong(endDate)
        parcel.writeByte(if (assetAll) 1 else 0)
        parcel.writeString(asset)
        parcel.writeByte(if (equivalentValueAll) 1 else 0)
        parcel.writeLong(fromEquivalentValue)
        parcel.writeLong(toEquivalentValue)
        parcel.writeByte(if (agoriseFees) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FilterOptions> {
        override fun createFromParcel(parcel: Parcel): FilterOptions {
            return FilterOptions(parcel)
        }

        override fun newArray(size: Int): Array<FilterOptions?> {
            return arrayOfNulls(size)
        }
    }

}