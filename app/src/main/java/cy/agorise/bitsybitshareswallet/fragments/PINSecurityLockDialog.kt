package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jakewharton.rxbinding3.widget.textChanges
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.databinding.DialogPinSecurityLockBinding
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.CryptoUtils
import cy.agorise.bitsybitshareswallet.utils.hideKeyboard
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * Contains all the specific logic to create and confirm a new PIN or verifying the validity of the current one.
 */
class PINSecurityLockDialog : BaseSecurityLockDialog() {

    companion object {
        const val TAG = "PINSecurityLockDialog"
    }

    private var _binding: DialogPinSecurityLockBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DialogPinSecurityLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var newPIN = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        // Request focus to the PIN EditText and automatically show the keyboard when the dialog appears.
        binding.tietPIN.requestFocus()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        setupScreen()

        // Listens to the event when the user clicks the 'Enter' button in the keyboard and acts accordingly
        binding.tietPIN.setOnEditorActionListener { v, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_GO) {
                if (currentStep == STEP_SECURITY_LOCK_VERIFY) {
                    // The user just wants to verify the current hashed PIN/Pattern
                    val pin = v.text.toString().trim()
                    val hashedPIN = CryptoUtils.createSHA256Hash(currentPINPatternSalt + pin)

                    if (hashedPIN == currentHashedPINPattern) {
                        // PIN is correct, proceed
                        resetIncorrectSecurityLockAttemptsAndTime()
                        binding.tietPIN.hideKeyboard()
                        binding.rootView.requestFocus()
                        dismiss()
                        mCallback?.onPINPatternEntered(actionIdentifier)
                    } else {
                        increaseIncorrectSecurityLockAttemptsAndTime()
                        if (incorrectSecurityLockAttempts < Constants.MAX_INCORRECT_SECURITY_LOCK_ATTEMPTS) {
                            // Show the error only when the user has not reached the max attempts limit, because if that
                            // is the case another error is gonna be shown in the setupScreen() method
                            binding.tilPIN.error = getString(R.string.error__wrong_pin)
                        }
                        setupScreen()
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
                        binding.tvTitle.text = getString(R.string.title__pins_dont_match)
                    } else {
                        val salt = CryptoUtils.generateSalt()
                        val hashedPIN = CryptoUtils.createSHA256Hash(salt + pinConfirm)

                        // Stores the newly selected PIN, hashed
                        PreferenceManager.getDefaultSharedPreferences(v.context).edit()
                            .putString(Constants.KEY_HASHED_PIN_PATTERN, hashedPIN)
                            .putString(Constants.KEY_PIN_PATTERN_SALT, salt)
                            .putInt(Constants.KEY_SECURITY_LOCK_SELECTED, 1).apply() // 1 -> PIN

                        dismiss()
                        mCallback?.onPINPatternChanged()
                    }
                }

                handled = true
            }
            handled
        }

        mDisposables.add(
            binding.tietPIN.textChanges()
                .skipInitialValue()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (currentStep == STEP_SECURITY_LOCK_VERIFY &&
                        incorrectSecurityLockAttempts < Constants.MAX_INCORRECT_SECURITY_LOCK_ATTEMPTS
                    ) {
                        // Make sure the error is removed when the user types again
                        binding.tilPIN.isErrorEnabled = false
                    } else if (currentStep == STEP_SECURITY_LOCK_CREATE) {
                        // Show the min length requirement for the PIN only when it has not been fulfilled
                        if (it.trim().length >= Constants.MIN_PIN_LENGTH) {
                            binding.tilPIN.helperText = ""
                        } else {
                            binding.tilPIN.helperText = getString(R.string.msg__min_pin_length)
                        }
                    }
                }
        )
    }

    private fun setupScreen() {
        when (currentStep) {
            STEP_SECURITY_LOCK_VERIFY -> {
                binding.tvTitle.text = getString(R.string.title__re_enter_your_pin)
                binding.tvSubTitle.text = getString(R.string.msg__enter_your_pin)
                binding.tietPIN.isEnabled = true
                if (incorrectSecurityLockAttempts >= Constants.MAX_INCORRECT_SECURITY_LOCK_ATTEMPTS) {
                    // User has entered the PIN incorrectly too many times
                    val now = System.currentTimeMillis()
                    if (now <= incorrectSecurityLockTime + Constants.INCORRECT_SECURITY_LOCK_COOLDOWN) {
                        binding.tietPIN.setText("")
                        binding.tietPIN.isEnabled = false
                        startContDownTimer()
                    } else {
                        resetIncorrectSecurityLockAttemptsAndTime()
                    }
                }
            }
            STEP_SECURITY_LOCK_CREATE -> {
                binding.tvTitle.text = getString(R.string.title__set_bitsy_security_lock)
                binding.tvSubTitle.text = getString(R.string.msg__set_a_pin)
                binding.tilPIN.helperText = getString(R.string.msg__min_pin_length)
                binding.tilPIN.isErrorEnabled = false
            }
            STEP_SECURITY_LOCK_CONFIRM -> {
                binding.tvTitle.text = getString(R.string.title__re_enter_your_pin)
                binding.tvSubTitle.text = ""
                binding.tvSubTitle.visibility = View.GONE
                binding.tietPIN.setText("")
                binding.tilPIN.helperText = ""
                binding.tilPIN.isErrorEnabled = false
            }
        }
    }

    override fun onTimerSecondPassed(errorMessage: String) {
        binding.tilPIN.error = errorMessage
    }

    override fun onTimerFinished() {
        setupScreen()
        binding.tilPIN.isErrorEnabled = false
    }
}