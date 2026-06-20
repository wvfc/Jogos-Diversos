package com.joguecomigo.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.joguecomigo.domain.Difficulty
import java.util.Locale

/** Formata segundos como mm:ss. */
fun formatTime(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

/**
 * Linha de seleção de dificuldade. Apenas as dificuldades até [unlocked]
 * podem ser escolhidas (níveis desbloqueados).
 */
@Composable
fun DifficultySelector(
    selected: Difficulty,
    unlocked: Difficulty,
    onSelect: (Difficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Difficulty.entries.forEach { difficulty ->
            val enabled = difficulty.order <= unlocked.order
            FilterChip(
                selected = difficulty == selected,
                onClick = { if (enabled) onSelect(difficulty) },
                enabled = enabled,
                label = { Text(difficulty.label) },
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .selectable(selected = difficulty == selected, enabled = enabled) {},
            )
        }
    }
}
