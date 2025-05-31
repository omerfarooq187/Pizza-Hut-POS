package presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font


val poppinsFontFamily = FontFamily(
    Font(resource = "fonts/poppins_regular.ttf")
)


val customTypography = Typography(
    displayLarge = TextStyle(fontFamily = poppinsFontFamily),
    displayMedium = TextStyle(fontFamily = poppinsFontFamily),
    displaySmall = TextStyle(fontFamily = poppinsFontFamily),
    headlineLarge = TextStyle(fontFamily = poppinsFontFamily),
    headlineMedium = TextStyle(fontFamily = poppinsFontFamily),
    headlineSmall = TextStyle(fontFamily = poppinsFontFamily),
    titleLarge = TextStyle(fontFamily = poppinsFontFamily),
    titleMedium = TextStyle(fontFamily = poppinsFontFamily),
    titleSmall = TextStyle(fontFamily = poppinsFontFamily),
    bodyLarge = TextStyle(fontFamily = poppinsFontFamily),
    bodyMedium = TextStyle(fontFamily = poppinsFontFamily),
    bodySmall = TextStyle(fontFamily = poppinsFontFamily),
    labelLarge = TextStyle(fontFamily = poppinsFontFamily),
    labelMedium = TextStyle(fontFamily = poppinsFontFamily),
    labelSmall = TextStyle(fontFamily = poppinsFontFamily),
)
