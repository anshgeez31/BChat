package com.example.bchat.presentation.uicomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bchat.domain.chat.BluetoothDevice
import com.example.bchat.presentation.BluetoothUiState
import com.example.bchat.ui.theme.Yellow

@Composable
fun DeviceScreen(
    state:BluetoothUiState,
    onStartScan:()->Unit,
    onStopScan:()->Unit,
    onStartServer:()->Unit,
    onDeviceClick: (BluetoothDevice)->Unit

) {
    Column (
        modifier=Modifier
            .fillMaxSize()
        ){
        BluetoothDeviceList(pairedDevices = state.pairedDevices,
            scannedDevices =state.scannedDevices ,
            onClick = onDeviceClick,
        modifier = Modifier.fillMaxWidth().weight(1f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = onStartScan,colors=ButtonDefaults.buttonColors(backgroundColor = Yellow)) {
                Text(text = "Start Scan", color = Color.Black)
            }
            Button(onClick = onStopScan,colors=ButtonDefaults.buttonColors(backgroundColor = Yellow)) {
                Text(text = "Stop Scan",color = Color.Black)
            }
            Button(onClick = onStartServer,colors=ButtonDefaults.buttonColors(backgroundColor = Yellow)) {
                Text(text = "Start Server",color = Color.Black)
            }
        }
    }
}

@Composable
fun BluetoothDeviceList(
    pairedDevices:List<BluetoothDevice>,
    scannedDevices:List<BluetoothDevice>,
    onClick:(BluetoothDevice)->Unit,
    modifier: Modifier=Modifier
) {
    LazyColumn(modifier = modifier
    ){
        item {
            Text(text = "Paired Devices",
                color=Color.White,
            fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(pairedDevices){device->
            Text(text = device.name?:"(No name)",
                color=Color.White,
                modifier= Modifier
                    .fillMaxWidth()
                    .clickable { onClick(device) }
                    .padding(16.dp)
            )
        }

        item {
            Text(text = "Scanned Devices",
                color=Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(scannedDevices){device->
            Text(text = device.name?:"(No name)",
                color=Color.White,
                modifier= Modifier
                    .fillMaxWidth()
                    .clickable { onClick(device) }
                    .padding(16.dp)
            )
        }
    }

}