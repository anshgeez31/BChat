package com.example.bchat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bchat.domain.chat.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) :ViewModel() {
    private val _state=MutableStateFlow(BluetoothUiState())
    val state= combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _state
    ){scannedDevices,pairedDevices,state->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            message = if(state.isConnected)
            {
                state.message
            }
            else emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),_state.value)


    private var deviceConnectionJob:Job?=null

    init {
        bluetoothController.isConnected.onEach { isConnected->
            _state.update { it.copy(isConnected=isConnected) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error->
            _state.update { it.copy(errorMessage = error) }
        }.launchIn(viewModelScope)
    }

    fun connectToDevice(device: BluetoothDeviceDomain)
    {
        _state.update {it.copy(isConnecting = true) }
        deviceConnectionJob=bluetoothController
            .connectToDevice(device)
            .listen()
    }
    fun disconnectFromDevice(){
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _state.update { it.copy(
            isConnecting = false,
            isConnected = false
        ) }
    }

    fun waitForIncomingConnections(){
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob=bluetoothController
            .startBluetoothServer()
            .listen()
    }
    fun sendMessage(message: String){
        viewModelScope.launch {
            val bluetoothChatMessage=bluetoothController.trySendMessage(message)
            if(bluetoothChatMessage!=null)
            {
                _state.update { it.copy(
                    message = it.message+bluetoothChatMessage
                ) }
            }
        }
    }

    fun startScan()
    {
        bluetoothController.startScanning()
    }

    fun stopScan()
    {
        bluetoothController.stopScanning()
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result->
            when(result)
            {
                ConnectionResult.ConnectionEstablished->{
                    _state.update { it.copy(
                        isConnected = true,
                        isConnecting = false,
                        errorMessage = null
                    ) }
                }
                is ConnectionResult.TransferSucceeded->{
                    _state.update { it.copy(
                        message = it.message+result.message
                    ) }
                }
                is ConnectionResult.Error->{
                    _state.update { it.copy(
                        isConnected = false,
                        isConnecting = false,
                        errorMessage = result.message
                    ) }
                }
            }
        }
            .catch {throwable ->
                bluetoothController.closeConnection()
                _state.update { it.copy(
                    isConnecting = false,
                    isConnected = false
                ) }
            }.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}
//BluetoothViewModel  is used for interact with the BluetoothController and then map the result
//to the UI state
//stateIn--> change from normal flow to state flow
