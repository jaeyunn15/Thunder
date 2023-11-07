package com.jeremy.thunder

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import okhttp3.WebSocket
import okio.ByteString
class SocketHandlerTest: BehaviorSpec({
    val webSocket = mockk<WebSocket> {
        every { send(any<String>()) } returns true
        every { send(any<ByteString>()) } returns true
        every { close(any(), any()) } returns true
    }

    val socketHandler = SocketHandler()

    Given("WebSocket Not Initialized") {

        When("WebSocket send message") {
            val result = socketHandler.send("Hello")

            Then("return fail result") {
                result shouldBe false
            }
        }
    }

    Given("WebSocket initialized") {
        socketHandler.initWebSocket(webSocket)

        When("WebSocket send message") {
            val isSuccess = socketHandler.send("Hello")

            Then("return success result") {
                isSuccess shouldBe true
            }
        }
    }

})