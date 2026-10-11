package com.pocket.assistant.location.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.pocket.assistant.location.CoordType

@Composable
fun ManualInputDialog(
    initialType: CoordType,
    onDismiss: () -> Unit,
    onConfirm: (lat: Double, lon: Double, type: CoordType) -> Unit,
) {
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(initialType) }

    val lat = latText.toDoubleOrNull()
    val lon = lonText.toDoubleOrNull()
    val valid = lat != null && lon != null &&
        lat in -90.0..90.0 && lon in -180.0..180.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动输入坐标") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("纬度 lat（-90 ~ 90）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = lonText,
                    onValueChange = { lonText = it },
                    label = { Text("经度 lon（-180 ~ 180）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text("坐标系")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoordType.values().forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { if (valid) onConfirm(lat!!, lon!!, type) },
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
