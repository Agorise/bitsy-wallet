package cy.agorise.bitsybitshareswallet.activities

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cy.agorise.bitsybitshareswallet.utils.FieldsValidator
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.*
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.preference.PreferenceManager
import android.widget.Toast
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants


open class CustomActivity : AppCompatActivity() {

    /*
    * Contains the validator for general fields
    * */
    @JvmField protected var fieldsValidator = FieldsValidator()

    /*
    * Contains the global activity
    * */
    protected lateinit var globalActivity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
        * Save the current activity for further reference
        * */
        this.globalActivity = this

        val intentFilter = IntentFilter()
        intentFilter.addAction("com.package.ACTION_CLOSE")
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                finish()
                unregisterReceiver(receiver);
                System.exit(0)
            }
        }

        try{
            unregisterReceiver(receiver)
        }catch(e: Exception){

        }
        registerReceiver(receiver, intentFilter)
    }


    override fun onPause() {
        super.onPause()

        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.KEY_MINUTES_CLOSE_MODE_ACTIVATED, false)
        ) {

            val broadcastIntent = Intent()
            broadcastIntent.action = "com.package.ACTION_CLOSE"
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext, 0, broadcastIntent,
                0
            )
            val time = Calendar.getInstance()
            time.setTimeInMillis(System.currentTimeMillis())
            time.add(Calendar.SECOND, 180)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC, time.timeInMillis, pendingIntent)
        }
    }


    override fun onResume() {
        super.onResume()


        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.KEY_MINUTES_CLOSE_MODE_ACTIVATED, false)
        ) {

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val broadcastIntent = Intent()
            broadcastIntent.action = "com.package.ACTION_CLOSE"
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext, 0, broadcastIntent, 0
            )
            alarmManager.cancel(pendingIntent)
        }
    }


    companion object {

        var receiver:BroadcastReceiver? = null
    }

}