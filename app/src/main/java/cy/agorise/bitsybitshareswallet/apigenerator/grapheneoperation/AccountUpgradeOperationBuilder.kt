package cy.agorise.bitsybitshareswallet.apigenerator.grapheneoperation

import cy.agorise.graphenej.AssetAmount
import cy.agorise.graphenej.UserAccount
import cy.agorise.graphenej.errors.MalformedOperationException
import cy.agorise.graphenej.operations.BaseOperationBuilder

class AccountUpgradeOperationBuilder : BaseOperationBuilder() {

    private var accountToUpgrade: UserAccount? = null
    private var fee: AssetAmount? = null
    private var isUpgrade = true

    fun setAccountToUpgrade(accountToUpgrade: UserAccount): AccountUpgradeOperationBuilder {
        this.accountToUpgrade = accountToUpgrade
        return this
    }

    fun setFee(fee: AssetAmount): AccountUpgradeOperationBuilder {
        this.fee = fee
        return this
    }

    fun setIsUpgrade(isUpgrade: Boolean?): AccountUpgradeOperationBuilder {
        this.isUpgrade = isUpgrade!!
        return this
    }

    override fun build(): AccountUpgradeOperation {
        val accountUpgrade: AccountUpgradeOperation
        if (accountToUpgrade == null) {
            throw MalformedOperationException("Missing account to upgrade information")
        }

        if (fee != null) {
            accountUpgrade = AccountUpgradeOperation(accountToUpgrade, isUpgrade, fee)
        } else {
            accountUpgrade = AccountUpgradeOperation(accountToUpgrade, isUpgrade)
        }
        return accountUpgrade
    }
}
