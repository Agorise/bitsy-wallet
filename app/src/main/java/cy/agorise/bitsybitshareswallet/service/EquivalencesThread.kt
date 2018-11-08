package cy.agorise.bitsybitshareswallet.service

import android.util.Log
import androidx.lifecycle.LifecycleService
import cy.agorise.bitsybitshareswallet.apigenerator.GrapheneApiGenerator
import cy.agorise.bitsybitshareswallet.models.BitsharesAsset
import java.util.ArrayList

class EquivalencesThread(
    private val service: LifecycleService,
    private val fromAsset: String,
    private val bitsharesAssets: List<BitsharesAsset>
) : Thread() {
    private var keepLoadingEquivalences = true

    override fun run() {
        super.run()

        while (this.keepLoadingEquivalences) {
            val queryAsset = ArrayList<BitsharesAsset>()
            for (asset in bitsharesAssets) {
                if (!asset.name.equals(fromAsset)) {
                    queryAsset.add(asset)
                }
            }
            try {
                //GrapheneApiGenerator.getEquivalentValue(fromAsset, bitsharesAssets, this.service);
                GrapheneApiGenerator.getEquivalentValue(fromAsset, queryAsset, this.service)
                Log.i("Equivalences Thread", "In loop")
                Thread.sleep(300000)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }

        }
    }

    fun stopLoadingEquivalences() {
        this.keepLoadingEquivalences = false
    }
}
