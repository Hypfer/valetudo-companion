package cloud.valetudo.companion.utils

import android.view.View

fun View.setVisibility(isVisible: Boolean) {
    this.visibility = if (isVisible) View.VISIBLE else View.GONE
}