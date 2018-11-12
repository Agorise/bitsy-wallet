package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull

class AmbassadorLocation(
    var id: String?, var name: String?,
    /*public Country getCountry(Locale locale){
        return new Country(country, locale);
    }*/

    var country: String?
) : Comparable<AmbassadorLocation> {

    override fun toString(): String {
        return this!!.name!!
    }

    override fun equals(obj: Any?): Boolean {
        return obj is AmbassadorLocation && id == obj.id
    }

    override fun compareTo(@NonNull ambassadorLocation: AmbassadorLocation): Int {
        return name!!.compareTo(ambassadorLocation.name!!)
    }
}

