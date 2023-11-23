package com.jeremy.thunder.stomp.model

enum class Command {
    CONNECT,
    SEND,
    SUBSCRIBE,
    UNSUBSCRIBE,
    BEGIN,
    COMMIT,
    ABORT,
    ACK,
    NACK,
    DISCONNECT,
}