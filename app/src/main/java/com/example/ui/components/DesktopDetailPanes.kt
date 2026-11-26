package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.HotelDateHelper
import com.example.data.Reservation
import com.example.data.ReservationStatus
import com.example.data.Room
import com.example.data.RoomStatus

fun Modifier.desktopPointerFeedback(
    interactionSource: MutableInteractionSource
): Modifier = composed {
    this
        .pointerHoverIcon(PointerIcon.Hand)
        .focusable(interactionSource = interactionSource)
}

@Composable
fun rememberDesktopCardAppearance(
    isSelected: Boolean,
    interactionSource: MutableInteractionSource
): Pair<Color, BorderStroke> {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    var pointerInside by remember { mutableStateOf(false) }
    val activeHover = isHovered || pointerInside

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            activeHover -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        },
        label = "desktopCardContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused || isSelected -> MaterialTheme.colorScheme.primary
            activeHover -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        label = "desktopCardBorder"
    )
    val borderWidth = if (isSelected || isFocused) 2.dp else 1.dp
    return containerColor to BorderStroke(borderWidth, borderColor)
}

fun Modifier.trackPointerHover(onHoverChanged: (Boolean) -> Unit): Modifier =
    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Enter -> onHoverChanged(true)
                    PointerEventType.Exit -> onHoverChanged(false)
                }
            }
        }
    }

@Composable
fun ReservationDetailPane(
    reservation: Reservation?,
    isBusy: Boolean,
    onCheckIn: (Reservation) -> Unit,
    onCheckOut: (Reservation) -> Unit,
    onEdit: (Reservation) -> Unit,
    onCancelReservation: (Reservation) -> Unit,
    onDeleteReservation: (Reservation) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxSize()
    ) {
        if (reservation == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Detalle de reserva",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Selecciona una reserva de la lista para gestionar el check-in, check-out, pagos o edición sin salir de esta vista.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val status = reservation.reservationStatus
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
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
                    Spacer(modifier = Modifier.width(8.dp))
                    ReservationStatusPill(status = status)
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SplitDetailRow("Fechas", "${HotelDateHelper.formatShortDate(reservation.checkInDate)} → ${HotelDateHelper.formatShortDate(reservation.checkOutDate)} (${reservation.nightsCount} noches)")
                    SplitDetailRow("Huéspedes", "${reservation.guestsCountInt} personas")
                    SplitDetailRow("Tarifa por noche", HotelDateHelper.formatCurrency(reservation.nightlyRate))
                    SplitDetailRow("Importe total", HotelDateHelper.formatCurrency(reservation.totalAmount))
                    SplitDetailRow("Abonado", HotelDateHelper.formatCurrency(reservation.paidAmount))
                    SplitDetailRow(
                        "Saldo pendiente",
                        HotelDateHelper.formatCurrency(reservation.balanceDue),
                        highlight = reservation.balanceDue > 0.01
                    )
                    HotelDateHelper.formatTimestampTime(reservation.actualCheckInAt)?.let {
                        SplitDetailRow("Check-in registrado", it)
                    }
                    HotelDateHelper.formatTimestampTime(reservation.actualCheckOutAt)?.let {
                        SplitDetailRow("Check-out registrado", it)
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
                            Text("Solicitudes especiales", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(reservation.specialRequests, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (status == ReservationStatus.CONFIRMED) {
                    Button(
                        onClick = { onCheckIn(reservation) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .pointerHoverIcon(PointerIcon.Hand)
                            .testTag("split_check_in_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrar Check-in")
                    }
                } else if (status == ReservationStatus.CHECKED_IN) {
                    Button(
                        onClick = { onCheckOut(reservation) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .pointerHoverIcon(PointerIcon.Hand)
                            .testTag("split_check_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Completar Check-out")
                    }
                }

                if (status == ReservationStatus.CONFIRMED || status == ReservationStatus.CHECKED_IN) {
                    OutlinedButton(
                        onClick = { onEdit(reservation) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .pointerHoverIcon(PointerIcon.Hand)
                            .testTag("split_edit_reservation_button")
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
                            onClick = { onCancelReservation(reservation) },
                            enabled = !isBusy,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancelar")
                        }
                    }
                    if (status != ReservationStatus.CHECKED_IN) {
                        TextButton(
                            onClick = { onDeleteReservation(reservation) },
                            enabled = !isBusy,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .pointerHoverIcon(PointerIcon.Hand)
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomDetailPane(
    room: Room?,
    activeReservation: Reservation?,
    isBusy: Boolean,
    onChangeStatus: (Room, RoomStatus) -> Unit,
    onEditRoom: (Room) -> Unit,
    onNewReservationForRoom: (Room) -> Unit,
    onOpenReservation: (Reservation) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxSize()
    ) {
        if (room == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Inspector de habitación",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Selecciona una habitación para cambiar su estado operativo en un clic, ver al huésped actual o editar sus tarifas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Habitación ${room.number}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "${room.roomType.labelEs} · Piso ${room.floorInt}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RoomStatusPill(status = room.roomStatus)
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SplitDetailRow("Tarifa por noche", HotelDateHelper.formatCurrency(room.nightlyRate))
                    SplitDetailRow("Capacidad máxima", "${room.capacityInt} huéspedes")
                    if (!room.notes.isNullOrBlank()) {
                        SplitDetailRow("Amenidades", room.notes)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Cambio rápido de estado",
                        style = MaterialTheme.typography.titleSmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RoomStatus.entries.forEach { target ->
                            FilterChip(
                                selected = room.roomStatus == target,
                                onClick = {
                                    if (room.roomStatus != target && !isBusy) {
                                        onChangeStatus(room, target)
                                    }
                                },
                                label = { Text(target.labelEs) },
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            )
                        }
                    }
                }

                if (activeReservation != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Estancia vinculada",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = activeReservation.guestName,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${HotelDateHelper.formatShortDate(activeReservation.checkInDate)} → ${HotelDateHelper.formatShortDate(activeReservation.checkOutDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { onOpenReservation(activeReservation) },
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Text("Abrir detalle de estancia")
                            }
                        }
                    }
                }

                if (room.roomStatus == RoomStatus.AVAILABLE) {
                    Button(
                        onClick = { onNewReservationForRoom(room) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .pointerHoverIcon(PointerIcon.Hand)
                            .testTag("split_book_room_button")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reservar o Check-in aquí")
                    }
                }

                OutlinedButton(
                    onClick = { onEditRoom(room) },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .pointerHoverIcon(PointerIcon.Hand)
                        .testTag("split_edit_room_button")
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar habitación")
                }
            }
        }
    }
}

@Composable
fun KeyboardShortcutsCard(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Keyboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Atajos de teclado",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            ShortcutRow("Ctrl + 1 / 2 / 3", "Cambiar entre Hoy, Reservas y Habitaciones")
            ShortcutRow("Ctrl + N", "Nueva reserva (o habitación en pestaña Habitaciones)")
            ShortcutRow("Ctrl + F", "Buscar reservas")
            ShortcutRow("Esc", "Cerrar panel o limpiar selección")
        }
    }
}

@Composable
private fun ShortcutRow(keys: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = keys,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SplitDetailRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
        )
    }
}
