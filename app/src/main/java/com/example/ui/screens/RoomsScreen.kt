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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.Reservation
import com.example.data.ReservationStatus
import com.example.data.Room
import com.example.data.RoomStatus
import com.example.data.RoomType
import com.example.ui.components.EmptyHotelState
import com.example.ui.components.RoomDetailPane
import com.example.ui.components.RoomGridCard
import com.example.ui.components.RoomInventorySummaryCard

@Composable
fun RoomsScreen(
    rooms: List<Room>,
    reservations: List<Reservation>,
    isBusy: Boolean,
    onSeedStarterHotel: () -> Unit,
    onAddRoom: () -> Unit,
    onRoomClick: (Room) -> Unit,
    onChangeRoomStatus: (Room, RoomStatus) -> Unit,
    onNewReservationForRoom: (Room) -> Unit,
    onOpenReservation: (Reservation) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStatus by rememberSaveable { mutableStateOf<RoomStatus?>(null) }
    var selectedType by rememberSaveable { mutableStateOf<RoomType?>(null) }
    var selectedFloor by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedRoomId by rememberSaveable { mutableStateOf<String?>(null) }

    val floors = remember(rooms) {
        rooms.map { it.floorInt }.distinct().sorted()
    }

    val activeGuestByRoomId = remember(reservations) {
        reservations
            .filter { it.reservationStatus == ReservationStatus.CHECKED_IN }
            .associate { it.roomId to it.guestName }
    }

    val filteredRooms = remember(rooms, selectedStatus, selectedType, selectedFloor) {
        rooms.filter { room ->
            val okStatus = selectedStatus == null || room.roomStatus == selectedStatus
            val okType = selectedType == null || room.roomType == selectedType
            val okFloor = selectedFloor == null || room.floorInt == selectedFloor
            okStatus && okType && okFloor
        }
    }

    val selectedRoom = filteredRooms.firstOrNull { it.id == selectedRoomId }
        ?: filteredRooms.firstOrNull()

    val linkedReservation = remember(selectedRoom, reservations) {
        if (selectedRoom == null) null
        else reservations.firstOrNull {
            it.roomId == selectedRoom.id && it.reservationStatus == ReservationStatus.CHECKED_IN
        } ?: reservations.firstOrNull {
            it.roomId == selectedRoom.id && it.reservationStatus == ReservationStatus.CONFIRMED
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isSplitView = maxWidth >= 840.dp && rooms.isNotEmpty()

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(if (isSplitView) 1.18f else 1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedStatus == null,
                                onClick = { selectedStatus = null },
                                label = { Text("Todas (${rooms.size})") },
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .testTag("filter_room_all")
                            )
                        }
                        items(RoomStatus.entries) { status ->
                            val count = rooms.count { it.roomStatus == status }
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = {
                                    selectedStatus = if (selectedStatus == status) null else status
                                },
                                label = { Text("${status.labelEs} ($count)") },
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .testTag("filter_room_${status.name.lowercase()}")
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedType == null,
                                onClick = { selectedType = null },
                                label = { Text("Todos los tipos") },
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .testTag("filter_room_type_all")
                            )
                        }
                        items(RoomType.entries) { type ->
                            val count = rooms.count { it.roomType == type }
                            FilterChip(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = if (selectedType == type) null else type
                                },
                                label = { Text("${type.labelEs} ($count)") },
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .testTag("filter_room_type_${type.name.lowercase()}")
                            )
                        }
                    }

                    if (floors.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedFloor == null,
                                    onClick = { selectedFloor = null },
                                    label = { Text("Todos los pisos") },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                )
                            }
                            items(floors) { floor ->
                                FilterChip(
                                    selected = selectedFloor == floor,
                                    onClick = {
                                        selectedFloor = if (selectedFloor == floor) null else floor
                                    },
                                    label = { Text("Piso $floor") },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                )
                            }
                        }
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = when {
                        maxWidth >= 760.dp -> 3
                        maxWidth >= 480.dp -> 2
                        else -> 1
                    }
                    val rows = remember(filteredRooms, columns) {
                        filteredRooms.chunked(columns)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (rooms.isNotEmpty()) {
                            item(key = "inventory_summary") {
                                RoomInventorySummaryCard(rooms = rooms)
                            }
                        }

                        if (filteredRooms.isEmpty()) {
                            item(key = "empty_rooms_state") {
                                EmptyHotelState(
                                    title = if (rooms.isEmpty()) "Sin habitaciones registradas" else "Ninguna habitación coincide",
                                    subtitle = if (rooms.isEmpty()) {
                                        "Agrega tus habitaciones o carga un hotel de inicio con 8 habitaciones para comenzar."
                                    } else {
                                        "Prueba quitando los filtros de tipo, estado o piso."
                                    },
                                    primaryButtonText = if (rooms.isEmpty()) "Cargar hotel de inicio" else "Limpiar filtros",
                                    onPrimaryClick = if (rooms.isEmpty()) {
                                        onSeedStarterHotel
                                    } else {
                                        {
                                            selectedStatus = null
                                            selectedType = null
                                            selectedFloor = null
                                        }
                                    },
                                    secondaryButtonText = if (rooms.isEmpty()) "Crear habitación" else null,
                                    onSecondaryClick = if (rooms.isEmpty()) onAddRoom else null,
                                    isBusy = isBusy
                                )
                            }
                        } else {
                            items(rows, key = { row -> row.joinToString("-") { it.id } }) { rowRooms ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowRooms.forEach { room ->
                                        RoomGridCard(
                                            room = room,
                                            isSelected = isSplitView && selectedRoom?.id == room.id,
                                            activeGuestName = activeGuestByRoomId[room.id],
                                            onCardClick = {
                                                if (isSplitView) {
                                                    selectedRoomId = room.id
                                                } else {
                                                    onRoomClick(room)
                                                }
                                            },
                                            onMarkAvailableClick = {
                                                onChangeRoomStatus(room, RoomStatus.AVAILABLE)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(columns - rowRooms.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isSplitView) {
                Box(
                    modifier = Modifier
                        .weight(0.82f)
                        .fillMaxHeight()
                        .padding(top = 12.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    RoomDetailPane(
                        room = selectedRoom,
                        activeReservation = linkedReservation,
                        isBusy = isBusy,
                        onChangeStatus = onChangeRoomStatus,
                        onEditRoom = onRoomClick,
                        onNewReservationForRoom = onNewReservationForRoom,
                        onOpenReservation = onOpenReservation
                    )
                }
            }
        }
    }
}
