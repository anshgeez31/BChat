package com.example.bchat.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bchat.presentation.uicomponents.ChatScreen
import com.example.bchat.presentation.uicomponents.DeviceScreen
import com.example.bchat.ui.theme.BChatTheme
import com.example.bchat.ui.theme.DarkSlateGrey
import com.example.bchat.ui.theme.DarkSlateGreyLight
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val bluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
//        null check for devices which does not support bluetooth
    }

    private val isBluetoothEnabledSetting:Boolean
    get()=bluetoothAdapter?.isEnabled==true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
//                    Not Needed as it show after enabling bluetooth enabling from dialog box
        }

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permission ->
            val canEnableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permission[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else true

            if (canEnableBluetooth && !isBluetoothEnabledSetting) {
                enableBluetoothLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }


        setContent {
            BChatTheme {
                val viewModel = hiltViewModel<BluetoothViewModel>()
                val state by viewModel.state.collectAsState()

                LaunchedEffect(key1 = state.errorMessage){
                    state.errorMessage?.let { message->
                        Toast.makeText(
                            applicationContext,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                LaunchedEffect(key1 = state.isConnected ){
                    if(state.isConnected){
                        Toast.makeText(applicationContext,"You're Connected!",Toast.LENGTH_LONG).show()
                    }
                }

                Surface(
                    color = DarkSlateGreyLight
                ) {
                    when {
                        state.isConnecting -> {
                            Column(
                                modifier=Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Text(text = "Connecting..")
                            }
                        }
                        state.isConnected->{
                            ChatScreen(state = state,
                                onDisconnect = viewModel::disconnectFromDevice,
                                onSendMessage =viewModel::sendMessage )
                        }
                        else -> {
                            DeviceScreen(
                                state = state,
                                onStartScan = viewModel::startScan,
                                onStopScan = viewModel::stopScan,
                                onDeviceClick = viewModel::connectToDevice,
                                onStartServer = viewModel::waitForIncomingConnections)

                        }
                    }
                }
            }
        }
    }
}

