package com.jeremy.thunder.di

import android.content.Context
import com.jeremy.thunder.event.converter.ConverterType
import com.jeremy.thunder.makeWebSocketCore
import com.jeremy.thunder.socket.SocketService
import com.jeremy.thunder.thunder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SocketModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)
        return OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideSocketService(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): SocketService {
        return thunder {
            webSocketCore(okHttpClient.makeWebSocketCore("wss://fstream.binance.com/stream"))
            setApplicationContext(context)
            setConverterType(ConverterType.Serialization)
        }.create()
    }
}