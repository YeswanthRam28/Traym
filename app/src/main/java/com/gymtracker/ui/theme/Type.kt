package com.gymtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.gymtracker.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val AnybodyFont = GoogleFont("Anybody")
val HankenGroteskFont = GoogleFont("Hanken Grotesk")
val JetBrainsMonoFont = GoogleFont("JetBrains Mono")

val AnybodyFamily = FontFamily(
    Font(googleFont = AnybodyFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = AnybodyFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = AnybodyFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = AnybodyFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = AnybodyFont, fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = AnybodyFont, fontProvider = provider, weight = FontWeight.Black)
)

val HankenGroteskFamily = FontFamily(
    Font(googleFont = HankenGroteskFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = HankenGroteskFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = HankenGroteskFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = HankenGroteskFont, fontProvider = provider, weight = FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Bold)
)

// Material 3 Typography mappings
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = AnybodyFamily,
        fontWeight = FontWeight.Black,
        fontSize = 88.sp,
        lineHeight = 88.sp,
        letterSpacing = (-0.02).sp
    ),
    displayMedium = TextStyle(
        fontFamily = AnybodyFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.01).sp
    ),
    displaySmall = TextStyle(
        fontFamily = AnybodyFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AnybodyFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 35.2.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AnybodyFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AnybodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.05).sp
    ),
    titleMedium = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = HankenGroteskFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 27.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HankenGroteskFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = HankenGroteskFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HankenGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = HankenGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 16.sp
    )
)
