package ir.arminniromandi.permissionchecker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.arminniromandi.permissionchecker.R
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel
import ir.arminniromandi.permissionchecker.ui.theme.AppTheme

@Composable
fun RiskBadge(
    risk: RiskLevel,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val riskColors = AppTheme.riskColors
    val containerColor = riskColors.containerColorFor(risk)
    val contentColor = riskColors.onContainerColorFor(risk)

    val (labelRes, icon) = when (risk) {
        RiskLevel.HIGH -> Pair(R.string.risk_high, Icons.Filled.Warning)
        RiskLevel.MEDIUM -> Pair(R.string.risk_medium, Icons.Filled.WarningAmber)
        RiskLevel.LOW -> Pair(R.string.risk_low, Icons.Filled.Info)
        RiskLevel.NONE -> Pair(R.string.risk_none, Icons.Filled.CheckCircle)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(labelRes),
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(labelRes),
                color = contentColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
