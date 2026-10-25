package com.harangpanul.counter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.harangpanul.counter.ui.CounterScreen
import com.harangpanul.counter.ui.theme.CounterTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CounterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CounterTheme {
                val count by viewModel.count.collectAsState()
                CounterScreen(
                    count = count,
                    onIncrement = viewModel::increment,
                    onReset = viewModel::reset,
                )
            }
        }
    }
}
