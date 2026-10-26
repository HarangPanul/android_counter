package com.harangpanul.counter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
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
                val floatingActive by FloatingCounterService.running.collectAsState()
                CounterScreen(
                    count = count,
                    onIncrement = viewModel::increment,
                    onDecrement = viewModel::decrement,
                    onReset = viewModel::reset,
                    floatingActive = floatingActive,
                    onFloating = {
                        if (floatingActive) FloatingCounterService.stop(this) else startFloating()
                    },
                )
            }
        }
    }

    private fun startFloating() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.floating_permission_needed, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
            )
            return
        }
        FloatingCounterService.start(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // 알림 권한 팝업이 보이도록 앱을 닫지 않는다. 거부해도 플로팅은 동작한다.
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            return
        }
        moveTaskToBack(true)
    }
}
