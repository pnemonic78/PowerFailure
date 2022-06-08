package net.sf.power.monitor.preference

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.AttributeSet
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import net.sf.power.monitor.R

class RecipientPreference(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    Preference(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.preferenceStyle
    )

    constructor(context: Context) : this(context, null)

    private var recipientValue: String? = null
    var recipient: String
        get() {
            if (recipientValue == null) {
                recipientValue = sharedPreferences?.getString(key, "") ?: ""
            }
            return recipientValue!!
        }
        private set(value) {
            recipientValue = value
            if (persistString(value)) {
                notifyChanged()
            }
        }

    private var host: Fragment? = null
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private var requestPhoneLauncher: ActivityResultLauncher<Void?>? = null

    override fun setDefaultValue(defaultValue: Any?) {
        super.setDefaultValue(defaultValue)
        recipientValue = defaultValue?.toString()
    }

    override fun onClick() {
        if (checkPermissions()) {
            pickRecipient()
        }
    }

    private fun pickRecipient() {
        requestPhoneLauncher?.launch(null)
    }

    private fun chooseBestRecipient(contactUri: Uri) {
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

    fun setHost(host: Fragment) {
        this.host = host
        this.requestPermissionLauncher =
            host.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    pickRecipient()
                } else if (host.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                    // TODO explain that we need this permission to pick a contact with his phone number.
                }
            }
        this.requestPhoneLauncher = host.registerForActivityResult(PickPhone()) { uri ->
            if (uri != null) {
                chooseBestRecipient(uri)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val context: Context = this.context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionChecker.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) != PermissionChecker.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher?.launch(Manifest.permission.READ_CONTACTS)
                return false
            }
        }
        return true
    }

    class PickPhone : ActivityResultContract<Void?, Uri?>() {
        override fun createIntent(context: Context, input: Void?): Intent {
            return getPickerIntent()
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
        }
    }

    companion object {
        private val PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        )
        private const val SELECTION = ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER + "=1"

        private const val COLUMN_NUMBER = 0
        private const val COLUMN_NORMALIZED_NUMBER = 1
        private const val COLUMN_TYPE = 2

        private fun getPickerIntent(): Intent {
            return Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        }
    }
}