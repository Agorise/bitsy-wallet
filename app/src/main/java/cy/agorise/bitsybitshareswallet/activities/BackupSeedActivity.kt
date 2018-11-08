package cy.agorise.bitsybitshareswallet.activities

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import cy.agorise.bitsybitshareswallet.R

class BackupSeedActivity: AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        setContentView(R.layout.backup_seed)
    }
}