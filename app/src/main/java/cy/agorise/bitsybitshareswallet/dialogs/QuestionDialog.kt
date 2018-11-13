package cy.agorise.bitsybitshareswallet.dialogs

import android.app.Activity
import cy.agorise.bitsybitshareswallet.R


/*
* This class is used to show a question dialog
*
*
* Use example:
*
*
* var questionDialog:QuestionDialog = QuestionDialog(this)
        questionDialog.setText(R.string.no_amount_requested)
        questionDialog.show()

* */
class QuestionDialog : CrystalDialog {

    constructor(activity: Activity) : super(activity) {

        /*
        * Create the buttons needed
        * */
        this.builder.positiveButton(R.string.ok)
        this.builder.negativeButton(R.string.cancel)
    }
}