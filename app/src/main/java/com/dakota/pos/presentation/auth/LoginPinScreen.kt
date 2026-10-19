package com.dakota.pos.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.model.User
import com.dakota.pos.presentation.theme.CoffeeCaramel
import com.dakota.pos.presentation.theme.CoffeeEspressoDark
import com.dakota.pos.presentation.theme.CoffeeLatte
import com.dakota.pos.presentation.theme.CoffeeWarmBrown
import com.dakota.pos.presentation.theme.PosBackgroundDark
import com.dakota.pos.presentation.theme.PosBorderDark
import com.dakota.pos.presentation.theme.PosSurfaceDark
import com.dakota.pos.presentation.theme.PosSurfaceElevatedDark
import com.dakota.pos.presentation.theme.PosTextPrimaryDark
import com.dakota.pos.presentation.theme.PosTextSecondaryDark
import com.dakota.pos.presentation.theme.StatusError

@Composable
fun LoginPinScreen(
    uiState: AuthUiState,
    onSelectUser: (User) -> Unit,
    onDigitClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackgroundDark)
    ) {
        // Left Panel: Staff Profile Selector (Tablet Landscape 35%)
        Column(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
                .background(PosSurfaceDark)
                .padding(24.dp)
        ) {
            Text(
                text = "DAKOTA POS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = CoffeeCaramel
            )
            Text(
                text = "Pilih Profil Kasir / Staff",
                style = MaterialTheme.typography.bodyMedium,
                color = PosTextSecondaryDark
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.activeUsers) { user ->
                    val isSelected = user.id == uiState.selectedUser?.id
                    StaffUserCard(
                        user = user,
                        isSelected = isSelected,
                        onClick = { onSelectUser(user) }
                    )
                }
            }
        }

        // Right Panel: PIN Pad Entry (Tablet Landscape 65%)
        Column(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val selectedUser = uiState.selectedUser
            if (selectedUser != null) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "PIN Lock",
                    tint = CoffeeCaramel,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedUser.fullName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = PosTextPrimaryDark
                )
                RoleBadge(role = selectedUser.role)

                Spacer(modifier = Modifier.height(24.dp))

                // PIN Dots Indicator
                PinDotsIndicator(
                    pinLength = uiState.enteredPin.length,
                    maxDots = if (selectedUser.role == UserRole.CASHIER) 4 else 6
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Error / Loading Message
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        color = StatusError,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center
                    )
                }

                if (uiState.isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(
                        color = CoffeeCaramel,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Numeric Keypad
                NumericKeypad(
                    onDigit = onDigitClick,
                    onBackspace = onBackspaceClick,
                    onClear = onClearClick
                )
            } else {
                Text(
                    text = "Silakan pilih profil staf di sebelah kiri untuk masuk",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PosTextSecondaryDark
                )
            }
        }
    }
}

@Composable
fun StaffUserCard(
    user: User,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) CoffeeCaramel else PosBorderDark
    val bgColor = if (isSelected) PosSurfaceElevatedDark else PosSurfaceDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isSelected) CoffeeWarmBrown else CoffeeEspressoDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (isSelected) CoffeeLatte else PosTextSecondaryDark
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = PosTextPrimaryDark
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = PosTextSecondaryDark
            )
        }

        RoleBadge(role = user.role)
    }
}

@Composable
fun RoleBadge(role: UserRole) {
    val (badgeBg, badgeText) = when (role) {
        UserRole.OWNER -> Pair(Color(0xFF4A148C), "Owner")
        UserRole.MANAGER -> Pair(Color(0xFF0D47A1), "Manager")
        UserRole.CASHIER -> Pair(Color(0xFF1B5E20), "Kasir")
        UserRole.BARISTA -> Pair(Color(0xFFE65100), "Barista")
    }

    Surface(
        color = badgeBg.copy(alpha = 0.25f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeBg)
    ) {
        Text(
            text = badgeText,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PinDotsIndicator(
    pinLength: Int,
    maxDots: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxDots) {
            val isFilled = i < pinLength
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isFilled) CoffeeCaramel else Color.Transparent)
                    .border(
                        2.dp,
                        if (isFilled) CoffeeCaramel else PosBorderDark,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val buttons = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('C', '0', 'B') // C = Clear, B = Backspace
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        buttons.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { char ->
                    KeypadButton(
                        char = char,
                        onClick = {
                            when (char) {
                                'C' -> onClear()
                                'B' -> onBackspace()
                                else -> onDigit(char)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    char: Char,
    onClick: () -> Unit
) {
    val isAction = char == 'C' || char == 'B'
    val buttonBg = if (isAction) PosSurfaceDark else PosSurfaceElevatedDark
    val contentColor = when (char) {
        'C' -> StatusError
        'B' -> CoffeeLatte
        else -> PosTextPrimaryDark
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(buttonBg)
            .border(1.dp, PosBorderDark, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (char == 'B') {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = char.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor
            )
        }
    }
}
