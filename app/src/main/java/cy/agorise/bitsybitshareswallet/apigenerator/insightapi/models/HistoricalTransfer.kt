package cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models

import cy.agorise.graphenej.operations.TransferOperation

class HistoricalTransfer {
    var id: String? = null
    var operation: TransferOperation? = null
    var result: Array<Any>? = null
    var blockNum: Long = 0
    var transactionsInBlock: Long = 0
    var operationsInTrx: Long = 0
    var virtualOp: Long = 0
}