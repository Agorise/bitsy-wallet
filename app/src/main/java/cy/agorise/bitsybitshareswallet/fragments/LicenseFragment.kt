package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.databinding.FragmentLicenseBinding
import cy.agorise.bitsybitshareswallet.utils.Constants

class LicenseFragment : Fragment() {

    companion object {
        private const val TAG = "LicenseFragment"
    }

    private var _binding: FragmentLicenseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Remove up navigation icon from the toolbar
        val toolbar: Toolbar? = activity?.findViewById(R.id.toolbar)
        toolbar?.navigationIcon = null

        _binding = FragmentLicenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        // Get version number of the last agreed license version
        val agreedLicenseVersion = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Constants.KEY_LAST_AGREED_LICENSE_VERSION, 0)

        // If the last agreed license version is the actual one then proceed to the following Activities
        if (agreedLicenseVersion == Constants.CURRENT_LICENSE_VERSION) {
            agree()
        } else {
            binding.wbLA.loadUrl("file:///android_asset/eula.html")

            binding.btnDisagree.setOnClickListener { activity?.finish() }

            binding.btnAgree.setOnClickListener { agree() }
        }
    }

    /**
     * This function stores the version of the current accepted license version into the Shared Preferences and
     * sends the user to import/create account.
     */
    private fun agree() {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putInt(Constants.KEY_LAST_AGREED_LICENSE_VERSION, Constants.CURRENT_LICENSE_VERSION)
        }

        findNavController().navigate(R.id.import_brainkey_action)
    }
}
