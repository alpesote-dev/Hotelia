package com.example.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.HotelDateHelper
import com.example.data.Reservation
import com.example.data.ReservationStatus
import com.example.ui.components.ReservationStatusPill

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckInSheet(
    reservation: Reservation,
    isBusy: Boolean,
    onConfirmCheckIn: (additionalPayment: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val balanceDue = reservation.balanceDue
    var paymentText by remember(reservation.id) {
        mutableStateOf(if (balanceDue > 0.01) "%.2f".format(java.util.Locale.US, balanceDue) else "0")
    }
    val parsedPayment = paymentText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Confirmar Check-in",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = reservation.guestName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Habitación ${reservation.roomNumber} (${reservation.roomTypeEnum.labelEs}) · ${reservation.nightsCount} noches",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${HotelDateHelper.formatShortDate(reservation.checkInDate)} → ${HotelDateHelper.formatShortDate(reservation.checkOutDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!reservation.guestDocument.isNullOrBlank()) {
                        Text(
                            text = "Documento: ${reservation.guestDocument}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total estancia", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(HotelDateHelper.formatCurrency(reservation.totalAmount), style = MaterialTheme.typography.titleSmall)
                }
                Column {
                    Text("Abonado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(HotelDateHelper.formatCurrency(reservation.paidAmount), style = MaterialTheme.typography.titleSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pendiente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        HotelDateHelper.formatCurrency(balanceDue),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (balanceDue > 0.01) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (balanceDue > 0.01) {
                Text(
                    text = "Cobro en recepción",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = parsedPayment == 0.0,
                        onClick = { paymentText = "0" },
                        label = { Text("Sin cobro ahora") }
                    )
                    FilterChip(
                        selected = kotlin.math.abs(parsedPayment - balanceDue) < 0.01,
                        onClick = { paymentText = "%.2f".format(java.util.Locale.US, balanceDue) },
                        label = { Text("Saldar todo (${HotelDateHelper.formatCurrency(balanceDue)})") }
                    )
                }
                OutlinedTextField(
                    value = paymentText,
                    onValueChange = { paymentText = it },
                    label = { Text("Importe a cobrar ahora") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("check_in_payment_input")
                )
            }

            Button(
                onClick = { onConfirmCheckIn(parsedPayment) },
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_check_in_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Entregar llave y hacer Check-in")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckOutSheet(
    reservation: Reservation,
    isBusy: Boolean,
    onConfirmCheckOut: (finalPayment: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val balanceDue = reservation.balanceDue
    var paymentText by remember(reservation.id) {
        mutableStateOf(if (balanceDue > 0.01) "%.2f".format(java.util.Locale.US, balanceDue) else "0")
    }
    val parsedPayment = paymentText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Liquidar y hacer Check-out",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = reservation.guestName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Habitación ${reservation.roomNumber} · ${reservation.nightsCount} noches",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total: ${HotelDateHelper.formatCurrency(reservation.totalAmount)}", style = MaterialTheme.typography.bodySmall)
                        Text("Abonado: ${HotelDateHelper.formatCurrency(reservation.paidAmount)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = if (balanceDue > 0.01) {
                            "Saldo pendiente: ${HotelDateHelper.formatCurrency(balanceDue)}"
                        } else {
                            "Cuenta totalmente saldada"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = if (balanceDue > 0.01) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (balanceDue > 0.01) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kotlin.math.abs(parsedPayment - balanceDue) < 0.01,
                        onClick = { paymentText = "%.2f".format(java.util.Locale.US, balanceDue) },
                        label = { Text("Saldar ${HotelDateHelper.formatCurrency(balanceDue)}") }
                    )
                    FilterChip(
                        selected = parsedPayment == 0.0,
                        onClick = { paymentText = "0" },
                        label = { Text("Cerrar sin cobro") }
                    )
                }
                OutlinedTextField(
                    value = paymentText,
                    onValueChange = { paymentText = it },
                    label = { Text("Cobro final") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("check_out_payment_input")
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "La habitación ${reservation.roomNumber} pasará automáticamente a Limpieza.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Button(
                onClick = { onConfirmCheckOut(parsedPayment) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_check_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Completar Check-out")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationDetailSheet(
    reservation: Reservation,
    isBusy: Boolean,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onEdit: () -> Unit,
    onCancelReservation: () -> Unit,
    onDeleteReservation: () -> Unit,
    onDismiss: () -> Unit
) {
    val status = reservation.reservationStatus
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reservation.guestName,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Habitación ${reservation.roomNumber} · ${reservation.roomTypeEnum.labelEs}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ReservationStatusPill(status = status)
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Fechas", "${HotelDateHelper.formatShortDate(reservation.checkInDate)} → ${HotelDateHelper.formatShortDate(reservation.checkOutDate)} (${reservation.nightsCount} noches)")
                DetailRow("Huéspedes", "${reservation.guestsCountInt} personas")
                DetailRow("Tarifa", "${HotelDateHelper.formatCurrency(reservation.nightlyRate)} / noche")
                DetailRow("Total", HotelDateHelper.formatCurrency(reservation.totalAmount))
                DetailRow("Abonado", HotelDateHelper.formatCurrency(reservation.paidAmount))
                DetailRow(
                    "Saldo pendiente",
                    HotelDateHelper.formatCurrency(reservation.balanceDue),
                    highlight = reservation.balanceDue > 0.01
                )
                HotelDateHelper.formatTimestampTime(reservation.actualCheckInAt)?.let {
                    DetailRow("Hora de check-in", it)
                }
                HotelDateHelper.formatTimestampTime(reservation.actualCheckOutAt)?.let {
                    DetailRow("Hora de check-out", it)
                }
            }

            if (!reservation.guestDocument.isNullOrBlank() || !reservation.guestPhone.isNullOrBlank() || !reservation.guestEmail.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        reservation.guestDocument?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        reservation.guestPhone?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        reservation.guestEmail?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (!reservation.specialRequests.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Notas y solicitudes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(reservation.specialRequests, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (status == ReservationStatus.CONFIRMED) {
                Button(
                    onClick = onCheckIn,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("detail_check_in_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registrar Check-in")
                }
            } else if (status == ReservationStatus.CHECKED_IN) {
                Button(
                    onClick = onCheckOut,
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("detail_check_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Completar Check-out")
                }
            }

            if (status == ReservationStatus.CONFIRMED || status == ReservationStatus.CHECKED_IN) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("detail_edit_reservation_button")
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar reserva")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (status == ReservationStatus.CONFIRMED) {
                    TextButton(
                        onClick = onCancelReservation,
                        enabled = !isBusy,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("detail_cancel_reservation_button")
                    ) {
                        Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancelar reserva")
                    }
                }
                if (status != ReservationStatus.CHECKED_IN) {
                    TextButton(
                        onClick = onDeleteReservation,
                        enabled = !isBusy,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("detail_delete_reservation_button")
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
        )
    }
}
