package ir.arminniromandi.permissionchecker.ui.applist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.arminniromandi.permissionchecker.R
import ir.arminniromandi.permissionchecker.data.source.AppIconLoader
import ir.arminniromandi.permissionchecker.domain.model.AppInfo
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel
import ir.arminniromandi.permissionchecker.ui.common.AppIconImage
import ir.arminniromandi.permissionchecker.ui.common.EmptyState
import ir.arminniromandi.permissionchecker.ui.common.RiskBadge
import ir.arminniromandi.permissionchecker.ui.theme.AppTheme

@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    iconLoader: AppIconLoader,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            // Filter Chips Horizontal Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.selectedRiskFilter == null,
                    onClick = { viewModel.onRiskFilterChanged(null) },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
                FilterChip(
                    selected = uiState.selectedRiskFilter == RiskLevel.HIGH,
                    onClick = {
                        val newFilter = if (uiState.selectedRiskFilter == RiskLevel.HIGH) null else RiskLevel.HIGH
                        viewModel.onRiskFilterChanged(newFilter)
                    },
                    label = { Text(stringResource(R.string.filter_high)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppTheme.riskColors.highContainer,
                        selectedLabelColor = AppTheme.riskColors.onHighContainer
                    )
                )
                FilterChip(
                    selected = uiState.selectedRiskFilter == RiskLevel.MEDIUM,
                    onClick = {
                        val newFilter = if (uiState.selectedRiskFilter == RiskLevel.MEDIUM) null else RiskLevel.MEDIUM
                        viewModel.onRiskFilterChanged(newFilter)
                    },
                    label = { Text(stringResource(R.string.filter_medium)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppTheme.riskColors.mediumContainer,
                        selectedLabelColor = AppTheme.riskColors.onMediumContainer
                    )
                )
                FilterChip(
                    selected = uiState.selectedRiskFilter == RiskLevel.LOW,
                    onClick = {
                        val newFilter = if (uiState.selectedRiskFilter == RiskLevel.LOW) null else RiskLevel.LOW
                        viewModel.onRiskFilterChanged(newFilter)
                    },
                    label = { Text(stringResource(R.string.filter_low)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppTheme.riskColors.lowContainer,
                        selectedLabelColor = AppTheme.riskColors.onLowContainer
                    )
                )
            }

            // Include System Apps Checkbox row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onIncludeSystemAppsToggled(!uiState.includeSystemApps) }
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.includeSystemApps,
                    onCheckedChange = { viewModel.onIncludeSystemAppsToggled(it) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.filter_system_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Apps List
            if (uiState.apps.isEmpty()) {
                EmptyState(message = stringResource(R.string.no_apps_found))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.apps,
                        key = { it.packageName }
                    ) { app ->
                        AppListItemCard(
                            app = app,
                            iconLoader = iconLoader,
                            onClick = { onNavigateToDetail(app.packageName) }
                        )
                    }
                }
            }
        }
}

@Composable
private fun AppListItemCard(
    app: AppInfo,
    iconLoader: AppIconLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                packageName = app.packageName,
                iconLoader = iconLoader,
                size = 52.dp,
                contentDescription = app.appName
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (app.isSystemApp) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.system_app_tag),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            RiskBadge(risk = app.overallRisk)
        }
    }
}
