package cy.agorise.bitsybitshareswallet.dialogs

import android.app.Activity
import cy.agorise.bitsybitshareswallet.R


/*
*
* Class to just call simple loading dialog
*
* Sumple Use:
*
* var loading:Loading = Loading(this)
        loading.show()
*
* */
open class Loading : CrystalDialog {

    constructor(activity:Activity) : super(activity) {

        /*
        * Set loading properties only
        * */
        this.progress()
        this.setTitle(R.string.loading)
    }
}