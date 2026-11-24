package com.targetchain.targetchain.ui.password

import androidx.annotation.StringRes
import com.targetchain.targetchain.R
import com.targetchain.targetchain.challenge.NewPasswordError
import com.targetchain.targetchain.challenge.PasswordNameError

@StringRes
fun PasswordNameError.messageRes(): Int = when (this) {
    PasswordNameError.EMPTY -> R.string.password_name_empty
    PasswordNameError.DUPLICATE -> R.string.password_name_duplicate
}

@StringRes
fun NewPasswordError.messageRes(): Int = when (this) {
    NewPasswordError.EMPTY -> R.string.password_empty
    NewPasswordError.MISMATCH -> R.string.password_mismatch
}
