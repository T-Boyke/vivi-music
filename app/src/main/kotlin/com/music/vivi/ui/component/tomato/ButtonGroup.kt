package com.music.vivi.ui.component.tomato

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * A Segmented Button Group.
 * Often used in Tomato for filtering or grouping actions.
 */
@Composable
fun ButtonGroup(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex

            // Shape logic for start, middle, end buttons
            val shape = when (index) {
                0 -> RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius, topEnd = 0.dp, bottomEnd = 0.dp)
                items.lastIndex -> RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = cornerRadius, bottomEnd = cornerRadius)
                else -> RoundedCornerShape(0.dp)
            }

            // Offset to overlap borders slightly (prevent double border thickness)
            val offsetModifier = if (index > 0) Modifier.offset(x = (-1 * index).dp) else Modifier

            OutlinedButton(
                onClick = { onItemSelected(index) },
                modifier = Modifier
                    .weight(1f)
                    .then(offsetModifier)
                    .zIndex(if (isSelected) 1f else 0f), // Bring selected to front
                shape = shape,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) color else MaterialTheme.colorScheme.outline
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
                    contentColor = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
