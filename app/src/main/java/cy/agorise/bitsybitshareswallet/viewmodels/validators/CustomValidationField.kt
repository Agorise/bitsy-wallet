package cy.agorise.bitsybitshareswallet.viewmodels.validators

import android.app.Activity
import android.view.View
import cy.agorise.bitsybitshareswallet.interfaces.UIValidatorListener

open class CustomValidationField {

    /*
     * Listener to deliver response to controller
     * */
    protected lateinit var uiValidatorListener: UIValidatorListener

    /*
    * Contains the field to validate
    * */
    @JvmField var currentView : View? = null


    /*
    * Contains a handler to my self
    * */
    @JvmField protected var globalCustomValidationField: CustomValidationField

    /*
    * Contains the acivity for utility
    * */
    @JvmField protected var activity: Activity


    constructor(activity: Activity){

        /*
        * Save the activity
        * */
        this.activity = activity

        /*
        * Init the custom field for later references
        * */
        this.globalCustomValidationField = this
    }
}