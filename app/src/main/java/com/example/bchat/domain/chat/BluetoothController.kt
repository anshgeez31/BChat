package com.example.bchat.domain.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val isConnected:StateFlow<Boolean>
    val scannedDevices:StateFlow<List<BluetoothDevice>>
    val pairedDevices:StateFlow<List<BluetoothDevice>>
    val errors:SharedFlow<String>

    fun startScanning()
    fun stopScanning()

//    this func launch the server ,that has device A has to do
    fun startBluetoothServer(): Flow<ConnectionResult>

//    this func connect the devices to the server , that has device B has to do
    fun connectToDevice(device:BluetoothDevice):Flow<ConnectionResult>

    suspend fun trySendMessage(message: String):BluetoothChatMessage?

    fun closeConnection()

    fun release()
//    for free all the memory and disconnect from the devices

}
//flow is a reactive data structure ,if we start a server this is a blocking action so as long we connect
//we have to keep listening the events like when we have a new message any one have to notify