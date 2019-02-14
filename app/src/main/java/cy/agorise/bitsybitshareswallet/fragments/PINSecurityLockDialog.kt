package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.jakewharton.rxbinding3.widget.textChanges
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.CryptoUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.dialog_pin_security_lock.*

/**
 * Contains all the specific logic to create and confirm a new PIN or verifying the validity of the current one.
 */
class PINSecurityLockDialog : BaseSecurityLockDialog() {

    companion object {
        const val TAG = "PINSecurityLockDialog"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.dialog_pin_security_lock, container, false)
    }

    private var newPIN = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupScreen()

        // Listens to the event when the user clicks the 'Enter' button in the keyboard and acts accordingly
        tietPIN.setOnEditorActionListener { v, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_GO) {
                if (currentStep == STEP_SECURITY_LOCK_VERIFY) {
                    // The user just wants to verify the current encrypted PIN/Pattern
                    val encryptedPIN = CryptoUtils.encrypt(v.context, v.text.toString()).trim()

                    if (encryptedPIN == currentEncryptedPINPattern) {
                        // PIN is correct, proceed
                        dismiss()
                        mCallback?.onPINPatternEntered(actionIdentifier)
                    } else {
                        tilPIN.error = getString(R.string.error__wrong_pin)
                    }
                } else if (currentStep == STEP_SECURITY_LOCK_CREATE) {
                    // The user is trying to create a new PIN
                    if (v.text.toString().trim().length >= Constants.MIN_PIN_LENGTH) {
                        // Proceed to the next step only if the PIN has the min length
                        newPIN = v.text.toString().trim()
                        currentStep = STEP_SECURITY_LOCK_CONFIRM
                        setupScreen()
                    }
                } else if (currentStep == STEP_SECURITY_LOCK_CONFIRM) {
                    val pinConfirm = v.text.toString().trim()
                    if (pinConfirm != newPIN) {
                        tvTitle.text = getString(R.string.title__pins_dont_match)
                    } else {
                        val encryptedPIN = CryptoUtils.encrypt(v.context, v.text.toString()).trim()

                        // Stores the newly selected PIN, encrypted
                        PreferenceManager.getDefaultSharedPreferences(v.context).edit()
                            .putString(Constants.KEY_ENCRYPTED_PIN, encryptedPIN)
                            .putInt(Constants.KEY_SECURITY_LOCK_SELECTED, 0).apply() // 0 -> PIN

                        dismiss()
                        mCallback?.onPINPatternChanged()
                    }
                }

                handled = true
            }
            handled
        }

        mDisposables.add(
            tietPIN.textChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (currentStep == STEP_SECURITY_LOCK_VERIFY) {
                        // Make sure the error is removed when the user types again
                        tilPIN.isErrorEnabled = false
                    } else if (currentStep == STEP_SECURITY_LOCK_CREATE) {
                        // Show the min length requirement for the PIN only when it has not been fulfilled
                        if (it.trim().length >= Constants.MIN_PIN_LENGTH) {
                            tilPIN.helperText = ""
                        } else {
                            tilPIN.helperText = getString(R.string.msg__min_pin_length)
                        }
                    }
                }
        )
    }

    private fun setupScreen() {
        when (currentStep) {
            STEP_SECURITY_LOCK_VERIFY -> {
                tvTitle.text = getString(R.string.title__re_enter_your_pin)
                tvSubTitle.text = getString(R.string.msg__enter_your_pin)
                tilPIN.helperText = ""
                tilPIN.isErrorEnabled = false
            }
            STEP_SECURITY_LOCK_CREATE -> {
                tvTitle.text = getString(R.string.title__set_bitsy_screen_lock)
                tvSubTitle.text = getString(R.string.msg__set_bitsy_pin)
                tilPIN.helperText = getString(R.string.msg__min_pin_length)
                tilPIN.isErrorEnabled = false
            }
            STEP_SECURITY_LOCK_CONFIRM -> {
                tvTitle.text = getString(R.string.title__re_enter_your_pin)
                tvSubTitle.text = ""
                tvSubTitle.visibility = View.GONE
                tietPIN.setText("")
                tilPIN.helperText = ""
                tilPIN.isErrorEnabled = false
            }
        }
    }
}