package com.example.bchat.presentation.uicomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bchat.domain.chat.BluetoothChatMessage
import com.example.bchat.ui.theme.BChatTheme
import com.example.bchat.ui.theme.OldRose
import com.example.bchat.ui.theme.Vanilla

@Composable
fun ChatMessageUI(
    message: BluetoothChatMessage,
    modifier: Modifier
){
    Column (
        modifier=modifier
            .clip(
                RoundedCornerShape(
                    topStart = if(message.isFromLocalUser) { 15.dp } else 0.dp,
                    topEnd = 15.dp,
                    bottomStart = 15.dp,
                    bottomEnd = if(message.isFromLocalUser){15.dp }else 0.dp
                )
            )
            .background(if(message.isFromLocalUser) OldRose else Vanilla)
            .padding(16.dp)
        ){
                Text(text = message.senderName, fontSize = 10.sp, color = Color.Black)
                Text(text = message.message, color = Color.Black,modifier=Modifier.widthIn(max= 250.dp))
        }



}

@Preview
@Composable
fun ChatMessageUIPreview(){
    BChatTheme() {
        BluetoothChatMessage(
            message = "Hello World",
            senderName = "Pixel 6",
            isFromLocalUser = true
        )

    }
}