package cy.agorise.bitsybitshareswallet.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.dialogs.*
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequests
import cy.agorise.bitsybitshareswallet.requestmanagers.ValidateCreateBitsharesAccountRequest
import kotlinx.android.synthetic.main.activity_account.*
import android.widget.Toast
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequestListener


class AccountActivity: AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account)

        tvImportAccount.setOnClickListener(){

            var intent:Intent = Intent(baseContext,BrainkeyActivity::class.java)
            startActivity(intent)
        }

        btnCreate.setOnClickListener(){

            /*
        * Question if continue or not
        * */
            var questionDialog: QuestionDialog = QuestionDialog(baseContext as Activity)
            questionDialog.setText(R.string.continue_question)
            questionDialog.setOnNegative(object : NegativeResponse {
                override fun onNegative(dialogMaterial: DialogMaterial) {
                    dialogMaterial.dismiss()
                }
            })
            questionDialog.setOnPositive(object : PositiveResponse {
                override fun onPositive() {

                    // Make request to create a bitshare account
                    var accountName: String = etAccountName?.getText().toString().trim()
                    val request = ValidateCreateBitsharesAccountRequest(accountName, applicationContext)

                    //DTVV: Friday 27 July 2018
                    //Makes dialog to tell the user that the account is been created
                    val creatingAccountMaterialDialog = CrystalDialog(baseContext as Activity)
                    creatingAccountMaterialDialog.setText(R.string.window_create_seed_DialogMessage)
                    creatingAccountMaterialDialog.progress()
                    this@AccountActivity.runOnUiThread {
                        creatingAccountMaterialDialog.show()
                    }
                    request.listener = (object : CryptoNetInfoRequestListener {
                        override fun onCarryOut() {
                            creatingAccountMaterialDialog.dismiss()
                            if (request.status == ValidateCreateBitsharesAccountRequest.StatusCode.SUCCEEDED) {
                                val accountSeed = request.account
                                val intent = Intent(applicationContext, BackupSeedActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra("SEED_ID", accountSeed!!.id)
                                intent.putExtra("newAccount", true)
                                startActivity(intent)
                            } else if (request.status == ValidateCreateBitsharesAccountRequest.StatusCode.ACCOUNT_EXIST) {
                                Toast.makeText(
                                    baseContext,
                                    (baseContext as Activity).getString(R.string.Account_already_exists),
                                    Toast.LENGTH_LONG
                                ).show()
                                disableCreate()
                            } else {
                                //fieldsValidator.validate()
                            }
                        }
                    })
                    (object : Thread() {
                        override fun run() {

                            /*
                            *
                            * Run thread*/
                            CryptoNetInfoRequests.getInstance()!!.addRequest(request)
                        }
                    }).start()
                }
            })
        }
    }

    /*
    * Enable create button
    * */
    private fun enableCreate() {
        runOnUiThread(Runnable {
            //btnCreate?.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            btnCreate?.setEnabled(true)
        })
    }

    /*
     * Disable create button
     * */
    private fun disableCreate() {
        runOnUiThread(Runnable {
            btnCreate?.setEnabled(false)
            //btnCreate?.setBackground(resources.getDrawable(R.drawable.disable_style))
        })
    }
}