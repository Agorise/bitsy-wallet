package cy.agorise.bitsybitshareswallet.manager

class FeathersResponse<T>(private val error: Throwable?) {
    var total: Long = 0
    var limit: Long = 0
    var skip: Long = 0
    var data: List<T>? = null

    val isSuccessful: Boolean
        get() = error == null && data != null

    fun message(): String {
        return if (error != null) {
            error.message!!
        } else {
            ""
        }
    }
}
