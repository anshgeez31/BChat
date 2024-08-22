package com.example.bchat.presentation

import com.example.bchat.domain.chat.BluetoothChatMessage
import com.example.bchat.domain.chat.BluetoothDevice

data class BluetoothUiState(
    val scannedDevices:List<BluetoothDevice> = emptyList(),
    val pairedDevices:List<BluetoothDevice> = emptyList(),
    val isConnected:Boolean=false,
    val isConnecting:Boolean=false,
    val errorMessage:String?=null,
    val message: List<BluetoothChatMessage> = emptyList()
)
