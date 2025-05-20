package com.example.vareshki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuScreen(
    onCanteensClick: () -> Unit,
    onEmployeesClick: () -> Unit,
    onStatusesClick: () -> Unit,
    onProductsClick: () -> Unit,
    onOrderSelectionClick: () -> Unit,
    onShowProfile: () -> Unit,
    onSelectOrdersForInvoiceClick: () -> Unit,
    onViewInvoicesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "АДМИНИСТРИРОВАНИЕ СТОЛОВЫХ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),

                )
                IconButton(
                    onClick = { onShowProfile() }
                ) {
                    Icon(
                        Icons.Default.Person,
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Редактировать профиль",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            MenuItem(text = "история статусов заказа", onClick = onOrderSelectionClick)
            MenuItem(text = "столовые", onClick = onCanteensClick)
            MenuItem(text = "работники", onClick = onEmployeesClick)
            MenuItem(text = "статусы", onClick = onStatusesClick)
            MenuItem(text = "продукты", onClick = onProductsClick)
            MenuItem(text = "формирование накладных", onClick = onSelectOrdersForInvoiceClick)
            MenuItem(text = "просмотр накладных", onClick = onViewInvoicesClick)
        }


    }
}

@Composable
fun MenuItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp).padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.alignByBaseline()
        )
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}