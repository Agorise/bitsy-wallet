package cy.agorise.bitsybitshareswallet.dialogs

import android.app.Activity
import com.afollestad.materialdialogs.MaterialDialog
import cy.agorise.bitsybitshareswallet.R

/*
*
* Controls the custom implementarion for all kind of material dialogs
* Reference in: https://github.com/afollestad/material-dialogs
*
* */
open abstract class DialogMaterial{

    protected var builder: MaterialDialog //Contains the builder
    protected lateinit var materialDialog: MaterialDialog //Contains the controller for the dialog

    /*
    * Contains the activity
    * */
    protected var activity:Activity;

    /*
    * Contains pointer to myself
    * */
    protected var dialogMaterial:DialogMaterial;

    /*
    * Contains the response for positive button click
    * */
    protected var positiveResponse:PositiveResponse? = null

    /*
    * Contains the response for negative button click
    * */
    protected var negativeResponse:NegativeResponse? = null


    constructor(activity: Activity) {

        /*
        * Save the activity
        * */
        this.activity = activity

        dialogMaterial = this;

        /*
        *   Init the builder
        * */
        builder = MaterialDialog(activity)
        builder.cancelable(false)
    }

    /*
    * Show the dialog
    * */
    fun show() {

        /*
        * If user wants positive and negative
        * */
        if(positiveResponse != null && negativeResponse != null){

            /*
            * Add positve
            * */
            builder.positiveButton(R.string.ok) { dialog ->

                /*
                * If response is not null deliver response
                * */
                if(positiveResponse != null){
                    positiveResponse!!.onPositive()
                }
            }

            /*
            * Add negative
            * */
            builder.negativeButton(R.string.cancel){ dialog ->

                /*
                * If response is not null deliver response
                * */
                if(negativeResponse != null){
                    negativeResponse!!.onNegative(dialogMaterial)
                }
            }
        }

        /*
        * If user wants positive button
        * */
        if(positiveResponse != null){
            builder.positiveButton(R.string.ok){ dialog ->

                /*
                * If response is not null deliver response
                * */
                if(positiveResponse != null){
                    positiveResponse!!.onPositive()
                }
            }
        }

        /*
        * Build internal material dialog, this lets to show it
        * */
        this.build()

        /*
        * Show the dialog
        * */
        builder.show()
    }

    /*
    * Close the dialog
    * */
    fun dismiss() {
        this.materialDialog.dismiss()
    }

    /*
    * After the class is completed as needed, we need to call this method to join all together after show the
    * childs implementations
    * */
    open fun build() {

    }

    /*
    * Set indeterminate progress
    *
    * */
    open fun progress(){
        this.builder.icon(R.drawable.loading)
    }

    /*
    * Setters and getters
    * */
    fun setText(message: Int) {
        this.builder.message(message)
    }

    fun setOnPositive(onPositive:PositiveResponse){
        this.positiveResponse = onPositive
    }

    fun setOnNegative(onNegative:NegativeResponse){
        this.negativeResponse = onNegative
    }

    fun setTitle(title: Int) {
        this.builder.title(title)
    }
    /*
     * End of setters and getters
     * */
}