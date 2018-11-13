package cy.agorise.bitsybitshareswallet.repository

import android.app.Activity
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase

open class Repository{


    var activity:Activity? = null


    constructor(activity:Activity?){
        this.activity = activity
        if(db == null){
            db = BitsyDatabase.getAppDatabase(activity!!)!!
        }
    }

    companion object {

        var db: BitsyDatabase? = null
    }
}