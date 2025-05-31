package presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. Define Theme Colors
val PizzaRed = Color(0xFFC41E3A)
val PizzaCrust = Color(0xFFE3B448)
val PizzaSauce = Color(0xFF8B0000)
val PizzaCheese = Color(0xFFFFD700)
val PizzaWhite = Color(0xFFFDFDFD)
val PizzaBlack = Color(0xFF2D2D2D)

// 2. Create Custom Color Scheme
private val PizzaColorScheme = lightColorScheme(
    primary = PizzaRed,
    secondary = PizzaCrust,
    tertiary = PizzaCheese,
    background = PizzaWhite,
    surface = PizzaWhite,
    onPrimary = PizzaWhite,
    onSecondary = PizzaBlack,
    onTertiary = PizzaBlack,
    onBackground = PizzaBlack,
    onSurface = PizzaBlack,
    error = PizzaSauce
)

// 3. Create Theme Composable
@Composable
fun PizzaHutTheme(
    darkTheme: Boolean = false, // Add dark theme support if needed
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PizzaColorScheme,
        typography = PizzaTypography,
        shapes = PizzaShapes,
        content = content
    )
}

// 4. Define Typography
val PizzaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    // Define other text styles similarly...
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = PizzaBlack
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = PizzaRed
    )
)

// 5. Define Shapes
val PizzaShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun PizzaButtonColors() = ButtonDefaults.buttonColors(
    containerColor = PizzaRed,
    contentColor = PizzaWhite
)

@Composable
fun PizzaOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = PizzaRed
)