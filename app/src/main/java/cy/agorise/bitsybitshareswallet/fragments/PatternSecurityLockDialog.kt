package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.databinding.DialogPatternSecurityLockBinding
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.CryptoUtils


/**
 * Contains all the specific logic to create and confirm a new Pattern or verifying the validity of the current one.
 */
class PatternSecurityLockDialog : BaseSecurityLockDialog() {

    companion object {
        const val TAG = "PatternSecurityLockDialog"
    }

    private var _binding: DialogPatternSecurityLockBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DialogPatternSecurityLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var newPattern = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        setupScreen()

        binding.patternLockView.addPatternLockListener(mPatternLockViewListener)

        binding.btnClear.setOnClickListener { setupScreen() }
    }

    private val mPatternLockViewListener = object : PatternLockViewListener {
        override fun onStarted() {
            // Make sure the button is hidden when the user starts a new pattern when it was incorrect
            when (currentStep) {
                STEP_SECURITY_LOCK_VERIFY -> {
                    setMessage("")
                }
                STEP_SECURITY_LOCK_CREATE -> {
                    binding.btnClear.visibility = View.INVISIBLE
                    setMessage(getString(R.string.msg__release_finger))
                }
                STEP_SECURITY_LOCK_CONFIRM -> {
                    binding.btnClear.visibility = View.INVISIBLE
                    setMessage(getString(R.string.msg__release_finger))
                }
            }
        }

        override fun onProgress(progressPattern: List<PatternLockView.Dot>) {

        }

        override fun onComplete(pattern: List<PatternLockView.Dot>) {
            if (currentStep == STEP_SECURITY_LOCK_VERIFY) {
                val hashedPattern = CryptoUtils.createSHA256Hash(
                    currentPINPatternSalt +
                            getStringPattern(pattern)
                )
                if (hashedPattern == currentHashedPINPattern) {
                    // Pattern is correct, proceed
                    resetIncorrectSecurityLockAttemptsAndTime()
                    dismiss()
                    mCallback?.onPINPatternEntered(actionIdentifier)
                } else {
                    increaseIncorrectSecurityLockAttemptsAndTime()
                    if (incorrectSecurityLockAttempts < Constants.MAX_INCORRECT_SECURITY_LOCK_ATTEMPTS) {
                        // Show the error only when the user has not reached the max attempts limit, because if that
                        // is the case another error is gonna be shown in the setupScreen() method
                        setError(getString(R.string.error__wront_pattern))
                    }
                    setupScreen()
                }
            } else if (currentStep == STEP_SECURITY_LOCK_CREATE) {
                binding.btnClear.visibility = View.VISIBLE
                if (pattern.size < 4) {
                    setError(getString(R.string.error__connect_at_least_4_dots))
                    binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)
                } else {
                    setMessage(getString(R.string.text__pattern_recorded))
                    binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT)
                    binding.patternLockView.isInputEnabled = false
                    binding.btnNext.isEnabled = true
                    newPattern = getStringPattern(pattern)
                    binding.btnNext.setOnClickListener {
                        currentStep = STEP_SECURITY_LOCK_CONFIRM
                        setupScreen()
                    }
                }
            } else if (currentStep == STEP_SECURITY_LOCK_CONFIRM) {
                val patternConfirm = getStringPattern(pattern)
                if (patternConfirm != newPattern) {
                    setError(getString(R.string.error__wront_pattern))
                    binding.btnNext.isEnabled = false
                    binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)
                } else {
                    setMessage(getString(R.string.msg__your_new_unlock_pattern))
                    binding.patternLockView.isEnabled = false
                    binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT)
                    binding.btnNext.isEnabled = true
                    binding.btnNext.setOnClickListener {
                        context?.let {
                            val salt = CryptoUtils.generateSalt()
                            val hashedPattern = CryptoUtils.createSHA256Hash(salt + patternConfirm)

                            // Stores the newly selected Pattern, encrypted
                            PreferenceManager.getDefaultSharedPreferences(it).edit {
                                putString(Constants.KEY_HASHED_PIN_PATTERN, hashedPattern)
                                putString(Constants.KEY_PIN_PATTERN_SALT, salt)
                                putInt(Constants.KEY_SECURITY_LOCK_SELECTED, 2) // 2 -> Pattern
                            }

                            dismiss()
                            mCallback?.onPINPatternChanged()
                        }
                    }
                }
            }
        }

        override fun onCleared() {

        }
    }

    /**
     * Converts the given pattern into a string representation of it.
     */
    private fun getStringPattern(pattern: List<PatternLockView.Dot>): String {
        val sb = StringBuilder()
        for (dot in pattern)
            sb.append(dot.id)

        return sb.toString()
    }

    private fun setupScreen() {
        when (currentStep) {
            STEP_SECURITY_LOCK_VERIFY -> {
                binding.tvTitle.text = getString(R.string.title__re_enter_your_pattern)
                binding.tvSubTitle.text = getString(R.string.msg__enter_your_pattern)
                binding.btnClear.visibility = View.GONE
                binding.btnNext.visibility = View.GONE
                binding.patternLockView.isInputEnabled = true
                binding.patternLockView.isInStealthMode = true
                if (incorrectSecurityLockAttempts >= Constants.MAX_INCORRECT_SECURITY_LOCK_ATTEMPTS) {
                    // User has entered the Pattern incorrectly too many times
                    val now = System.currentTimeMillis()
                    if (now <= incorrectSecurityLockTime + Constants.INCORRECT_SECURITY_LOCK_COOLDOWN) {
                        binding.patternLockView.isInputEnabled = false
                        startContDownTimer()
                    } else {
                        resetIncorrectSecurityLockAttemptsAndTime()
                    }
                }
            }
            STEP_SECURITY_LOCK_CREATE -> {
                binding.tvTitle.text = getString(R.string.title__set_bitsy_security_lock)
                binding.tvSubTitle.text = getString(R.string.msg__set_a_pattern)
                setMessage(getString(R.string.text__draw_an_unlock_pattern))
                binding.patternLockView.clearPattern()
                binding.patternLockView.isInputEnabled = true
                binding.btnClear.visibility = View.INVISIBLE
                binding.btnNext.isEnabled = false
            }
            STEP_SECURITY_LOCK_CONFIRM -> {
                binding.tvTitle.text = getString(R.string.title__re_enter_your_pattern)
                binding.tvSubTitle.text = ""
                setMessage(getString(R.string.msg__draw_pattern_confirm))
                binding.tvSubTitle.visibility = View.GONE
                binding.patternLockView.clearPattern()
                binding.patternLockView.isInputEnabled = true
                binding.btnClear.visibility = View.INVISIBLE
                binding.btnNext.isEnabled = false
                binding.btnNext.text = getString(R.string.button__confirm)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setMessage(message: String) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            binding.tvMessage.setTextAppearance(context, R.style.TextAppearance_Bitsy_Body2)
        } else {
            binding.tvMessage.setTextAppearance(R.style.TextAppearance_Bitsy_Body2)
        }
        binding.tvMessage.text = message
    }

    @Suppress("DEPRECATION")
    private fun setError(error: String) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            binding.tvMessage.setTextAppearance(context, R.style.TextAppearance_Bitsy_Body2_Error)
        } else {
            binding.tvMessage.setTextAppearance(R.style.TextAppearance_Bitsy_Body2_Error)
        }
        binding.tvMessage.text = error
    }

    override fun onTimerSecondPassed(errorMessage: String) {
        setError(errorMessage)
    }

    override fun onTimerFinished() {
        setupScreen()
        setMessage("")
    }
}