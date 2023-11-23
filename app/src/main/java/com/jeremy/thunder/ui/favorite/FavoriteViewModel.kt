package com.jeremy.thunder.ui.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
) : ViewModel() {

    fun requestSpecificTicker() {
        viewModelScope.launch {

        }
    }

    fun requestCancelSpecificTicker() {

    }

    fun observeSpecificTicker() {
    }
}