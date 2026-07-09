package fr.mangi.zendure.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import fr.mangi.zendure.R
import fr.mangi.zendure.util.TimeZoneUtil
import fr.mangi.zendure.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(viewModel: MainViewModel, onApply: () -> Unit, onBack: () -> Unit) {
    val device = viewModel.selected
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.config_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (device != null) {
                Column {
                    Text(
                        text = device.model?.label ?: stringResource(R.string.unknown_model),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "${device.name} — ${device.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    device.model?.let {
                        Text(
                            text = "${stringResource(R.string.product_key)} : ${it.productKey}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !viewModel.restoreCloud,
                    onClick = { viewModel.restoreCloud = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.mode_local)) }
                SegmentedButton(
                    selected = viewModel.restoreCloud,
                    onClick = { viewModel.restoreCloud = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.mode_cloud)) }
            }

            OutlinedTextField(
                value = viewModel.ssid,
                onValueChange = { viewModel.ssid = it },
                label = { Text(stringResource(R.string.field_ssid)) },
                supportingText = { Text(stringResource(R.string.ssid_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = viewModel.wifiPassword,
                onValueChange = { viewModel.wifiPassword = it },
                label = { Text(stringResource(R.string.field_password)) },
                singleLine = true,
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(R.string.show_password),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (!viewModel.restoreCloud) {
                OutlinedTextField(
                    value = viewModel.brokerHost,
                    onValueChange = { viewModel.brokerHost = it },
                    label = { Text(stringResource(R.string.field_broker)) },
                    supportingText = { Text(stringResource(R.string.broker_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = viewModel.timeZone,
                onValueChange = { viewModel.timeZone = it },
                label = { Text(stringResource(R.string.field_timezone)) },
                supportingText = { Text(stringResource(R.string.timezone_hint)) },
                isError = !TimeZoneUtil.isValid(viewModel.timeZone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            WarningCard(
                text = if (viewModel.restoreCloud) {
                    stringResource(R.string.cloud_info)
                } else {
                    stringResource(R.string.warning_anonymous)
                },
            )

            Button(
                onClick = onApply,
                enabled = viewModel.isFormValid(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.apply))
            }
        }
    }
}

@Composable
private fun WarningCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}
