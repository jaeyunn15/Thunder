# 🚧🚧🚧🚧 Work In Progress 🚧🚧🚧🚧

# Thunder

A WebSocket library that draws heavily from the [Scarlet](https://github.com/Tinder/Scarlet) by Tinder.     
The overall design of this library is very similar to Scarlet.     
Currently, support only for Coroutine Flow.     

---
## Download First

First, go to your settings.gradle file and add the code below.
~~~ groovy
[settings.gradle]

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // <-- here! you have to copy & paste
    }
}
~~~

Use the library by adding a Dependency to the module you want to use.

~~~ groovy
dependencies {
    implementation 'com.github.jaeyunn15:Thunder:0.0.2'
}
~~~

---
## Restriction 🚨
We need to target **Java 17** because the library is aligned with **Java 17**.      
In the future, we will respond by lowering the Java version.



---
## Usage

First, we need to define an interface to request data and receive responses.
~~~ kotlin
interface SocketService {

    @Send
    fun request(request: BinanceRequest)

    @Receive
    fun response(): Flow<TickerResponse>
}
~~~


Second, we need to create a Thunder instance, which requires an ApplicationContext and an OkHttpClient.
~~~ kotlin

val thunderInstance = Thunder.Builder()
    .webSocketCore(okHttpClient.makeWebSocketCore("wss://fstream.binance.com/stream"))
    .setApplicationContext(context)
    .build()

thunderInstance.create<SocketService>()
~~~



Alternatively, you can use a dependency injection library like Hilt to create them for you.

~~~ kotlin

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
        return Thunder.Builder()
            .webSocketCore(okHttpClient.makeWebSocketCore("wss://fstream.binance.com/stream"))
            .setApplicationContext(context)
            .build()
            .create()
    }
~~~



You can request data based on the request format, and receive data using Flow, depending on the response format.
A single request/response example used in the AAC viewmodel.

~~~ kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val service: SocketService
): ViewModel() {

    private val _response = MutableStateFlow<Ticker?>(null)
    val response: StateFlow<Ticker?> = _response.asStateFlow()

    fun request() {
        viewModelScope.launch {
            delay(2000) // TODO :: automatically request process base on connection. maybe resolve at 0.0.3
            service.request(request = BinanceRequest())
        }
    }

    fun observeResponse() {
        service.observeTicker().onEach { result ->
            _response.update { result.data }
        }.launchIn(viewModelScope)
    }
}
~~~

---


## Copyright
~~~
Copyright (c) 2023, Jeremy, LLC
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of Match Group, LLC nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL MATCH GROUP, LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
~~~
