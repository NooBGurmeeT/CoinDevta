package com.gurmeet.coindevta.presentation.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    symbol: String,
    uiState: ChartUiState,
    livePrice: Double,
    onBack: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = symbol.replace("USDT", ""),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            // Price Header Card
            PriceHeaderCard(symbol, livePrice)

            Spacer(modifier = Modifier.height(20.dp))

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                uiState.error != null -> {
                    Text(
                        text = uiState.error,
                        color = Color.Red
                    )
                }

                else -> {
                    ChartCard(uiState.chartPrices)
                }
            }
        }
    }
}

@Composable
fun PriceHeaderCard(symbol: String, price: Double) {

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = symbol.replace("USDT", ""),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "₹ $price",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2ECC71)
            )
        }
    }
}

@Composable
fun ChartCard(prices: List<Double>) {

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        Box(
            modifier = Modifier
                .height(300.dp)
                .padding(16.dp)
        ) {
            LineChart(prices)
        }
    }
}

@Composable
fun LineChart(prices: List<Double>) {

    if (prices.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {

        val maxPrice = prices.maxOrNull() ?: return@Canvas
        val minPrice = prices.minOrNull() ?: return@Canvas
        val priceRange = maxPrice - minPrice

        val stepX = size.width / (prices.size - 1)

        val path = Path()

        prices.forEachIndexed { index, price ->

            val x = index * stepX
            val y = size.height - ((price - minPrice) / priceRange) * size.height

            if (index == 0) {
                path.moveTo(x, y.toFloat())
            } else {
                path.lineTo(x, y.toFloat())
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF2ECC71),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}
