package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.Reservation
import com.example.data.ReservationStatus
import com.example.ui.components.EmptyHotelState
import com.example.ui.components.ReservationActionCard
import com.example.ui.components.ReservationCardMode
import com.example.ui.components.ReservationDetailPane

@Composable
fun ReservationsScreen(
    reservations: List<Reservation>,
    hasRooms: Boolean,
    isBusy: Boolean,
    searchFocusRequester: FocusRequester,
    onCreateReservation: () -> Unit,
    onReservationClick: (Reservation) -> Unit,
    onCheckInClick: (Reservation) -> Unit,
    onCheckOutClick: (Reservation) -> Unit,
    onEditReservation: (Reservation) -> Unit,
    onCancelReservation: (Reservation) -> Unit,
    onDeleteReservation: (Reservation) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf<ReservationStatus?>(null) }
    var selectedReservationId by rememberSaveable { mutableStateOf<String?>(null) }

    val filtered = remember(reservations, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase()
        reservations.filter { res ->
            val matchesStatus = selectedFilter == null || res.reservationStatus == selectedFilter
            val matchesQuery = q.isEmpty() ||
                res.guestName.lowercase().contains(q) ||
                res.roomNumber.lowercase().contains(q) ||
                (res.guestDocument?.lowercase()?.contains(q) == true) ||
                (res.guestPhone?.lowercase()?.contains(q) == true)
            matchesStatus && matchesQuery
        }
    }

    val selectedReservation = filtered.firstOrNull { it.id == selectedReservationId }
        ?: filtered.firstOrNull()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isSplitView = maxWidth >= 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(if (isSplitView) 1.08f else 1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar huésped, habitación o DNI... (Ctrl+F)") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Limpiar búsqueda")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester)
                            .testTag("reservations_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedFilter == null,
                                onClick = { selectedFilter = null },
                                label = { Text("Todas (${reservations.size})") },
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            )
                        }
                        items(ReservationStatus.entries) { status ->
                            val count = reservations.count { it.reservationStatus == status }
                            FilterChip(
                                selected = selectedFilter == status,
                                onClick = {
                                    selectedFilter = if (selectedFilter == status) null else status
                                },
                                label = { Text("${status.labelEs} ($count)") },
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .testTag("filter_res_${status.name.lowercase()}")
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            EmptyHotelState(
                                title = if (reservations.isEmpty()) "Aún no hay reservas" else "Sin resultados",
                                subtitle = if (reservations.isEmpty()) {
                                    "Registra una reserva futura o realiza un check-in directo para empezar."
                                } else {
                                    "Prueba cambiando los filtros o el término de búsqueda."
                                },
                                primaryButtonText = if (hasRooms && reservations.isEmpty()) "Nueva reserva" else null,
                                onPrimaryClick = if (hasRooms && reservations.isEmpty()) onCreateReservation else null
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { reservation ->
                            ReservationActionCard(
                                reservation = reservation,
                                mode = ReservationCardMode.GENERAL,
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
            }

            if (isSplitView) {
                Box(
                    modifier = Modifier
                        .weight(0.92f)
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
