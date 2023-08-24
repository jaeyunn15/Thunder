# Thunder ![Generic badge](https://img.shields.io/badge/version-0.0.10-green.svg)

A WebSocket library that draws heavily from the [Scarlet](https://github.com/Tinder/Scarlet) by Tinder.     
The overall design of this library is very similar to Scarlet.     
Currently, support only for Coroutine Flow.     

---
## Feature
- WebSocket connection using OkHttp3.
- Provide retry and reconnect handling based on the application's network and socket state. (I called it valve cache, recovery cache)
- Provide the ability to automatically recover requests via the last request cache.
- (TBD) Provides a websocket connection based on the Stomp Message Protocol.

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
    implementation 'com.github.jaeyunn15:Thunder:0.0.10
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

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
~~~
