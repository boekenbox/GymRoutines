package com.noahjutz.gymroutines.ui.workout.insights

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.noahjutz.gymroutines.R
import java.util.Locale
import kotlin.math.roundToInt

internal fun formatWeight(value: Double): String {
    return String.format(Locale.getDefault(), "%.1f", value)
}

@Composable
internal fun PrType.displayName(): String = when (this) {
    PrType.Load -> stringResource(R.string.insights_pr_type_load)
    PrType.RepsAtLoad -> stringResource(R.string.insights_pr_type_reps_at_load)
    PrType.EstimatedOneRm -> stringResource(R.string.insights_pr_type_estimated_one_rm)
}

@Composable
internal fun PrEventUi.displayValue(): String {
    return when (type) {
        PrType.Load -> stringResource(
            R.string.insights_pr_value_weight,
            formatWeight(value),
            stringResource(R.string.insights_unit_weight)
        )

        PrType.RepsAtLoad -> stringResource(
            R.string.insights_pr_value_reps_at_load,
            value.roundToInt(),
            formatWeight(load ?: 0.0),
            stringResource(R.string.insights_unit_weight)
        )

        PrType.EstimatedOneRm -> stringResource(
            R.string.insights_pr_value_weight,
            formatWeight(value),
            stringResource(R.string.insights_unit_weight)
        )
    }
}
