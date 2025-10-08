package com.noahjutz.gymroutines.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import androidx.compose.ui.res.stringResource

val WarmupIndicatorWidth: Dp = 56.dp

@Composable
fun SetTypeBadge(
    isWarmup: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    onToggle: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colors
    val backgroundColor = if (isWarmup) {
        colors.primary.copy(alpha = 0.16f)
    } else {
        colors.onSurface.copy(alpha = 0.08f)
    }
    val textColor = if (isWarmup) colors.primary else colors.onSurface
    val label = if (isWarmup) stringResource(R.string.set_type_warmup_short) else (index + 1).toString()
    val description = if (isWarmup) {
        stringResource(R.string.set_type_warmup)
    } else {
        stringResource(R.string.set_type_working, index + 1)
    }

    val clickableModifier = if (onToggle != null) {
        Modifier.clickable(onClick = onToggle)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .then(clickableModifier)
            .semantics { contentDescription = description },
        color = backgroundColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
