package com.example.bchat.domain.chat

sealed interface ConnectionResult{

    object ConnectionEstablished: ConnectionResult
    data class Error(val message:String):ConnectionResult
    data class TransferSucceeded(val message: BluetoothChatMessage):ConnectionResult
}