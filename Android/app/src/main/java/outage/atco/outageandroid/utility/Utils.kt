package outage.atco.outageandroid.utility

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.os.SystemClock
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import com.crashlytics.android.Crashlytics
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import outage.atco.outageandroid.BuildConfig
import outage.atco.outageandroid.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/// View Math

val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).roundToInt()


/// Date Formatter

fun Date.formatDisplay(format: String = "h:mm a '|' MMMM d YYYY"): String {
    val sdf = SimpleDateFormat(format, Locale.CANADA)
    // Note: Server returns times already adjusted for timezone, but without timezone info, so set to UTC to prevent offset
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(this)
}

fun Date.formatServer(format: String = "yyyy-MM-dd HH:mm"): String {
    val sdf = SimpleDateFormat(format, Locale.ROOT)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(this)
}

/// EditTexts

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    addTextChangedListener(object: TextWatcher {

        override fun afterTextChanged(s: Editable?) {
            afterTextChanged.invoke(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
    })
}

fun EditText.afterTextChangedWithDebounce(debounceTime: Long = 300L, afterTextChanged: (String) -> Unit) {
    var lastInput = ""
    var debounceJob: Job? = null
    val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    addTextChangedListener(object : TextWatcher {

        override fun afterTextChanged(editable: Editable?) {
            val newInput = editable?.toString() ?: return
            debounceJob?.cancel()

            if (lastInput == newInput) return

            lastInput = newInput
            debounceJob = uiScope.launch {
                delay(debounceTime)
                if (lastInput != newInput) return@launch

                afterTextChanged(newInput)

            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
    })
}

fun Editable?.toStringOrNotProvided(): String {
    if (this.isNullOrBlank()) return "Not Provided"
    return this.toString()
}

fun Editable?.toStringOrUnknown(): String {
    if (this.isNullOrBlank()) return "Unknown"
    return this.toString()
}

// Keyboard helpers
fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
}

fun Context.hideKeyboard(view: View?) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
}

fun Fragment.showKeyboard() {
    view?.let { activity?.showKeyboard(it) }
}

fun Activity.showKeyboard() {
    showKeyboard(if (currentFocus == null) View(this) else currentFocus)
}

fun Context.showKeyboard(view: View?) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.toggleSoftInputFromWindow(view?.windowToken, 0, 0)
}

/// View
fun MaterialCardView.makeCircular() {
    radius = this.measuredHeight / 2.0f
}

fun View.clickWithDebounce(debounceTime: Long = 600L, action: () -> Unit) {
    setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) return
            action()

            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun Button.showSubmitting(text: String? = null, colorRes: Int = R.color.white) {
    isEnabled = false
    this.showProgress {
        progressColorRes = colorRes
        if (text != null) buttonText = text else buttonTextRes = R.string.submitting
    }
}

fun Button.showLoading() {
    isEnabled = false
    this.showProgress {
        progressColorRes = R.color.white
        buttonTextRes = R.string.loading
    }
}

fun Button.hideLoading(textResource: Int) {
    isEnabled = true
    this.hideProgress(textResource)
}

// Validate Password (one small letter, one capitalized, one digit, no spaces, at least 8 characters)
fun EditText.validatePassword(): Boolean {
    val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=\\S+\$).{8,}".toRegex()
    val isPasswordValid = this.text?.matches(passwordRegex) == true
    val error = if (isPasswordValid) null else resources.getString(R.string.settings_password_requirements)
    val parent = this.parent.parent as? TextInputLayout
    if (error != parent?.error) parent?.error = error
    return isPasswordValid
}

fun EditText.validateRequired(): Boolean {
    val error = if (this.text.isNullOrBlank()) resources.getString(R.string.settings_required) else null
    val parent = this.parent.parent as? TextInputLayout
    if (error != parent?.error) parent?.error = error
    return error == null
}

fun EditText.validateLengthRequired(length: Int): Boolean {
    val error = if (this.text.length < length) resources.getString(R.string.settings_minimum_length, length) else null
    val parent = this.parent.parent as? TextInputLayout
    if (error != parent?.error) parent?.error = error
    return error == null
}

fun EditText.validateMatching(matchedField: EditText, fieldNames: String): Boolean {
    val errorString = resources.getString(R.string.settings_error_match, fieldNames)
    val error = if (this.text.toString() != matchedField.text.toString() || this.text.isNullOrBlank()) errorString else null
    val parent = this.parent.parent as? TextInputLayout
    if (error != parent?.error) parent?.error = error
    return error == null
}

/// Serialization Helpers

val gson = Gson()

//convert a data class to a map
fun <T> T.serializeToMap(): Map<String, Any> {
    return convert()
}

//convert a map to a data class
inline fun <reified T> Map<String, Any>.toDataClass(): T {
    return convert()
}

//convert an object of type I to type O
inline fun <I, reified O> I.convert(): O {
    val json = gson.toJson(this)
    return gson.fromJson(json, object : TypeToken<O>() {}.type)
}

fun LatLng.getAddressString(context: Context?): String? {
    try {
        val geocoder = Geocoder(context ?: return null)
        val address = geocoder.getFromLocation(this.latitude, this.longitude, 1).firstOrNull()
        return address?.getAddressLine(0)
    } catch (e: Exception) {
        e.log()
        return null
    }
}

fun LatLng.getAddress(context: Context?): Address? {
    try {
        val geocoder = Geocoder(context ?: return null)
        val address = geocoder.getFromLocation(this.latitude, this.longitude, 1).firstOrNull()
        return address
    } catch (e: Exception) {
        e.log()
        return null
    }
}

fun List<AutocompletePrediction>?.toDropdownDisplay(): List<SpannableStringBuilder>? {
    val list = this?.map {
        SpannableStringBuilder()
            .bold { append(it.getPrimaryText(null)) }
            .append("\n${it.getSecondaryText(null)}")
    } ?: listOf()

    return if (list.isEmpty()) null else list
}

// Images

// From: https://developer.android.com/topic/performance/graphics/load-bitmap#load-bitmap
fun decodeSampledBitmapFromResource(res: Resources, resId: Int, reqWidth: Int, reqHeight: Int): Bitmap {
    // First decode with inJustDecodeBounds=true to check dimensions
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, this)

        // Calculate inSampleSize
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false

        BitmapFactory.decodeResource(res, resId, this)
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

// Errors & Messages

var dialog: AlertDialog? = null

fun showErrorMessage(message: String, activity: Activity?) {
    dismissErrorDialog()
    val alert = AlertDialog.Builder(activity ?: return, R.style.AppTheme_ErrorDialog)
            .setPositiveButton(R.string.ok, null)
            .setMessage(message)
            .setIcon(R.drawable.ic_error_icon)
            .setTitle(R.string.error)
            .show()

    dialog = alert
}

fun dismissErrorDialog() {
    dialog?.dismiss()
}

class LoggedException(message: String? = null, cause: Throwable? = null): Exception(message, cause) {

    init {
        // Only send to Crashlytics if there is a cause
        cause?.let { Crashlytics.logException(it) }
        // If there is no cause, then likely not major, so just print local log instead of sending it
        message?.let { Log.e("Logged Exception", it) }

        // For debugging/testing purposes
        if (BuildConfig.DEBUG) message?.let { Crashlytics.log(it) }
    }
}

fun Exception.log(): LoggedException {
    return LoggedException(message, cause)
}

class LoggedError(message: String?, cause: Throwable?): Error(message, cause) {

    init {
        Crashlytics.log(message)
    }
}