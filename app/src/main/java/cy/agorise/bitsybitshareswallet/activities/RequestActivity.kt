package cy.agorise.bitsybitshareswallet.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cy.agorise.bitsybitshareswallet.R
import de.bitshares_munich.smartcoinswallet.ReceiveTransactionActivity
import kotlinx.android.synthetic.main.request_screen.*
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.*

class RequestActivity : CustomActivity() , View.OnClickListener {

    internal lateinit var locale: Locale
    internal lateinit var format: NumberFormat
    internal lateinit var language: String

    internal var to = ""
    internal var account_id = ""




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.request_screen)

        forwardDisabling()
        setBackButton(true)
        title = resources.getString(R.string.request_amount_screen_name)
        language = fetchStringSharePref(applicationContext, getString(R.string.pref_language))!!

        locale = resources.configuration.locale
        format = NumberFormat.getInstance(locale)
        fieldsReference()

        val intent = intent
        if (intent.hasExtra(getString(R.string.to))) {
            to = intent.getStringExtra(getString(R.string.to))
        }
        if (intent.hasExtra(getString(R.string.account_id))) {
            account_id = intent.getStringExtra(getString(R.string.account_id))
        }

        tvCancel.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {

                val intent = Intent(applicationContext, ReceiveTransactionActivity::class.java)
                intent.putExtra(getString(R.string.currency), popwin1.getText().toString())
                intent.putExtra(getString(R.string.to), to)
                intent.putExtra(getString(R.string.account_id), account_id)
                intent.putExtra("price", "0")
                startActivity(intent)
                finish()
            }
        })

        tvNext.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {

                try {
                    val number = format.parse(removeSpecialCharacters())
                    if (number.toDouble() > 0) {
                        val amount = number.toString()
                        val intent = Intent(applicationContext, ReceiveTransactionActivity::class.java)
                        intent.putExtra(getString(R.string.currency), popwin1.getText().toString())
                        intent.putExtra(getString(R.string.to), to)
                        intent.putExtra(getString(R.string.price), amount)
                        intent.putExtra(getString(R.string.account_id), account_id)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(applicationContext, R.string.please_enter_valid_amount, Toast.LENGTH_SHORT)
                            .show()
                    }
                } catch (e: ParseException) {
                    e.printStackTrace()
                }

            }
        })
    }

    fun fetchStringSharePref(context: Context, key: String, defaultValue: String = ""): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(key, defaultValue)
    }


    fun backbtn(v: View) {
        var str = txtScreen.text.toString()
        if (str.length > 0) {
            str = str.substring(0, str.length - 1)
            txtScreen.text = str
            try {

                if (!txtScreen.text.toString().isEmpty()) {
                    val number = format.parse(removeSpecialCharacters())
                    if (number.toDouble() > 0) {
                        forwardEnabling()
                    } else {
                        forwardDisabling()
                    }
                    txtScreen.setText(setLocaleNumberFormat(locale, number))
                } else {
                    forwardDisabling()
                }
            } catch (e: ParseException) {

                e.printStackTrace()

            }

        } else {
            forwardDisabling()
        }
    }


    private fun fieldsReference() {
        btnOne.setOnClickListener(this)
        btnTwo.setOnClickListener(this)
        btnThree.setOnClickListener(this)
        btnFour.setOnClickListener(this)
        btnFive.setOnClickListener(this)
        btnSix.setOnClickListener(this)
        btnSeven.setOnClickListener(this)
        btnEight.setOnClickListener(this)
        btnNine.setOnClickListener(this)
        btnZero.setOnClickListener(this)
        btnDoubleZero.setOnClickListener(this)
        btnDot.setOnClickListener(this)
        keypadNumbersLocalization()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home ->{
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    override fun onClick(p0: View?) {

        val button = p0 as Button
        val currentkey = button.text.toString()
        val dot = btnDot.text.toString()
        if (currentkey == dot) {
            txtScreen.append(currentkey)
        } else if (currentkey == btnZero.text.toString()) {
            txtScreen.append(currentkey)
        } else if (currentkey == btnDoubleZero.text.toString()) {
            txtScreen.append(currentkey)
        } else {

            txtScreen.append(currentkey)
            try {
                val number = format.parse(removeSpecialCharacters())
                if (number.toDouble() > 0) {
                    forwardEnabling()
                } else {
                    forwardDisabling()
                }
                txtScreen.text = setLocaleNumberFormat(locale,number)
            } catch (e: ParseException) {
                e.printStackTrace()

            }

        }
    }


    fun setLocaleNumberFormat(locale: Locale, number: Number): String {

        val formatter = NumberFormat.getInstance(locale)
        formatter.maximumFractionDigits = 4
        var result:String = formatter.format(number)
        return result

    }

    private fun forwardEnabling() {
        tvNext.isEnabled = true
        llNext.setBackgroundColor(Color.rgb(112, 136, 46))
    }


    private fun removeSpecialCharacters(): String {
        //Farsi and arabic
        var inputNumber = txtScreen.text.toString()
        val dot = btnDot.text.toString()
        inputNumber = inputNumber.replace("٬", "")
        inputNumber = inputNumber.replace(160.toChar().toString(), "")
        if (dot == ",") {
            inputNumber = inputNumber.replace(".", "")
        } else if (dot == ".") {
            inputNumber = inputNumber.replace(",", "")
        }
        return inputNumber
    }


    private fun keypadNumbersLocalization() {
        btnOne.text = setLocaleNumberFormat(locale, 1)
        btnTwo.text = setLocaleNumberFormat(locale, 2)
        btnThree.text = setLocaleNumberFormat(locale, 3)
        btnFour.text = setLocaleNumberFormat(locale, 4)
        btnFive.text = setLocaleNumberFormat(locale, 5)
        btnSix.text = setLocaleNumberFormat(locale, 6)
        btnSeven.text = setLocaleNumberFormat(locale, 7)
        btnEight.text = setLocaleNumberFormat(locale, 8)
        btnNine.text = setLocaleNumberFormat(locale, 9)
        btnZero.text = setLocaleNumberFormat(locale, 0)
        //btnDot.setText(String.valueOf(setDecimalSeparator(locale)))
        btnDot.setText(setDecimalSeparator(locale).toString())
        btnDoubleZero.text = setLocaleNumberFormat(locale, 0) + "" + setLocaleNumberFormat(locale, 0)
    }

    fun setDecimalSeparator(locale: Locale): Char {

        val decimalFormatSymbols = DecimalFormatSymbols(locale)
        val decimal:Char = decimalFormatSymbols.decimalSeparator
        return decimal

    }

    private fun forwardDisabling() {
        tvNext.isEnabled = false
        llNext.setBackgroundColor(Color.rgb(211, 211, 211))
    }

    fun setBackButton(isBackButton: Boolean?) {
        if (isBackButton!!) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }
}
