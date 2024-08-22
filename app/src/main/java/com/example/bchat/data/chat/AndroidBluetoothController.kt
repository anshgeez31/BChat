package com.example.bchat.data.chat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.example.bchat.domain.chat.BluetoothChatMessage
import com.example.bchat.domain.chat.BluetoothController
import com.example.bchat.domain.chat.BluetoothDeviceDomain
import com.example.bchat.domain.chat.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class AndroidBluetoothController(
    private val context:Context
):BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
//        null check for devices which does not support bluetooth
    }

    private var dataTransferService:BluetoothDataTransferService?=null

    private val _isConnected=MutableStateFlow<Boolean>(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices=MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices=MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _errors= MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private var currentBluetoothServerSocket:BluetoothServerSocket?=null
    private var currentBluetoothClientSocket: BluetoothSocket?=null


    @SuppressLint("MissingPermission")
    private val bluetoothStateReceiver=BluetoothStateReceiver{ isConnected, bluetoothDevice ->
        if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice)==true)
        {
            _isConnected.update { isConnected }
        }
        else
        {
            CoroutineScope(Dispatchers.IO).launch{
                _errors.emit("Can't Connect to a non-paired device!!")
            }
        }

    }

    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }
    private val foundDeviceReceiver=FoundDeviceReceiver{ device->
        _scannedDevices.update { devices->
            val newDevice=device.toBluetoothDeviceDomain()
            if(newDevice in devices)
            {
                devices
            }
            else
            {
                devices+newDevice
            }
        }
    }



    override fun startScanning() {
        if(!checkPermission(Manifest.permission.BLUETOOTH_SCAN))
        {
            return
        }
        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopScanning() {
        if(!checkPermission(Manifest.permission.BLUETOOTH_SCAN))
        {
            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }


    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow{
            if(!checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }
           currentBluetoothServerSocket= bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "chat_service",
                UUID.fromString(SERVICE_UUID)
            )

//            we have to block this background thread as long as we open for to accept for devices
            var shouldLoop=true
            while (shouldLoop)
            {
               currentBluetoothClientSocket= try {
                    currentBluetoothServerSocket?.accept()
//                   accept  actually block the thread until current Bluetooth Server Socket is active
//                   server socket is for only connections
//                   client socket is for keep connected to server

                }catch (e:IOException){
                    shouldLoop=false
                    null
                }
                emit(ConnectionResult.ConnectionEstablished)
                currentBluetoothClientSocket?.let {
                    currentBluetoothServerSocket?.close()
                    val service=BluetoothDataTransferService(it)
                    dataTransferService=service

                    emitAll(service
                        .listenForIncomingMessages()
                        .map { ConnectionResult.TransferSucceeded(it) })

                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return  flow {
            if(!checkPermission(Manifest.permission.BLUETOOTH_CONNECT))
            {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }

            val bluetoothDevice=bluetoothAdapter?.getRemoteDevice(device.address)


            currentBluetoothClientSocket=bluetoothAdapter
                ?.getRemoteDevice(device.address)
                ?.createRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                )
            stopScanning()
            if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice)==false)
            {

            }

            currentBluetoothClientSocket?.let {socket->
                try {
                    socket.connect()
                    emit(ConnectionResult.ConnectionEstablished)

                    BluetoothDataTransferService(socket).also {
                        dataTransferService=it
                        emitAll(
                            it.listenForIncomingMessages()
                                .map{ConnectionResult.TransferSucceeded(it)}
                        )
                    }

                }catch (e:IOException)
                {
                    socket.close()
                    currentBluetoothClientSocket=null
                    emit(ConnectionResult.Error("connection was interrupted"))
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)

    }

    override suspend fun trySendMessage(message: String): BluetoothChatMessage? {
        if(!checkPermission(Manifest.permission.BLUETOOTH_CONNECT))
        {
            return null
        }
        if(dataTransferService==null)
        {
            return null
        }
        val bluetoothChatMessage=BluetoothChatMessage(
            message=message,
            senderName =bluetoothAdapter?.name?:"Unknown Name",
            isFromLocalUser = true
        )
        dataTransferService?.sendMessage(bluetoothChatMessage.toByteArray())
        return bluetoothChatMessage
    }

    override fun closeConnection() {
        currentBluetoothClientSocket?.close()
        currentBluetoothServerSocket?.close()
        currentBluetoothClientSocket=null
        currentBluetoothServerSocket=null

    }
    override fun release() {
        context.unregisterReceiver(foundDeviceReceiver)
        context.unregisterReceiver(bluetoothStateReceiver)
        closeConnection()
    }


    private fun  updatePairedDevices()
    {
        if(!checkPermission(Manifest.permission.BLUETOOTH_CONNECT))
        {
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toBluetoothDeviceDomain() }
            ?.also {devices->
                _pairedDevices.update { devices } }
    }

    private  fun checkPermission(permission: String):Boolean{
        return context.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED
    }

    companion object{
        const val SERVICE_UUID="27b7d1da-08c7-4505-a6d1-2459987e5e2d"
    }
}
//bluetooth Adapter is a hardware Module that contains functionality to where on the one hand get
//your own Mac Address ,your own bluetooth name  but also to be list of scan devices to get  of
//paired devices to initiate scan and others