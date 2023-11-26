package com.jeremy.thunder

import com.jeremy.thunder.thunder_internal.event.WebSocketEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import okhttp3.WebSocket

private val webSocket = mockk<WebSocket>()
private val eventFlow = MutableSharedFlow<WebSocketEvent>(replay = 1, extraBufferCapacity = 100)
private val webSocketListener = mockk<SocketListener>() {
    every { collectEvent() } returns eventFlow
}
private val provider = mockk<ConnectionProvider>() {
    justRun { provide(webSocketListener) }
}
private val webSocketHandler = mockk<SocketHandler>() {
    justRun { initWebSocket(webSocket) }
    every { send(any()) } returns true
    every { close(any(), any()) } returns true
}

private val okHttpWebSocket = OkHttpWebSocket(
    provider = provider,
    socketListener = webSocketListener,
    socketHandler = webSocketHandler,
)


internal class OkHttpWebSocketTest : BehaviorSpec({
    Given("WebSocket Open Event Emit") {
        val events = arrayOf(spyk<WebSocketEvent>(WebSocketEvent.OnConnectionOpen(webSocket)))
        events.forEach { eventFlow.tryEmit(it) }

        When("WebSocket Open") {
            val resultFlow = okHttpWebSocket.open()

            Then("Receive WebSocket Connection Open Event") {
                resultFlow.first() shouldBe WebSocketEvent.OnConnectionOpen(webSocket)
            }
        }

    }

    Given("Hello Message String Given") {
        val msg = "Hello"

        When("WebSocket Send Message") {
            val resultFlow = okHttpWebSocket.send(msg)

            Then("Receive WebSocket Connection Open Event") {
                resultFlow shouldBe true
                webSocketHandler.should {
                    it.send(msg)
                }
            }
        }
    }

    Given("WebSocket Close Code & Reason") {
        val closeCode = 1001
        val closeReason = "SomethingWrong"

        When("WebSocket Close") {
            val result = okHttpWebSocket.close(closeCode, closeReason)

            Then("Receive Close Connection Event") {
                result shouldBe true
                webSocketHandler.should {
                    it.close(closeCode, closeReason)
                }
            }
        }
    }
})