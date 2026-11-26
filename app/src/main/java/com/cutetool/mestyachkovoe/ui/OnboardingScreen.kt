package com.cutetool.mestyachkovoe.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cutetool.mestyachkovoe.data.DistrictStatus
import com.cutetool.mestyachkovoe.data.FederalDistrict
import com.cutetool.mestyachkovoe.ui.theme.ExpressiveShapes

private val WelcomeShape = ExpressiveShapes.hero

@Composable
fun OnboardingScreen(
    initialDistrict: FederalDistrict,
    onContinue: (FederalDistrict) -> Unit
) {
    var selected by remember {
        mutableStateOf(if (initialDistrict.selectable) initialDistrict else FederalDistrict.URAL)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = ExpressiveShapes.tile,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Местячковое",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Региональные новости России",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Откуда будем собирать новости?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Выберите федеральный округ — лента и источники настроятся под него. Позже округ можно изменить в настройках.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FederalDistrict.entries.forEach { district ->
                    DistrictOptionRow(
                        district = district,
                        selected = district == selected,
                        onSelect = { selected = district }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onContinue(selected) },
                shape = ExpressiveShapes.button,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Продолжить с «${selected.title}»")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Это можно изменить в любой момент в настройках.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun DistrictPickerDialog(
    current: FederalDistrict,
    onSelect: (FederalDistrict) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ExpressiveShapes.hero,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Федеральный округ",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "От него зависят источники и фильтры ленты",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FederalDistrict.entries.forEach { district ->
                        DistrictOptionRow(
                            district = district,
                            selected = district == current,
                            onSelect = {
                                onSelect(district)
                                onDismiss()
                            }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Composable
fun UpdateIntroDialog(
    onKeepUral: () -> Unit,
    onChooseDistrict: () -> Unit
) {
    Dialog(onDismissRequest = onKeepUral) {
        Surface(
            shape = ExpressiveShapes.hero,
            tonalElevation = 6.dp
        ) {
            Column(
                Modifier
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        "Что нового",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    "Теперь вся Россия",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Местячковое стало мультирегиональным: лента собирает новости выбранного федерального округа. Для вас по умолчанию оставлен Урал.",
                    style = MaterialTheme.typography.bodyMedium
                )
                HighlightRow("Новые регионы", "Центральная Россия, Приволжье, Сибирь, Подмосковье, Юг России, Северный Кавказ, СЗФО и Дальний Восток")
                HighlightRow("Поиск", "Ищите новости по всей России по кнопке-лупе")
                HighlightRow("Где изменить", "Настройки → ФедОкруг ленты")
                Spacer(Modifier.height(4.dp))
                Button(onClick = onKeepUral, shape = ExpressiveShapes.button, modifier = Modifier.fillMaxWidth()) {
                    Text("Оставить Урал")
                }
                OutlinedButton(onClick = onChooseDistrict, shape = ExpressiveShapes.button, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Выбрать другой округ")
                }
            }
        }
    }
}

@Composable
private fun HighlightRow(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun DistrictOptionRow(
    district: FederalDistrict,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val enabled = district.selectable
    val container = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val content = when {
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Surface(
        onClick = onSelect,
        enabled = enabled,
        shape = ExpressiveShapes.tile,
        color = container,
        contentColor = content,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    district.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
                Text(
                    when {
                        district == FederalDistrict.URAL -> "Доступно по умолчанию"
                        district == FederalDistrict.MOSCOW_OBLAST -> "Отдельно от Центральной России"
                        district.regions.isNotEmpty() -> "${district.regions.size} регионов"
                        else -> "Скоро здесь появятся источники"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.72f)
                )
            }
            if (district.status != DistrictStatus.AVAILABLE) {
                StatusPill(
                    text = district.status.label,
                    active = enabled,
                    color = content
                )
                Spacer(Modifier.width(8.dp))
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Выбрано",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, active: Boolean, color: Color) {
    Surface(
        shape = CircleShape,
        color = if (active) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.onTertiaryContainer else color.copy(alpha = 0.8f)
        )
    }
}
