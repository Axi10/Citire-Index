package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.stats.ConsumptionInsights
import com.example.ui.theme.CostGreen
import java.util.Locale

/**
 * Three figures that answer "how am I doing?": the usual consumption, whether the last reading
 * was higher or lower than the one before, and what the last 12 months cost.
 */
@Composable
fun InsightsCard(
    insights: ConsumptionInsights,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val romanian = Locale("ro", "RO")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Tendințe consum",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (insights.readingsCounted == 0) {
                Text(
                    text = "După a doua citire vezi aici media, evoluția față de citirea anterioară și costul pe 12 luni.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(
                        label = "Medie / citire",
                        value = "${String.format(romanian, "%.1f", insights.averageConsumption)} $unit",
                        modifier = Modifier.weight(1f)
                    )

                    val change = insights.changePercent
                    val rising = change != null && change > 5.0
                    val falling = change != null && change < -5.0
                    // More consumption is the unwelcome direction, so it is the warm colour
                    val changeColor = when {
                        rising -> Color(0xFFE65100)
                        falling -> CostGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val icon = when {
                        rising -> Icons.Default.TrendingUp
                        falling -> Icons.Default.TrendingDown
                        else -> Icons.Default.TrendingFlat
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Față de anterioară",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = changeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (change != null) {
                                    val sign = if (change > 0) "+" else ""
                                    "$sign${String.format(romanian, "%.0f", change)}%"
                                } else {
                                    "-"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = changeColor
                            )
                        }
                    }

                    Stat(
                        label = "Cost 12 luni",
                        value = "${String.format(romanian, "%.0f", insights.costLast12Months)} lei",
                        valueColor = CostGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                val last = insights.lastConsumption
                if (last != null) {
                    Text(
                        text = "Ultima citire: ${String.format(romanian, "%.1f", last)} $unit · " +
                            "${insights.readingsCounted} citiri cu consum în medie",
                        fontSize = 11.sp,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
