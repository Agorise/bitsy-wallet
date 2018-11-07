package cy.agorise.bitsybitshareswallet.models

import androidx.room.Embedded
import androidx.room.Relation

class BitcoinTransactionExtended {

    @Embedded
    var cryptoCoinTransaction: CryptoCoinTransaction? = null

    @Embedded
    var bitcoinTransaction: BitcoinTransaction? = null

    @Relation(parentColumn = "id", entityColumn = "bitcoin_transaction_id", entity = BitcoinTransactionGTxIO::class)
    var bitcoinTransactionGTxIOList: List<BitcoinTransactionGTxIO>? = null
}
