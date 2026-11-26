package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.Reservation
import com.example.data.Room
import com.example.data.RoomStatus
import com.example.ui.HotelDashboardData
import com.example.ui.components.EmptyHotelState
import com.example.ui.components.OccupancyOverviewCard
import com.example.ui.components.ReservationActionCard
import com.example.ui.components.ReservationCardMode
import com.example.ui.components.ReservationDetailPane

private enum class DashboardQueue(val label: String) {
    CHECK_INS("Llegadas"),
    CHECK_OUTS("Salidas"),
    IN_HOUSE("En casa")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboard: HotelDashboardData,
    isBusy: Boolean,
    onSeedStarterHotel: () -> Unit,
    onAddFirstRoom: () -> Unit,
    onReservationClick: (Reservation) -> Unit,
    onCheckInClick: (Reservation) -> Unit,
    onCheckOutClick: (Reservation) -> Unit,
    onEditReservation: (Reservation) -> Unit,
    onCancelReservation: (Reservation) -> Unit,
    onDeleteReservation: (Reservation) -> Unit,
    onMarkRoomAvailable: (Room) -> Unit,
    onOpenRoomsInventory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedQueue by rememberSaveable { mutableStateOf(DashboardQueue.CHECK_INS) }
    var selectedReservationId by rememberSaveable { mutableStateOf<String?>(null) }
    val cleaningRooms = dashboard.rooms.filter { it.roomStatus == RoomStatus.CLEANING }

    val activeList = when (selectedQueue) {
        DashboardQueue.CHECK_INS -> dashboard.pendingCheckInsToday
        DashboardQueue.CHECK_OUTS -> dashboard.pendingCheckOutsToday
        DashboardQueue.IN_HOUSE -> dashboard.inHouseStays
    }

    val cardMode = when (selectedQueue) {
        DashboardQueue.CHECK_INS -> ReservationCardMode.CHECK_IN_QUEUE
        DashboardQueue.CHECK_OUTS -> ReservationCardMode.CHECK_OUT_QUEUE
        DashboardQueue.IN_HOUSE -> ReservationCardMode.GENERAL
    }

    val selectedReservation = activeList.firstOrNull { it.id == selectedReservationId }
        ?: activeList.firstOrNull()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isSplitView = maxWidth >= 840.dp && dashboard.rooms.isNotEmpty()

        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(if (isSplitView) 1.12f else 1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OccupancyOverviewCard(dashboard = dashboard)
                }

                if (dashboard.rooms.isEmpty()) {
                    item {
                        EmptyHotelState(
                            title = "Configura tu propiedad",
                            subtitle = "Carga un hotel boutique de ejemplo con 8 habitaciones y reservas listas para probar check-in y check-out, o agrega tu primera habitación manualmente.",
                            primaryButtonText = "Cargar hotel de inicio",
                            onPrimaryClick = onSeedStarterHotel,
                            secondaryButtonText = "Agregar primera habitación",
                            onSecondaryClick = onAddFirstRoom,
                            isBusy = isBusy
                        )
                    }
                    return@LazyColumn
                }

                item {
                    FilledTonalButton(
                        onClick = onOpenRoomsInventory,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .pointerHoverIcon(PointerIcon.Hand)
                            .testTag("dashboard_open_inventory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bed,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ver inventario de habitaciones (${dashboard.rooms.size})",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (cleaningRooms.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Limpieza pendiente (${cleaningRooms.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(cleaningRooms, key = { it.id }) { room ->
                                    Card(
                                        shape = MaterialTheme.shapes.medium,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.CleaningServices,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Hab. ${room.number}",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = room.roomType.shortEs,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            FilledTonalButton(
                                                onClick = { onMarkRoomAvailable(room) },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .pointerHoverIcon(PointerIcon.Hand)
                                                    .testTag("quick_clean_${room.id}")
                                            ) {
                                                Text("Lista")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        DashboardQueue.entries.forEachIndexed { index, queue ->
                            val count = when (queue) {
                                DashboardQueue.CHECK_INS -> dashboard.pendingCheckInsToday.size
                                DashboardQueue.CHECK_OUTS -> dashboard.pendingCheckOutsToday.size
                                DashboardQueue.IN_HOUSE -> dashboard.inHouseStays.size
                            }
                            SegmentedButton(
                                selected = selectedQueue == queue,
                                onClick = {
                                    selectedQueue = queue
                                    selectedReservationId = null
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = DashboardQueue.entries.size
                                ),
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .testTag("dashboard_queue_${queue.name.lowercase()}")
                            ) {
                                Text(
                                    text = "${queue.label} ($count)",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (activeList.isEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = when (selectedQueue) {
                                        DashboardQueue.CHECK_INS -> "Sin llegadas pendientes hoy"
                                        DashboardQueue.CHECK_OUTS -> "Sin salidas pendientes hoy"
                                        DashboardQueue.IN_HOUSE -> "No hay huéspedes alojados ahora"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Crea una nueva reserva o registra un check-in directo con el botón inferior.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(activeList, key = { it.id }) { reservation ->
                        ReservationActionCard(
                            reservation = reservation,
                            mode = cardMode,
                            isSelected = isSplitView && selectedReservation?.id == reservation.id,
                            onCardClick = {
                                if (isSplitView) {
                                    selectedReservationId = reservation.id
                                } else {
                                    onReservationClick(reservation)
                                }
                            },
                            onCheckInClick = { onCheckInClick(reservation) },
                            onCheckOutClick = { onCheckOutClick(reservation) }
                        )
                    }
                }
            }

            if (isSplitView) {
                Box(
                    modifier = Modifier
                        .weight(0.88f)
                        .fillMaxHeight()
                        .padding(top = 12.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    ReservationDetailPane(
                        reservation = selectedReservation,
                        isBusy = isBusy,
                        onCheckIn = onCheckInClick,
                        onCheckOut = onCheckOutClick,
                        onEdit = onEditReservation,
                        onCancelReservation = onCancelReservation,
                        onDeleteReservation = onDeleteReservation
                    )
                }
            }
        }
    }
}
