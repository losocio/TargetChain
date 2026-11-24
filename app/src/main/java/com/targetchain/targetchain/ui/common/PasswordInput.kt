package com.targetchain.targetchain.ui.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.targetchain.targetchain.challenge.insertedCharCount

/**
 * Password keyboard type: Samsung Keyboard and Gboard don't learn, suggest or autocorrect in
 * password fields, also when the text is visible.
 */
val PasswordKeyboardOptions = KeyboardOptions(
    keyboardType = KeyboardType.Password,
    autoCorrectEnabled = false,
    imeAction = ImeAction.Done,
)

/**
 * Accepts only changes that insert at most one character, so the password must be typed by hand:
 * pasting (field menu, keyboard clipboard), autofill, suggestions and dictation insert several
 * characters at once and are rejected.
 */
object TypedByHandOnly : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (insertedCharCount(originalText, asCharSequence()) > 1) revertAllChanges()
    }
}

/** Text toolbar that never shows, so the field has no "Paste" menu. */
object NoTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun hide() = Unit

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit
}
