package com.jeremy.thunder

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.jeremy.thunder.socket.SocketService
import com.jeremy.thunder.socket.Ticket
import com.jeremy.thunder.socket.Type
import com.jeremy.thunder.ui.theme.ThunderTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThunderTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //1. OkHttpClient 생성
                    val okHttpClient = OkHttpClient.Builder().addInterceptor(
                        HttpLoggingInterceptor().setLevel(
                            HttpLoggingInterceptor.Level.BODY
                        )
                    ).build()

                    //2. Thunder를 사용하여 Service 생성
                    val context = App.context()
                    val service = Thunder.Builder()
                        .webSocketCore(okHttpClient.makeWebSocketCore("wss://api.upbit.com/websocket/v1"))
                        .setApplicationContext(context)
                        .build()
                        .create<SocketService>()

                    //3. 데이터 요청
                    val requestList = listOf(
                        Ticket(),
                        Type(
                            type = "ticker"
                        ),
                        Type(
                            type = "trade"
                        ),
                    )

                    LaunchedEffect(key1 = Unit) {
                        delay(2000)
                        service.request(request = requestList)
                    }
                }
            }
        }
    }
}