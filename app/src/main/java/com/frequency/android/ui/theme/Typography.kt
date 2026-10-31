package com.frequency.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Base font size for consistent scaling across the app
internal const val BASE_FONT_SIZE = com.frequency.android.util.AppConstants.UI.BASE_FONT_SIZE_SP // sp - increased from 14sp for better readability

// Typography matching the iOS monospace design - using BASE_FONT_SIZE for consistency
val Typography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = (BASE_FONT_SIZE + 1).sp, lineHeight = (BASE_FONT_SIZE + 7).sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = BASE_FONT_SIZE.sp, lineHeight = (BASE_FONT_SIZE + 3).sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = (BASE_FONT_SIZE - 3).sp, lineHeight = (BASE_FONT_SIZE + 1).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = (BASE_FONT_SIZE + 6).sp, lineHeight = (BASE_FONT_SIZE + 12).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (BASE_FONT_SIZE + 2).sp, lineHeight = (BASE_FONT_SIZE + 8).sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (BASE_FONT_SIZE - 2).sp, lineHeight = (BASE_FONT_SIZE + 3).sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = (BASE_FONT_SIZE - 4).sp, lineHeight = (BASE_FONT_SIZE + 1).sp)
)
