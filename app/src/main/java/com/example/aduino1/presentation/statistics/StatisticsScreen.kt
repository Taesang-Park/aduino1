package com.example.aduino1.presentation.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aduino1.domain.model.HydrationInterval
import com.example.aduino1.domain.model.LedColorCommand
import java.text.SimpleDateFormat
import java.util.*

/**
 * Statistics Screen
 * 시간별 섭취량 통계 및 구간별 달성률 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val hourlyIntake by viewModel.hourlyIntake.collectAsState()
    val intervalStats by viewModel.intervalStats.collectAsState()
    val totalIntake by viewModel.totalIntake.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("통계") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 날짜 선택기
            DateSelector(
                selectedDate = selectedDate,
                onPreviousDay = { viewModel.selectPreviousDay() },
                onNextDay = { viewModel.selectNextDay() },
                onToday = { viewModel.selectToday() }
            )

            Divider()

            // 전체 요약
            OverallSummaryCard(
                totalIntake = totalIntake,
                dailyGoal = settings.dailyGoal,
                achievementRate = viewModel.getOverallAchievementRate(),
                modifier = Modifier.padding(16.dp)
            )

            // 시간별 섭취량 차트
            Text(
                text = "시간별 섭취량",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HourlyIntakeChart(
                hourlyIntake = hourlyIntake,
                maxValue = viewModel.getMaxHourlyIntake(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(16.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // 구간별 달성률
            Text(
                text = "구간별 달성률",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (intervalStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "구간 정보가 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height((160 * ((intervalStats.size + 1) / 2)).dp)
                ) {
                    items(intervalStats) { interval ->
                        IntervalCard(interval = interval)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 날짜 선택기
 */
@Composable
private fun DateSelector(
    selectedDate: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN)
    val displayDate = try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
        date?.let { dateFormat.format(it) } ?: selectedDate
    } catch (e: Exception) {
        selectedDate
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "이전 날")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayDate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onToday) {
                Text("오늘")
            }
        }

        IconButton(onClick = onNextDay) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "다음 날")
        }
    }
}

/**
 * 전체 요약 카드
 */
@Composable
private fun OverallSummaryCard(
    totalIntake: Int,
    dailyGoal: Int,
    achievementRate: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "오늘의 요약",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LinearProgressIndicator(
                progress = { (achievementRate / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "총 섭취량",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${totalIntake}ml",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                VerticalDivider(modifier = Modifier.height(50.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "달성률",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "%.1f%%".format(achievementRate),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 시간별 섭취량 막대 차트
 */
@Composable
private fun HourlyIntakeChart(
    hourlyIntake: Map<Int, Int>,
    maxValue: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        // 차트
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / 24f

            // Y축 눈금선
            for (i in 0..4) {
                val y = canvasHeight - (canvasHeight * i / 4f)
                drawLine(
                    color = surfaceVariant,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
            }

            // 막대 그리기
            (0..23).forEach { hour ->
                val value = hourlyIntake[hour] ?: 0
                val barHeight = if (maxValue > 0) {
                    (value.toFloat() / maxValue.toFloat()) * canvasHeight
                } else {
                    0f
                }

                val x = hour * barWidth
                val y = canvasHeight - barHeight

                // 막대
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(x + barWidth * 0.1f, y),
                    size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, barHeight)
                )
            }
        }

        // X축 레이블
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0, 6, 12, 18, 24).forEach { hour ->
                Text(
                    text = "${hour}시",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 구간 카드
 */
@Composable
private fun IntervalCard(interval: HydrationInterval) {
    val ledColor = when (interval.ledColor) {
        LedColorCommand.RED -> Color.Red
        LedColorCommand.YELLOW -> Color(0xFFFFA500)
        LedColorCommand.BLUE -> Color.Blue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = ledColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${interval.intervalNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = ledColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = interval.ledColor.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { interval.achievementRate.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = ledColor
                )

                Text(
                    text = "${interval.currentAmount} / ${interval.goalAmount}ml",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "%.1f%%".format(interval.achievementPercent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ledColor
                )
            }
        }
    }
}
