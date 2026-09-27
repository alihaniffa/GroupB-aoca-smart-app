package org.example.dementia_tester_app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A reusable horizontal menu component that can be used across different screens
 * 
 * @param menuItems List of menu items to display
 * @param selectedMenuItem Currently selected menu item
 * @param activeColor Color to use for the active menu item
 * @param fontSize Optional custom font size for menu items (defaults to 14.sp when >3 items, otherwise 18.sp)
 * @param onMenuItemSelected Callback when a menu item is selected
 */
@Composable
fun HorizontalMenu(
    menuItems: List<String>,
    selectedMenuItem: String,
    activeColor: Color,
    fontSize: TextUnit = if (menuItems.size > 3) 14.sp else 18.sp,
    onMenuItemSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        menuItems.forEach { item ->
            val isSelected = item == selectedMenuItem

            Text(
                text = item,
                fontSize = fontSize,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isSelected) TextDecoration.Underline else TextDecoration.None,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onMenuItemSelected(item) }
            )
        }
    }
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
    )
}
