# Thunder
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.jaeyunn15/thunder/badge.svg?style=plastic)](https://central.sonatype.com/artifact/io.github.jaeyunn15/thunder)

A WebSocket library that draws heavily from the [Scarlet](https://github.com/Tinder/Scarlet) by Tinder.     
Currently, support only for Coroutine Flow.     

---
## Production Story
- [Thunder: WebSocket have come through the clouds.](https://medium.com/proandroiddev/%EF%B8%8Fthunder-websocket-have-come-through-the-clouds-4db1a9b31ffa)

---
## Example project usage.
- [CryptoApp](https://github.com/jaeyunn15/CryptoApp) : You can see Thunder embedded in a real-time data architecture.

---
## Basic Feature
- WebSocket connection using OkHttp3.
- Regardless of the state of your app, if it's alive, it will automatically manage to stay connected. 
- Provide **retry and reconnect** handling based on the application's network and socket state.
- Provide the ability to **automatically recover requests** via the last request cache.
- Provides a websocket connection based on the STOMP. (December, 2023)

### Converter 
- Gson
- KotlinX-Serialization **(Default)**       

---
## Download First
Thunder is available via Maven Central.

First, go to your settings.gradle file and add the code below.
~~~ groovy
[settings.gradle]

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() <-- here! you have to copy & paste
    }
}
~~~

Use the library by adding a Dependency to the module you want to use.    
Check Latest Version here : [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.jaeyunn15/thunder/badge.svg?style=plastic)](https://central.sonatype.com/artifact/io.github.jaeyunn15/thunder)
~~~ groovy
Gradle
dependencies {
    implementation 'io.github.jaeyunn15:thunder:1.0.2' // must required
    implementation 'io.github.jaeyunn15:thunder-okhttp:1.0.2' // must required
}
~~~

~~~ toml
libs.versions
thunder = { group = "io.github.jaeyunn15", name = "thunder", version = "1.0.2" }
thunderOkhttp = { group = "io.github.jaeyunn15", name = "thunder-okhttp", version = "1.0.2" }
~~~

---
## Restriction 🚨
The minimum SDK version provided by Thunder is 24.



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
    .setConverterType(ConverterType.Gson)
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
            .pingInterval(
                10,
                TimeUnit.SECONDS
            ) // If there are no events for a minute, we need to put in some code for ping pong to output a socket connection error from okhttp.
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
            .setConverterType(ConverterType.Gson)
            .build()
            .create()
    }

    // or you can like this. (Kotlin Type-Safe Builder)
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
MIT License

Copyright (c) 2023 jaeyun

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

~~~
