package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants
import io.reactivex.disposables.CompositeDisposable
import kotlin.math.roundToInt

/**
 * Encapsulates the shared logic required for the PIN and Pattern Security Lock Fragments.
 */
abstract class BaseSecurityLockDialog : DialogFragment() {

    companion object {
        /** Used to denote that the user is in the step of creating the preferred security lock option */
        const val STEP_SECURITY_LOCK_CREATE = 1

        /** Used to denote that the user is in the step of confirming the newly created security lock option,
         * this option should only be used internally */
        const val STEP_SECURITY_LOCK_CONFIRM = 2

        /** Used to denote that the user is in the step of verifying the current security lock option, to give
         * permission to do a security constrained action like sending a transaction or trying to change the
         * current security lock option */
        const val STEP_SECURITY_LOCK_VERIFY = 3

        /** Used to let the dialog know if the user wants to create a new PIN/Pattern or just verify its correctness
         * to get access to security constrained actions */
        const val KEY_STEP_SECURITY_LOCK = "key_step_security_lock"

        /** The calling fragment can be calling this dialog to unlock many different things, this variable helps to
         * keep track of what action the calling fragment wants to achieve */
        const val KEY_ACTION_IDENTIFIER = "key_action_identifier"
    }

    // Container Fragment must implement this interface
    interface OnPINPatternEnteredListener {
        fun onPINPatternEntered(actionIdentifier: Int)
        fun onPINPatternChanged()
    }

    /** Callback used to notify the parent that a PIN/Pattern has been entered successfully */
    protected var mCallback: OnPINPatternEnteredListener? = null

    /** Keeps track of the current step, can be create, confirm of verify */
    protected var currentStep: Int = -1

    /** Used so the calling object knows what was the intention to ask for the Security Lock */
    protected var actionIdentifier: Int = -1

    /** Keeps track of all RxJava disposables, to make sure they are all disposed when the fragment is destroyed */
    protected var mDisposables = CompositeDisposable()

    /** Current hashed version of the salt + PIN/Pattern */
    protected var currentHashedPINPattern: String? = null

    /** Salt used to hash the current PIN/Pattern */
    protected var currentPINPatternSalt: String? = null

    /** Current count of incorrect attempts to verify the current security lock */
    protected var incorrectSecurityLockAttempts = 0

    /** Time of the last lock/disable due to too many incorrect attempts to verify the security lock */
    protected var incorrectSecurityLockTime = 0L

    /** Timer used to update the error message when the user has tried too many incorrect attempts tu enter the
     * current security lock option */
    private var mCountDownTimer: CountDownTimer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onAttachToParentFragment(parentFragment)

        currentHashedPINPattern = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_HASHED_PIN_PATTERN, "")

        currentPINPatternSalt = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_PIN_PATTERN_SALT, "")

        incorrectSecurityLockAttempts = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Constants.KEY_INCORRECT_SECURITY_LOCK_ATTEMPTS, 0)

        incorrectSecurityLockTime = PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(Constants.KEY_INCORRECT_SECURITY_LOCK_TIME, 0L)

        currentStep = arguments?.getInt(KEY_STEP_SECURITY_LOCK) ?: -1

        actionIdentifier = arguments?.getInt(KEY_ACTION_IDENTIFIER) ?: -1
    }

    /**
     * Attaches the current [DialogFragment] to its [Fragment] parent, to initialize the
     * [OnPINPatternEnteredListener] interface
     */
    private fun onAttachToParentFragment(fragment: Fragment?) {
        try {
            mCallback = fragment as OnPINPatternEnteredListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$fragment must implement OnFilterOptionsSelectedListener")
        }
    }

    /**
     * Increases the incorrectSecurityLockAttempts counter by one and saves that value into the shared preferences
     * to account for cases when the user could try to trick the app by just closing and reopening the dialog. Also,
     * stores the current time, so that when the number of attempts is bigger than the maximum allowed, the security
     * lock gets locked for a certain amount of time.
     */
    protected fun increaseIncorrectSecurityLockAttemptsAndTime() {
        val now = System.currentTimeMillis()

        incorrectSecurityLockTime = now

        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putInt(Constants.KEY_INCORRECT_SECURITY_LOCK_ATTEMPTS, ++incorrectSecurityLockAttempts)
            putLong(Constants.KEY_INCORRECT_SECURITY_LOCK_TIME, now)
        }
    }

    /**
     * Resets the values of the incorrectSecurityLockAttempts and Time, both in the local variable as well as the one
     * stored in the shared preferences.
     */
    protected fun resetIncorrectSecurityLockAttemptsAndTime() {
        incorrectSecurityLockTime = 0
        incorrectSecurityLockAttempts = 0

        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putInt(Constants.KEY_INCORRECT_SECURITY_LOCK_ATTEMPTS, incorrectSecurityLockAttempts)
            putLong(Constants.KEY_INCORRECT_SECURITY_LOCK_TIME, incorrectSecurityLockTime)
        }
    }

    protected fun startContDownTimer() {
        var millis = incorrectSecurityLockTime + Constants.INCORRECT_SECURITY_LOCK_COOLDOWN - System.currentTimeMillis()
        millis = millis / 1000 * 1000 + 1000    // Make sure millis account for a whole second multiple

        mCountDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsUntilFinished = millisUntilFinished / 1000
                if (secondsUntilFinished < 60) {
                    // Less than a minute remains
                    val errorMessage = getString(R.string.error__security_lock_too_many_attempts_seconds,
                        secondsUntilFinished.toInt())
                    onTimerSecondPassed(errorMessage)
                } else {
                    // At least a minute remains
                    val minutesUntilFinished = (secondsUntilFinished / 60.0).roundToInt()
                    val errorMessage = getString(R.string.error__security_lock_too_many_attempts_minutes,
                        minutesUntilFinished)
                    onTimerSecondPassed(errorMessage)
                }
            }

            override fun onFinish() {
                onTimerFinished()
            }
        }.start()
    }

    abstract fun onTimerSecondPassed(errorMessage: String)

    abstract fun onTimerFinished()

    override fun onResume() {
        super.onResume()

        // Force dialog fragment to use the full width of the screen
        val dialogWindow = dialog?.window
        dialogWindow?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!mDisposables.isDisposed) mDisposables.dispose()

        mCountDownTimer?.cancel()
        mCountDownTimer = null
    }
}