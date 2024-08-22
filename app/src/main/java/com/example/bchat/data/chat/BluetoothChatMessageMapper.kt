package com.example.bchat.data.chat

import com.example.bchat.domain.chat.BluetoothChatMessage

fun String.toBluetoothChatMessage(isFromLocalUser:Boolean):BluetoothChatMessage{
    val name=substringBeforeLast("#")
    val message=substringAfter("#")
    return BluetoothChatMessage(
        message=message,
        senderName = name,
        isFromLocalUser=isFromLocalUser
    )
}
fun BluetoothChatMessage.toByteArray():ByteArray{
    return "$senderName#$message".encodeToByteArray()
}
