package com.example.bchat.data.chat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.bchat.domain.chat.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain():BluetoothDeviceDomain{
    return BluetoothDeviceDomain(
        name = name,
        address=address
    )
}