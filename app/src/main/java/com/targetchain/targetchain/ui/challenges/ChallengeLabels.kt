package com.targetchain.targetchain.ui.challenges

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.targetchain.targetchain.R
import com.targetchain.targetchain.data.model.ChallengeConfig
import com.targetchain.targetchain.data.model.ChallengeType

/** Challenge types that can be added. QR/barcode and NFC come with step 5. */
val AvailableChallengeTypes = listOf(ChallengeType.PASSWORD)

@StringRes
fun ChallengeType.labelRes(): Int = when (this) {
    ChallengeType.PASSWORD -> R.string.challenge_type_password
    ChallengeType.BARCODE -> R.string.challenge_type_barcode
    ChallengeType.NFC -> R.string.challenge_type_nfc
}

/** What the challenge asks for, e.g. the name of its password. */
@Composable
fun ChallengeConfig.detail(passwordNames: Map<Long, String>): String? = when (type) {
    ChallengeType.PASSWORD ->
        passwordId?.let(passwordNames::get) ?: stringResource(R.string.challenge_password_missing)
    ChallengeType.BARCODE, ChallengeType.NFC -> null
}
