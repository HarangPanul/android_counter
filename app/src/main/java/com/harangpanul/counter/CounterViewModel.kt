package com.harangpanul.counter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.harangpanul.counter.data.CounterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CounterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CounterRepository(application)

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    init {
        viewModelScope.launch {
            _count.value = repository.count.first()
        }
    }

    fun increment() = update(_count.value + 1)

    fun decrement() = update((_count.value - 1).coerceAtLeast(0))

    fun reset() = update(0)

    private fun update(value: Int) {
        _count.value = value
        viewModelScope.launch { repository.setCount(value) }
    }
}
