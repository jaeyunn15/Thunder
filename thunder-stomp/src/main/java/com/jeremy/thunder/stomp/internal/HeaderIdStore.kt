package com.jeremy.thunder.stomp.internal

import java.util.LinkedList
import java.util.Queue

/**
 * It is designed to sequentially apply multiple requests to a specific destination.
 * */

class HeaderIdStore {
    private val _store: HashMap<String, Queue<String>> = hashMapOf()

    fun put(key: String, id: String) {
        if (_store.containsKey(key) && !_store[key].isNullOrEmpty()) {
            _store[key]?.offer(id)
        } else {
            _store[key] = LinkedList<String>().apply { offer(id) }
        }
    }

    operator fun get(key: String): String {
        return if (_store.containsKey(key) && !_store[key].isNullOrEmpty()) {
            _store[key]?.poll() ?: ""
        } else ""
    }

    fun clear() {
        _store.clear()
    }
}