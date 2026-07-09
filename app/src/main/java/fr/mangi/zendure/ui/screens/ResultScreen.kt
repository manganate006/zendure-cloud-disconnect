package fr.mangi.zendure.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.mangi.zendure.R
import fr.mangi.zendure.model.ProvisioningState
import fr.mangi.zendure.model.ProvisioningStep
import fr.mangi.zendure.ui.components.CopyableField
import fr.mangi.zendure.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(viewModel: MainViewModel, onRetry: () -> Unit, onFinish: () -> Unit) {
    val state by viewModel.provisioning.collectAsState()

    // Pendant le provisioning, le retour arrière est bloqué (opération en cours sur l'appareil).
    BackHandler(enabled = state is ProvisioningState.Running) { }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.result_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepChecklist(state)

            when (val s = state) {
                is ProvisioningState.Running -> {
                    if (s.step == ProvisioningStep.AWAITING_REBOOT) {
                        Text(
                            stringResource(R.string.reboot_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is ProvisioningState.Success -> SuccessContent(viewModel, s, onFinish)

                is ProvisioningState.Failure -> {
                    Text(
                        stringResource(R.string.error_at_step, stepLabel(s.step)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        s.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.retry))
                    }
                }

                ProvisioningState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun StepChecklist(state: ProvisioningState) {
    val currentIndex = when (state) {
        is ProvisioningState.Running -> state.step.ordinal
        is ProvisioningState.Failure -> state.step.ordinal
        is ProvisioningState.Success -> ProvisioningStep.entries.size
        ProvisioningState.Idle -> -1
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProvisioningStep.entries.forEach { step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        step.ordinal < currentIndex -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )

                        step.ordinal == currentIndex && state is ProvisioningState.Failure -> Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )

                        step.ordinal == currentIndex && state is ProvisioningState.Running ->
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )

                        else -> Icon(
                            Icons.Outlined.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(stepLabel(step), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    viewModel: MainViewModel,
    state: ProvisioningState.Success,
    onFinish: () -> Unit,
) {
    Text(
        stringResource(R.string.success_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        if (state.restoredCloud) {
            stringResource(R.string.success_cloud)
        } else {
            stringResource(R.string.success_local, state.brokerHost)
        },
        style = MaterialTheme.typography.bodyMedium,
    )

    val productKey = viewModel.selected?.model?.productKey
    val deviceId = state.info?.deviceId

    if (state.info == null) {
        Text(
            stringResource(R.string.info_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            deviceId?.let { CopyableField(stringResource(R.string.device_id), it) }
            state.info?.deviceSn?.let { CopyableField(stringResource(R.string.device_sn), it) }
            productKey?.let { CopyableField(stringResource(R.string.product_key), it) }

            if (!state.restoredCloud && productKey != null && deviceId != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    stringResource(R.string.topics_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                CopyableField(
                    stringResource(R.string.topic_report),
                    "/$productKey/$deviceId/properties/report",
                )
                CopyableField(
                    stringResource(R.string.topic_write),
                    "iot/$productKey/$deviceId/properties/write",
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                CopyableField(
                    stringResource(R.string.verify_title),
                    "mosquitto_sub -h ${state.brokerHost} -v -t '/$productKey/#'",
                )
            }
        }
    }

    OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.finish))
    }
}

@Composable
private fun stepLabel(step: ProvisioningStep): String = stringResource(
    when (step) {
        ProvisioningStep.CONNECTING -> R.string.step_connecting
        ProvisioningStep.DISCOVERING_SERVICES -> R.string.step_discovering
        ProvisioningStep.NEGOTIATING_MTU -> R.string.step_mtu
        ProvisioningStep.ENABLING_NOTIFICATIONS -> R.string.step_notifications
        ProvisioningStep.READING_INFO -> R.string.step_reading_info
        ProvisioningStep.SENDING_CONFIG -> R.string.step_sending_config
        ProvisioningStep.APPLYING -> R.string.step_applying
        ProvisioningStep.AWAITING_REBOOT -> R.string.step_awaiting_reboot
    }
)
