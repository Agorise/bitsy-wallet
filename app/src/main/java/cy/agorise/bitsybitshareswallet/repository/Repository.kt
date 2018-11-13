package cy.agorise.bitsybitshareswallet.repository

import android.app.Activity
import cy.agorise.bitsybitshareswallet.dao.CrystalDatabase

open class Repository{


    var activity:Activity? = null


    constructor(activity:Activity?){
        this.activity = activity
        if(db == null){
            db = CrystalDatabase.getAppDatabase(activity!!)!!
        }
    }

    companion object {

        var db: CrystalDatabase? = null
    }
}