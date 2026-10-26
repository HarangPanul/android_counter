package com.harangpanul.counter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.harangpanul.counter.data.CounterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CounterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CounterRepository(application)

    // 플로팅 서비스도 같은 저장소를 갱신하므로 저장소를 단일 진실 공급원으로 구독한다.
    val count: StateFlow<Int> = repository.count
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun increment() {
        viewModelScope.launch { repository.increment() }
    }

    fun decrement() {
        viewModelScope.launch { repository.decrement() }
    }

    fun reset() {
        viewModelScope.launch { repository.reset() }
    }
}
