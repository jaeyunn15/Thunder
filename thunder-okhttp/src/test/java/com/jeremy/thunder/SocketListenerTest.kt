package com.jeremy.thunder

import com.jeremy.thunder.thunder_internal.event.WebSocketEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString.Companion.toByteString

private val socketListener = SocketListener()

internal class SocketListenerTest : BehaviorSpec({

    Given("WebSocket Initialized and Open") {
        val webSocket = mockk<WebSocket>()
        val response = mockk<Response>()
        val throwable = Throwable("This is test throwable")

        When("Emit OnMessage Event") {
            socketListener.onMessage(webSocket, "Hello World")

            Then("Receive OnMessage Event") {
                socketListener.collectEvent()
                    .first() shouldBe WebSocketEvent.OnMessageReceived("Hello World")
            }
        }

        When("Emit OnMessage Event As ByteString") {
            val msg = "Hello World".toByteArray().toByteString()
            socketListener.onMessage(webSocket, msg)

            Then("Receive OnMessage Event") {
                socketListener.collectEvent()
                    .first() shouldBe WebSocketEvent.OnMessageReceived("Hello World")
            }
        }

        When("Emit OnFailure Event") {
            socketListener.onFailure(webSocket, throwable, response)

            Then("Receive OnFailure Event") {
                socketListener.collectEvent()
                    .first() shouldBe WebSocketEvent.OnConnectionError("This is test throwable")
            }
        }

        When("Emit onClosed Event") {
            socketListener.onClosed(webSocket, 1001, "Some reason")

            Then("Receive onClosed Event") {
                socketListener.collectEvent().first() shouldBe WebSocketEvent.OnConnectionClosed
            }
        }
    }
})