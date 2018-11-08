package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context

class CreateBackupRequest(context: Context, password: String) : FileServiceRequest(context, password) {

    private var filePath: String? = null
    private var status: StatusCode? = null

    enum class StatusCode {
        NOT_STARTED,
        SUCCEEDED,
        FAILED
    }

    init {
        this.filePath = ""
        this.status = StatusCode.NOT_STARTED
    }

    fun setFilePath(filePath: String) {
        this.filePath = filePath
        this.validate()
    }

    fun setStatus(statusCode: StatusCode) {
        this.status = statusCode
        this.validate()
    }

    fun getStatus(): StatusCode? {
        return this.status
    }

    fun getFilePath(): String? {
        return this.filePath
    }

    fun validate() {
        if (this.status != StatusCode.NOT_STARTED) {
            this._fireOnCarryOutEvent()
        }
    }


}
