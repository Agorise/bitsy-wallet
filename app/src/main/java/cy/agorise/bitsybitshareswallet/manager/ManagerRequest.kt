package cy.agorise.bitsybitshareswallet.manager

interface ManagerRequest {

    fun success(answer: Any)

    fun fail()
}