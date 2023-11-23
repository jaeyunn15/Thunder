package com.jeremy.thunder.stomp.compiler

import java.io.StringReader
import java.util.Scanner
import java.util.regex.Pattern

object MessageCompiler {
    private val PATTERN_HEADER = Pattern.compile("([^:\\s]+)\\s*:\\s*([^:\\s]+)")
    const val TERMINATE_MESSAGE_SYMBOL = "\u0000"

    fun compileMessage(message: ThunderRequest): String = buildString {
        append("${message.command}\n")
        append(message.header.extract())
        append("\n")
        append("${message.payload}\n\n")
        append(TERMINATE_MESSAGE_SYMBOL)
    }

    fun parseMessage(data: String?): ThunderResponse {
        if (data.isNullOrBlank()) return UnitResponse

        return runCatching {
            val scanner = Scanner(StringReader(data)).apply {
                useDelimiter("\\n")
            }

            val command = scanner.next()
            val headers = hashMapOf<String, String>()

            while (scanner.hasNext(PATTERN_HEADER)) {
                val matcher = PATTERN_HEADER.matcher(scanner.next())
                matcher.find()
                headers[matcher.group(1)] = matcher.group(2)
            }

            scanner.skip("\\s")

            val payload = scanner.useDelimiter(TERMINATE_MESSAGE_SYMBOL).let {
                if (it.hasNext()) it.next() else null
            }

            if (command == ResponseCommandType.RECEIPT.name) {
                ReceiptResponse(headers)
            } else {
                MessageResponse(headers, payload)
            }
        }.getOrElse {
            ErrorResponse(it.message)
        }
    }
}