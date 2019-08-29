package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.LongSparseArray
import androidx.navigation.fragment.findNavController
import com.jakewharton.rxbinding3.widget.textChanges
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.network.FaucetService
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.containsDigits
import cy.agorise.bitsybitshareswallet.utils.containsVowels
import cy.agorise.bitsybitshareswallet.utils.toast
import cy.agorise.graphenej.Address
import cy.agorise.graphenej.BrainKey
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.calls.GetAccountByName
import cy.agorise.graphenej.models.AccountProperties
import cy.agorise.graphenej.models.JsonRpcResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_create_account.*
import org.bitcoinj.core.ECKey
import retrofit2.Callback
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.Crashlytics
import cy.agorise.bitsybitshareswallet.models.FaucetRequest
import cy.agorise.bitsybitshareswallet.models.FaucetResponse
import cy.agorise.bitsybitshareswallet.network.ServiceGenerator
import retrofit2.Call
import retrofit2.Response


class CreateAccountFragment : BaseAccountFragment() {

    companion object {
        private const val TAG = "CreateAccountFragment"

        private const val BRAINKEY_FILE = "brainkeydict.txt"
        private const val MIN_ACCOUNT_NAME_LENGTH = 8

        // Used when trying to validate that the account name is available
        private const val RESPONSE_GET_ACCOUNT_BY_NAME_VALIDATION = 1
        // Used when trying to obtain the info of the newly created account
        private const val RESPONSE_GET_ACCOUNT_BY_NAME_CREATED = 2
    }

    private lateinit var mAddress: String

    /** Variables used to store the validation status of the form fields */
    private var isPINValid = false
    private var isPINConfirmationValid = false
    private var isAccountValidAndAvailable = false

    // Map used to keep track of request and response id pairs
    private val responseMap = LongSparseArray<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_create_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Crashlytics.setString(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        // Use RxJava Debounce to check the validity and availability of the user's proposed account name
        mDisposables.add(
            tietAccountName.textChanges()
                .skipInitialValue()
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { validateAccountName(it.toString()) },
                    { Crashlytics.log(Log.DEBUG, TAG, it.message) }
                )
        )

        // Use RxJava Debounce to update the PIN error only after the user stops writing for > 500 ms
        mDisposables.add(
            tietPin.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { validatePIN() },
                    { Crashlytics.log(Log.DEBUG, TAG, it.message) }
                )
        )

        // Use RxJava Debounce to update the PIN Confirmation error only after the user stops writing for > 500 ms
        mDisposables.add(
            tietPinConfirmation.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { validatePINConfirmation() },
                    { Crashlytics.log(Log.DEBUG, TAG, it.message) }
                )
        )

        btnCancel.setOnClickListener { findNavController().navigateUp() }

        btnCreate.isEnabled = false
        btnCreate.setOnClickListener { createAccount() }

        // Generating BrainKey
        generateKeys()
    }

    private fun validateAccountName(accountName: String) {
        isAccountValidAndAvailable = false

        if ( !isAccountNameValid(accountName) ) {
            tilAccountName.helperText = ""
            tilAccountName.error = getString(R.string.error__invalid_account_name)
        } else {
            tilAccountName.isErrorEnabled = false
            tilAccountName.helperText = getString(R.string.text__verifying_account_availability)
            val id = mNetworkService?.sendMessage(GetAccountByName(accountName), GetAccountByName.REQUIRED_API)

            if (id != null)
                responseMap.append(id, RESPONSE_GET_ACCOUNT_BY_NAME_VALIDATION)
        }

        enableDisableCreateButton()
    }

    /**
     * Method used to determine if the account name entered by the user is valid
     * @param accountName   The proposed account name
     * @return              True if the name is valid, false otherwise
     */
    private fun isAccountNameValid(accountName: String): Boolean {
        return accountName.length >= MIN_ACCOUNT_NAME_LENGTH &&
                (accountName.containsDigits() || !accountName.containsVowels()) &&
                !accountName.contains("_")
    }

    private fun validatePIN() {
        val pin = tietPin.text.toString()

        if (pin.length < Constants.MIN_PIN_LENGTH) {
            tilPin.error = getString(R.string.error__pin_too_short)
            isPINValid = false
        } else {
            tilPin.isErrorEnabled = false
            isPINValid = true
        }

        validatePINConfirmation()
    }

    private fun validatePINConfirmation() {
        val pinConfirmation = tietPinConfirmation.text.toString()

        if (pinConfirmation != tietPin.text.toString()) {
            tilPinConfirmation.error = getString(R.string.error__pin_mismatch)
            isPINConfirmationValid = false
        } else {
            tilPinConfirmation.isErrorEnabled = false
            isPINConfirmationValid = true
        }

        enableDisableCreateButton()
    }

    private fun enableDisableCreateButton() {
        btnCreate.isEnabled =  (isPINValid && isPINConfirmationValid && isAccountValidAndAvailable)
    }

    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {
        if (responseMap.containsKey(response.id)) {
            val responseType = responseMap[response.id]
            when (responseType) {
                RESPONSE_GET_ACCOUNT_BY_NAME_VALIDATION -> handleAccountNameValidation(response.result)
                RESPONSE_GET_ACCOUNT_BY_NAME_CREATED    -> handleAccountNameCreated(response.result)
            }
            responseMap.remove(response.id)
        }
    }

    override fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate) { }

    /**
     * Handles the response from the NetworkService's GetAccountByName call to decide if the user's suggested
     * account is available or not.
     */
    private fun handleAccountNameValidation(result: Any?) {
        if (result is AccountProperties) {
            tilAccountName.helperText = ""
            tilAccountName.error = getString(R.string.error__account_not_available)
            isAccountValidAndAvailable = false
        } else {
            tilAccountName.isErrorEnabled = false
            tilAccountName.helperText = getString(R.string.text__account_is_available)
            isAccountValidAndAvailable = true
        }

        enableDisableCreateButton()
    }

    /**
     * Handles the response from the NetworkService's GetAccountByName call and stores the information of the newly
     * created account if the result is successful, shows a toast error otherwise
     */
    private fun handleAccountNameCreated(result: Any?) {
        if (result is AccountProperties) {
            onAccountSelected(result, tietPin.text.toString())
        } else {
            context?.toast(getString(R.string.error__created_account_not_found))
            setStateError()
        }
    }

    /**
     * Sets the state to Loading, when the app is trying to create an account and waiting for the response.
     */
    private fun setStateLoading() {
        btnCancel.isEnabled = false
        btnCreate.isEnabled = false
        progressBar.visibility = View.VISIBLE
    }

    /**
     * Sets the state to Error, when the app is unable to create the account or unable to retrieve
     * the information from the newly created account.
     */
    private fun setStateError() {
        btnCancel.isEnabled = true
        btnCreate.isEnabled = false
        progressBar.visibility = View.GONE
    }

    /**
     * Sends the account-creation request to the faucet server.
     * Only account name and public address is sent here.
     */
    private fun createAccount() {
        setStateLoading()

        val accountName = tietAccountName.text.toString()
        val faucetRequest = FaucetRequest(accountName, mAddress, Constants.FAUCET_REFERRER)

        val sg = ServiceGenerator(Constants.FAUCET_URL)
        val faucetService = sg.getService(FaucetService::class.java)

        val call = faucetService.registerPrivateAccount(faucetRequest)

        // Execute the call asynchronously. Get a positive or negative callback.
        call.enqueue(object : Callback<FaucetResponse> {
            override fun onResponse(call: Call<FaucetResponse>, response: Response<FaucetResponse>) {
                // The network call was a success and we got a response, obtain the info of the newly created account
                // with a delay to let the nodes update their information
                val handler = Handler()

                handler.postDelayed({
                    getCreatedAccountInfo(response.body())
                }, 4000)
            }

            override fun onFailure(call: Call<FaucetResponse>, t: Throwable) {
                // the network call was a failure
                context?.let { context ->
                    MaterialDialog(context).show {
                        title(R.string.title_error)
                        message(R.string.error__faucet)
                        negativeButton(android.R.string.ok)
                    }
                }

                setStateError()
            }
        })
    }

    private fun getCreatedAccountInfo(faucetResponse: FaucetResponse?) {
        if (faucetResponse?.account != null) {
            val id = mNetworkService?.sendMessage(GetAccountByName(faucetResponse.account?.name),
                GetAccountByName.REQUIRED_API)

            if (id != null)
                responseMap.append(id, RESPONSE_GET_ACCOUNT_BY_NAME_CREATED)
        } else {
            Log.d(TAG, "Private account creation failed ")
            val content = if (faucetResponse?.error?.base?.size ?: 0 > 0) {
                getString(R.string.error__faucet_template, faucetResponse?.error?.base?.get(0))
            } else {
                getString(R.string.error__faucet_template, "None")
            }

            context?.let {context ->
                MaterialDialog(context)
                    .title(R.string.title_error)
                    .message(text = content)
                    .show()
            }

            setStateError()
        }
    }

    /**
     * Method that generates a fresh key that will be controlling the newly created account.
     */
    private fun generateKeys() {
        var reader: BufferedReader? = null
        val dictionary: String
        try {
            reader = BufferedReader(InputStreamReader(context!!.assets.open(BRAINKEY_FILE), "UTF-8"))
            dictionary = reader.readLine()

            val brainKeySuggestion = BrainKey.suggest(dictionary)
            mBrainKey = BrainKey(brainKeySuggestion, 0)
            val address = Address(ECKey.fromPublicOnly(mBrainKey?.privateKey?.pubKey))
            Log.d(TAG, "brain key: $brainKeySuggestion")
            Log.d(TAG, "address would be: $address")
            mAddress = address.toString()
            tvBrainKey.text = mBrainKey?.brainKey

        } catch (e: IOException) {
            Log.e(TAG, "IOException while trying to generate key. Msg: " + e.message)
            context?.toast(getString(R.string.error__read_dict_file))
            findNavController().navigateUp()
        } catch (e: IllegalArgumentException) {
            Crashlytics.logException(e)
            // TODO if this does happen to real devices, use a proper error message
            context?.toast(getString(R.string.error__try_again))
            findNavController().navigateUp()
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    Log.e(TAG, "IOException while trying to close BufferedReader. Msg: " + e.message)
                }

            }
        }
    }
}