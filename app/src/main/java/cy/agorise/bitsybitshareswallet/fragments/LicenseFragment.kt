package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.crashlytics.android.Crashlytics
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants
import kotlinx.android.synthetic.main.fragment_license.*

class LicenseFragment : Fragment() {

    companion object {
        private const val TAG = "LicenseFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Remove up navigation icon from the toolbar
        val toolbar: Toolbar? = activity?.findViewById(R.id.toolbar)
        toolbar?.navigationIcon = null

        return inflater.inflate(R.layout.fragment_license, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Crashlytics.setString(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        // Get version number of the last agreed license version
        val agreedLicenseVersion = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Constants.KEY_LAST_AGREED_LICENSE_VERSION, 0)

        // If the last agreed license version is the actual one then proceed to the following Activities
        if (agreedLicenseVersion == Constants.CURRENT_LICENSE_VERSION) {
            agree()
        } else {
            wbLA.loadUrl("file:///android_asset/eula.html")

            btnDisagree.setOnClickListener { activity?.finish() }

            btnAgree.setOnClickListener { agree() }
        }
    }

    /**
     * This function stores the version of the current accepted license version into the Shared Preferences and
     * sends the user to import/create account.
     */
    private fun agree() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt(Constants.KEY_LAST_AGREED_LICENSE_VERSION, Constants.CURRENT_LICENSE_VERSION).apply()

        findNavController().navigate(R.id.import_brainkey_action)
    }
}