package fr.mangi.zendure.ui.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import fr.mangi.zendure.R

/**
 * N'affiche [content] que lorsque les permissions Bluetooth sont accordées
 * et que le Bluetooth est activé.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothPermissionGate(content: @Composable () -> Unit) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val state = rememberMultiplePermissionsState(permissions)

    if (state.allPermissionsGranted) {
        BluetoothEnabledGate(content)
    } else {
        val anyRationale = state.permissions.any { it.status.shouldShowRationale }
        GateMessage(
            icon = Icons.Default.Bluetooth,
            title = stringResource(R.string.perm_title),
            message = stringResource(R.string.perm_rationale),
            buttonLabel = stringResource(R.string.perm_grant),
            onClick = { state.launchMultiplePermissionRequest() },
            footer = if (anyRationale) stringResource(R.string.perm_denied_hint) else null,
        )
    }
}

@Composable
private fun BluetoothEnabledGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var bluetoothEnabled by remember { mutableStateOf(context.isBluetoothEnabled()) }
    val enableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        bluetoothEnabled = context.isBluetoothEnabled()
    }
    LifecycleResumeEffect(Unit) {
        bluetoothEnabled = context.isBluetoothEnabled()
        onPauseOrDispose { }
    }

    if (bluetoothEnabled) {
        content()
    } else {
        GateMessage(
            icon = Icons.Default.BluetoothDisabled,
            title = stringResource(R.string.bt_disabled),
            message = "",
            buttonLabel = stringResource(R.string.bt_enable),
            onClick = {
                enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            },
        )
    }
}

@Composable
private fun GateMessage(
    icon: ImageVector,
    title: String,
    message: String,
    buttonLabel: String,
    onClick: () -> Unit,
    footer: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (message.isNotEmpty()) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onClick) { Text(buttonLabel) }
        footer?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun Context.isBluetoothEnabled(): Boolean =
    (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled == true

fun Context.isLocationEnabled(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return true
    val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return LocationManagerCompat.isLocationEnabled(manager)
}
