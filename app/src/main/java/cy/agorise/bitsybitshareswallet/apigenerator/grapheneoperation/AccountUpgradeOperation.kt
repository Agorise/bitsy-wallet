package cy.agorise.bitsybitshareswallet.apigenerator.grapheneoperation

import com.google.common.primitives.Bytes
import com.google.gson.*
import cy.agorise.graphenej.AssetAmount
import cy.agorise.graphenej.BaseOperation
import cy.agorise.graphenej.OperationType
import cy.agorise.graphenej.UserAccount
import java.lang.reflect.Type

class AccountUpgradeOperation : BaseOperation {

    private var fee: AssetAmount? = null
    var accountToUpgrade: UserAccount? = null
    var isUpgradeToLifeTimeMember: Boolean = false

    constructor(
        accountToUpgrade: UserAccount,
        upgradeToLifeTimeMember: Boolean
    ) : super(OperationType.ACCOUNT_UPGRADE_OPERATION) {
        this.accountToUpgrade = accountToUpgrade
        this.isUpgradeToLifeTimeMember = upgradeToLifeTimeMember
    }

    constructor(
        accountToUpgrade: UserAccount,
        upgradeToLifeTimeMember: Boolean,
        fee: AssetAmount
    ) : super(OperationType.ACCOUNT_UPGRADE_OPERATION) {
        this.accountToUpgrade = accountToUpgrade
        this.isUpgradeToLifeTimeMember = upgradeToLifeTimeMember
        this.fee = fee
    }

    fun getFee(): AssetAmount? {
        return fee
    }

    override fun setFee(assetAmount: AssetAmount) {
        this.fee = assetAmount
    }

    override fun toBytes(): ByteArray {
        val feeBytes = fee!!.toBytes()
        val accountBytes = accountToUpgrade!!.toBytes()
        val upgradeToLifeTimeMemberBytes = if (this.isUpgradeToLifeTimeMember) byteArrayOf(0x1) else byteArrayOf(0x0)
        val extensions = this.extensions.toBytes()
        return Bytes.concat(feeBytes, accountBytes, upgradeToLifeTimeMemberBytes, extensions)
    }

    override fun toJsonString(): String {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(AccountUpgradeOperation::class.java, AccountUpgradeSerializer())
        return gsonBuilder.create().toJson(this)
    }

    override fun toJsonObject(): JsonElement {
        val array = JsonArray()
        array.add(this.id)
        val jsonObject = JsonObject()
        if (fee != null)
            jsonObject.add(BaseOperation.KEY_FEE, fee!!.toJsonObject())
        jsonObject.addProperty(KEY_ACCOUNT, accountToUpgrade!!.objectId)
        jsonObject.addProperty(KEY_UPGRADE, if (this.isUpgradeToLifeTimeMember) "true" else "false")
        jsonObject.add(BaseOperation.KEY_EXTENSIONS, JsonArray())
        array.add(jsonObject)
        return array
    }

    class AccountUpgradeSerializer : JsonSerializer<AccountUpgradeOperation> {

        override fun serialize(
            accountUpgrade: AccountUpgradeOperation,
            type: Type,
            jsonSerializationContext: JsonSerializationContext
        ): JsonElement {
            return accountUpgrade.toJsonObject()
        }
    }


    class AccountUpgradeDeserializer : JsonDeserializer<AccountUpgradeOperation> {

        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): AccountUpgradeOperation? {
            if (json.isJsonArray) {
                // This block is used just to check if we are in the first step of the deserialization
                // when we are dealing with an array.
                val serializedAccountUpgrade = json.asJsonArray
                return if (serializedAccountUpgrade.get(0).asInt != OperationType.ACCOUNT_UPGRADE_OPERATION.ordinal) {
                    // If the operation type does not correspond to a transfer operation, we return null
                    null
                } else {
                    // Calling itself recursively, this is only done once, so there will be no problems.
                    context.deserialize<AccountUpgradeOperation>(
                        serializedAccountUpgrade.get(1),
                        AccountUpgradeOperation::class.java
                    )
                }
            } else {
                val jsonObject = json.asJsonObject

                // Deserializing AssetAmount objects
                val fee =
                    context.deserialize<AssetAmount>(jsonObject.get(BaseOperation.KEY_FEE), AssetAmount::class.java)

                // Deserializing UserAccount objects
                val accountToUpgrade = UserAccount(jsonObject.get(KEY_ACCOUNT).asString)

                val upgradeToLifeTime = jsonObject.get(KEY_UPGRADE).asBoolean

                return AccountUpgradeOperation(accountToUpgrade, upgradeToLifeTime, fee)
            }
        }
    }

    companion object {

        private val KEY_ACCOUNT = "account_to_upgrade"
        private val KEY_UPGRADE = "upgrade_to_lifetime_member"
    }


}
