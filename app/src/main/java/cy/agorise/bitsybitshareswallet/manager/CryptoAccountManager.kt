package cy.agorise.bitsybitshareswallet.manager

import android.content.Context
import cy.agorise.bitsybitshareswallet.enums.CryptoNetAccount

interface CryptoAccountManager {

    /**
     * Creates a CryptoCoin Account, with the values of the account
     * @param account The values to be created,
     * @returnThe CruptoNetAccount created, or null if it couldn't be created
     */
    fun createAccountFromSeed(account: CryptoNetAccount, request: ManagerRequest, context: Context)

    /**
     * Imports a CryptoCoin account from a seed
     * @param account A CryptoNetAccount with the parameters to be imported
     * @returnThe CruptoNetAccount imported
     */
    fun importAccountFromSeed(account: CryptoNetAccount, context: Context)

    /**
     * Loads account data from the database
     *
     * @param account The CryptoNetAccount to be loaded
     */
    fun loadAccountFromDB(account: CryptoNetAccount, context: Context)


}
