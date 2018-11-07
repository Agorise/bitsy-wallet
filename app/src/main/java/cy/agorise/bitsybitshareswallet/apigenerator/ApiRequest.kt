package cy.agorise.bitsybitshareswallet.apigenerator;

class ApiRequest
/**
 * Basic constructor
 * @param id The id of this request
 * @param listener The listener for this request
 */
    (
    /**
     * The id of this api request
     */
    var id: Int,
    /**
     * The listener of this apirequest, to be passed the answer
     */
    var listener: ApiRequestListener
)

