package net.sf.power.monitor.preference

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.AttributeSet
import androidx.preference.Preference
import net.sf.power.monitor.R

class RecipientPreference(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : Preference(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.preferenceStyle)
    constructor(context: Context) : this(context, null)

    private var recipientValue: String? = null
    var recipient: String
        get() {
            if (recipientValue == null) {
                recipientValue = sharedPreferences.getString(key, "") ?: ""
            }
            return recipientValue!!
        }
        private set(value) {
            recipientValue = value
            persistString(value)
        }

    private var host: Fragment? = null
    private var requestCode = 0

    init {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        val pm = context.packageManager
        val info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        isEnabled = (info != null)
    }

    override fun setDefaultValue(defaultValue: Any?) {
        super.setDefaultValue(defaultValue)
        recipientValue = defaultValue?.toString()
    }

    override fun onClick() {
        pickRecipient()
    }

    private fun pickRecipient() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        host?.startActivityForResult(intent, requestCode)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == this.requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    chooseBestRecipient(data)
                }
            }
        }
    }

    private fun chooseBestRecipient(data: Intent) {
        val contactUri = data.data ?: return
        val cursor = context.contentResolver.query(contactUri, PROJECTION, SELECTION, null, null)
            ?: return

        var number: String?
        var numberNormalized: String?
        var numberType: Int
        var bestNumber = ""

        if (cursor.moveToFirst()) {
            do {
                number = cursor.getString(COLUMN_NUMBER)
                numberNormalized = cursor.getString(COLUMN_NORMALIZED_NUMBER) ?: number ?: continue
                numberType = cursor.getInt(COLUMN_TYPE)

                if (numberType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                    bestNumber = numberNormalized
                    break
                } else if (numberType == ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE) {
                    bestNumber = numberNormalized
                    break
                } else if (bestNumber.isEmpty()) {
                    bestNumber = numberNormalized
                }
            } while (cursor.moveToNext())

            if (callChangeListener(bestNumber)) {
                recipient = bestNumber
            }
        }
        cursor.close()
    }

    fun setOnClick(host: Fragment, requestCode: Int) {
        this.host = host
        this.requestCode = requestCode
    }

    companion object {
        private val PROJECTION = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE)
        private const val SELECTION = ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER + "=1"

        private const val COLUMN_NUMBER = 0
        private const val COLUMN_NORMALIZED_NUMBER = 1
        private const val COLUMN_TYPE = 2
    }
}