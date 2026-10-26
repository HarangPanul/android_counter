package com.harangpanul.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harangpanul.counter.R

/** 화면 전체가 +1 버튼이고, 하단의 동그란 버튼 세 개가 각각 -1, 초기화, 플로팅 모드 진입 버튼이다. */
@Composable
fun CounterScreen(
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    floatingActive: Boolean,
    onFloating: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.count_description)
    val floatingDescription = stringResource(R.string.floating_mode)
    val decrementDescription = stringResource(R.string.decrement)
    val context = LocalContext.current
    val vibrations = remember(context) { Vibrations(context) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    vibrations.increment()
                    onIncrement()
                },
            ),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = count.toString(),
                fontSize = 120.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics { contentDescription = "$description $count" },
            )
            // 자식 clickable이 터치를 소비하므로 버튼을 눌러도 +1이 함께 실행되지 않는다.
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
            ) {
                FilledIconButton(
                    onClick = {
                        vibrations.increment()
                        onDecrement()
                    },
                    modifier = Modifier.size(64.dp),
                ) {
                    // Icons.Filled.Remove는 material-icons-extended에만 있어 텍스트로 그린다.
                    Text(
                        text = "−",
                        fontSize = 32.sp,
                        modifier = Modifier.semantics {
                            contentDescription = decrementDescription
                        },
                    )
                }
                FilledIconButton(
                    onClick = {
                        vibrations.reset()
                        onReset()
                    },
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.reset),
                    )
                }
                FilledIconButton(
                    onClick = onFloating,
                    modifier = Modifier.size(64.dp),
                ) {
                    // 오버레이 알약 모양을 본뜬 아이콘. 기본 아이콘 세트에 마땅한 것이 없어 직접 그린다.
                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 16.dp)
                            // 플로팅 실행 중이면 알약을 채워서, 다시 누르면 꺼진다는 것을 보여준다.
                            .background(
                                if (floatingActive) LocalContentColor.current else Color.Transparent,
                                CircleShape,
                            )
                            .border(2.dp, LocalContentColor.current, CircleShape)
                            .semantics {
                                contentDescription = floatingDescription
                            },
                    )
                }
            }
        }
    }
}
