package com.example.bchat.domain.chat

data class BluetoothChatMessage(
    val message: String,
    val senderName: String,
    val isFromLocalUser:Boolean
)
