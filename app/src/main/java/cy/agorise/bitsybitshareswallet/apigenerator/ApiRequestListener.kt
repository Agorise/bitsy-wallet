package cy.agorise.bitsybitshareswallet.apigenerator

interface ApiRequestListener {

    /**
     * Call when the function returns successfully
     * @param answer The answer, this object depends on the kind of request is made to the api
     * @param idPetition the id of the ApiRequest petition
     */
    fun success(answer: Any?, idPetition: Int)

    /**
     * Call when the function fails
     * @param idPetition the id of the ApiRequest petition
     */
    fun fail(idPetition: Int)

}


