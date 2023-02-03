package cloud.valetudo.companion.utils

import android.widget.TextView
import androidx.annotation.StringRes

fun TextView.setText(@StringRes resId: Int, vararg vs: Any) {
    text = context.getString(resId, *vs)
}
