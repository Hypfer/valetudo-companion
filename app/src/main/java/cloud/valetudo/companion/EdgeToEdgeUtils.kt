package cloud.valetudo.companion

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom

fun View.applySystemBarBottomMarginInset() {
    val lp = layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
    val originalBottom = lp?.bottomMargin ?: marginBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val newLp = v.layoutParams
        if (newLp is androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
            newLp.bottomMargin = originalBottom + systemBars.bottom
            v.layoutParams = newLp
        }
        ViewCompat.onApplyWindowInsets(v, insets)
    }
}

fun View.applyImeBottomMarginInset() {
    val lp = layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
    val originalBottom = lp?.bottomMargin ?: marginBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        val bottom = maxOf(systemBars.bottom, ime.bottom)
        val newLp = v.layoutParams
        if (newLp is androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
            newLp.bottomMargin = originalBottom + bottom
            v.layoutParams = newLp
        }
        ViewCompat.onApplyWindowInsets(v, insets)
    }
}

fun View.applyStatusBarTopInset() {
    val originalTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        v.setPadding(
            v.paddingLeft,
            originalTop + systemBars.top,
            v.paddingRight,
            v.paddingBottom
        )
        ViewCompat.onApplyWindowInsets(v, insets)
    }
}
