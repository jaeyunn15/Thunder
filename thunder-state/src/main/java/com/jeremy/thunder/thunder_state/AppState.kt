package com.jeremy.thunder.thunder_state

sealed interface AppState

object Active: AppState

object Inactive: AppState