package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context

abstract class FileServiceRequest protected constructor(context: Context, password: String) {

    var context: Context
        protected set
    //protected Activity activity;
    var password: String
        protected set

    protected lateinit var listener: FileServiceRequestListener

    init {
        this.context = context
        //this.activity = activity;
        this.password = password
    }

    protected fun _fireOnCarryOutEvent() {
        listener.onCarryOut()
    }
}
