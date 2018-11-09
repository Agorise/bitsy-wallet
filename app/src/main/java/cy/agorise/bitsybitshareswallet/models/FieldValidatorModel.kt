package cy.agorise.bitsybitshareswallet.models

class FieldValidatorModel {

    /*
    * Determine if the field is valid
    * */
    /*
    * Setters and getters
    * */
    var isValid: Boolean = false

    /*
    * Contains the message of the error
    * */
    var message: String? = null
    /*
     * End of setters and getters
     * */


    /*
    * Set tha the field is invalid
    * */
    fun setInvalid() {
        this.isValid = false
    }


    /*
     * Set tha the field is valid
     * */
    fun setValid() {
        this.isValid = true
    }
}
