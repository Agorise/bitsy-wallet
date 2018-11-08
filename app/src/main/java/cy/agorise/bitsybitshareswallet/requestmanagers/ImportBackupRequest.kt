package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context

class ImportBackupRequest(context: Context, password: String, val filePath: String) :
    FileServiceRequest(context, password) {
    private var status: StatusCode? = null

    enum class StatusCode {
        NOT_STARTED,
        SUCCEEDED,
        FAILED
    }

    init {
        this.status = StatusCode.NOT_STARTED
    }

    fun setStatus(statusCode: StatusCode) {
        this.status = statusCode
        this.validate()
    }

    fun getStatus(): StatusCode? {
        return this.status
    }

    fun validate() {
        if (this.status != StatusCode.NOT_STARTED) {
            this._fireOnCarryOutEvent()
        }
    }
}
