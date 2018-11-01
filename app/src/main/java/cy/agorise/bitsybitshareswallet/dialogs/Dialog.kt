package cy.agorise.bitsybitshareswallet.dialogs

import android.app.Activity

/*
* Dialog material that shows loading gif and and explicit message
* */
open class CrystalDialog : DialogMaterial{

    constructor(activity: Activity) : super(activity) {

        /*
        * Prepare the dialog
        * */
        //this.builder.title(-1)
    }
}

/*
*   Internal interfaces
* */
interface PositiveResponse{
    fun onPositive()
}
interface NegativeResponse{
    fun onNegative(dialogMaterial:DialogMaterial)
}