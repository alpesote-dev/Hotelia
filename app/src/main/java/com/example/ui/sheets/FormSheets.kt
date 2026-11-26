package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.Room
import com.example.data.RoomStatus
import com.example.data.RoomType
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReservationFormSheet(
    existing: Reservation?,
    rooms: List<Room>,
    isBusy: Boolean,
    preselectedRoomId: String? = null,
    onSave: (
        guestName: String,
        guestEmail: String?,
        guestPhone: String?,
        guestDocument: String?,
        room: Room,
        checkInDate: String,
        checkOutDate: String,
        guestsCount: Int,
        paidAmount: Double,
        specialRequests: String?,
        checkInImmediately: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { HotelDateHelper.todayIso() }
    val tomorrow = remember { HotelDateHelper.todayOffsetIso(1) }
    val inTwoDays = remember { HotelDateHelper.todayOffsetIso(2) }

    val selectableRooms = remember(rooms, existing) {
        rooms.filter {
            it.roomStatus != RoomStatus.MAINTENANCE || it.id == existing?.roomId
        }
    }

    var guestName by remember(existing) { mutableStateOf(existing?.guestName ?: "") }
    var guestPhone by remember(existing) { mutableStateOf(existing?.guestPhone ?: "") }
    var guestDocument by remember(existing) { mutableStateOf(existing?.guestDocument ?: "") }
    var guestEmail by remember(existing) { mutableStateOf(existing?.guestEmail ?: "") }
    var checkInDate by remember(existing) { mutableStateOf(existing?.checkInDate ?: today) }
    var nights by remember(existing) { mutableIntStateOf(existing?.nightsCount ?: 2) }
    var guestsCount by remember(existing) { mutableIntStateOf(existing?.guestsCountInt ?: 2) }
    var selectedRoomId by remember(existing, selectableRooms, preselectedRoomId) {
        mutableStateOf(
            existing?.roomId
                ?: preselectedRoomId?.takeIf { id -> selectableRooms.any { it.id == id } }
                ?: selectableRooms.firstOrNull { it.roomStatus == RoomStatus.AVAILABLE }?.id
                ?: selectableRooms.firstOrNull()?.id
                ?: ""
        )
    }
    var paidText by remember(existing) {
        mutableStateOf(
            if (existing != null && existing.paidAmount > 0.0) {
                "%.2f".format(Locale.US, existing.paidAmount)
            } else {
                "0"
            }
        )
    }
    var specialRequests by remember(existing) { mutableStateOf(existing?.specialRequests ?: "") }
    var checkInImmediately by remember(existing) { mutableStateOf(false) }

    val selectedRoom = selectableRooms.firstOrNull { it.id == selectedRoomId }
    val checkOutDate = remember(checkInDate, nights) {
        HotelDateHelper.addDaysIso(checkInDate, nights.coerceAtLeast(1))
    }
    val totalAmount = (selectedRoom?.nightlyRate ?: 0.0) * nights
    val parsedPaid = paidText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val canWalkIn = existing == null &&
        selectedRoom != null &&
        selectedRoom.roomStatus == RoomStatus.AVAILABLE &&
        checkInDate <= today

    val isFormValid = guestName.trim().length >= 2 &&
        selectedRoom != null &&
        HotelDateHelper.isValidIsoDate(checkInDate) &&
        HotelDateHelper.isValidIsoDate(checkOutDate) &&
        checkOutDate > checkInDate

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
                text = if (existing == null) "Nueva reserva" else "Editar reserva",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = guestName,
                onValueChange = { if (it.length <= 80) guestName = it },
                label = { Text("Nombre del huésped *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reservation_guest_name_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = guestDocument,
                    onValueChange = { if (it.length <= 30) guestDocument = it },
                    label = { Text("DNI / Pasaporte") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = guestPhone,
                    onValueChange = { if (it.length <= 30) guestPhone = it },
                    label = { Text("Teléfono") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = guestEmail,
                onValueChange = { if (it.length <= 120) guestEmail = it },
                label = { Text("Correo electrónico (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Habitación asignada *", style = MaterialTheme.typography.titleSmall)

            if (selectableRooms.isEmpty()) {
                Text(
                    text = "No hay habitaciones disponibles. Crea una habitación primero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(selectableRooms, key = { it.id }) { room ->
                        val selected = room.id == selectedRoomId
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            ),
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier
                                .clickable { selectedRoomId = room.id }
                                .testTag("select_room_${room.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Hab. ${room.number}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = room.roomType.shortEs,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${HotelDateHelper.formatCurrency(room.nightlyRate)}/n",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Text("Fecha de entrada y estancia", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = checkInDate == today,
                    onClick = { checkInDate = today },
                    label = { Text("Hoy") }
                )
                FilterChip(
                    selected = checkInDate == tomorrow,
                    onClick = { checkInDate = tomorrow },
                    label = { Text("Mañana") }
                )
                FilterChip(
                    selected = checkInDate == inTwoDays,
                    onClick = { checkInDate = inTwoDays },
                    label = { Text("+2 días") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = checkInDate,
                    onValueChange = { if (it.length <= 10) checkInDate = it },
                    label = { Text("Entrada (AAAA-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )
                StepperBox(
                    label = "Noches",
                    value = nights,
                    onDecrement = { if (nights > 1) nights-- },
                    onIncrement = { if (nights < 30) nights++ },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperBox(
                    label = "Huéspedes",
                    value = guestsCount,
                    onDecrement = { if (guestsCount > 1) guestsCount-- },
                    onIncrement = {
                        val maxCap = selectedRoom?.capacityInt ?: 12
                        if (guestsCount < maxCap) guestsCount++
                    },
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Salida: ${HotelDateHelper.formatShortDate(checkOutDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total: ${HotelDateHelper.formatCurrency(totalAmount)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (totalAmount > 0.0) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = parsedPaid == 0.0,
                        onClick = { paidText = "0" },
                        label = { Text("Sin anticipo") }
                    )
                    FilterChip(
                        selected = kotlin.math.abs(parsedPaid - totalAmount * 0.5) < 0.05,
                        onClick = { paidText = "%.2f".format(Locale.US, totalAmount * 0.5) },
                        label = { Text("50% anticipo") }
                    )
                    FilterChip(
                        selected = kotlin.math.abs(parsedPaid - totalAmount) < 0.05,
                        onClick = { paidText = "%.2f".format(Locale.US, totalAmount) },
                        label = { Text("100% pagado") }
                    )
                }
            }

            OutlinedTextField(
                value = paidText,
                onValueChange = { paidText = it },
                label = { Text("Importe abonado") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = specialRequests,
                onValueChange = { if (it.length <= 300) specialRequests = it },
                label = { Text("Solicitudes especiales (opcional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (canWalkIn) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Check-in inmediato (Walk-in)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Entrega la habitación ahora mismo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = checkInImmediately,
                            onCheckedChange = { checkInImmediately = it },
                            modifier = Modifier.testTag("walk_in_switch")
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val room = selectedRoom ?: return@Button
                    onSave(
                        guestName.trim(),
                        guestEmail.trim().takeIf { it.isNotEmpty() },
                        guestPhone.trim().takeIf { it.isNotEmpty() },
                        guestDocument.trim().takeIf { it.isNotEmpty() },
                        room,
                        checkInDate,
                        checkOutDate,
                        guestsCount,
                        parsedPaid,
                        specialRequests.trim().takeIf { it.isNotEmpty() },
                        canWalkIn && checkInImmediately
                    )
                },
                enabled = !isBusy && isFormValid,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("save_reservation_button")
            ) {
                Text(
                    when {
                        existing != null -> "Guardar cambios"
                        canWalkIn && checkInImmediately -> "Registrar reserva y Check-in"
                        else -> "Confirmar reserva"
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoomFormSheet(
    existing: Room?,
    isBusy: Boolean,
    onSave: (
        number: String,
        type: RoomType,
        floor: Int,
        nightlyRate: Double,
        capacity: Int,
        status: RoomStatus,
        notes: String?
    ) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var number by remember(existing) { mutableStateOf(existing?.number ?: "") }
    var type by remember(existing) { mutableStateOf(existing?.roomType ?: RoomType.STANDARD) }
    var floor by remember(existing) { mutableIntStateOf(existing?.floorInt ?: 1) }
    var rateText by remember(existing) {
        mutableStateOf(
            if (existing != null) "%.2f".format(Locale.US, existing.nightlyRate) else "110.00"
        )
    }
    var capacity by remember(existing) { mutableIntStateOf(existing?.capacityInt ?: 2) }
    var status by remember(existing) { mutableStateOf(existing?.roomStatus ?: RoomStatus.AVAILABLE) }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }

    val parsedRate = rateText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val isValid = number.trim().length in 1..10 && parsedRate > 0.0 && parsedRate <= 100000.0

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
                text = if (existing == null) "Nueva habitación" else "Habitación ${existing.number}",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { if (it.length <= 10) number = it },
                    label = { Text("Número *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("room_number_input")
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Tarifa / noche *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("room_rate_input")
                )
            }

            Text("Categoría", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoomType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.labelEs) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StepperBox(
                    label = "Piso",
                    value = floor,
                    onDecrement = { if (floor > 1) floor-- },
                    onIncrement = { if (floor < 50) floor++ },
                    modifier = Modifier.weight(1f)
                )
                StepperBox(
                    label = "Capacidad",
                    value = capacity,
                    onDecrement = { if (capacity > 1) capacity-- },
                    onIncrement = { if (capacity < 12) capacity++ },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Estado operativo", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoomStatus.entries.forEach { option ->
                    FilterChip(
                        selected = status == option,
                        onClick = { status = option },
                        label = { Text(option.labelEs) }
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 300) notes = it },
                label = { Text("Amenidades o notas (opcional)") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSave(
                        number.trim(),
                        type,
                        floor,
                        parsedRate,
                        capacity,
                        status,
                        notes.trim().takeIf { it.isNotEmpty() }
                    )
                },
                enabled = !isBusy && isValid,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("save_room_button")
            ) {
                Text(if (existing == null) "Crear habitación" else "Guardar cambios")
            }

            if (existing != null && onDelete != null && existing.roomStatus != RoomStatus.OCCUPIED) {
                TextButton(
                    onClick = onDelete,
                    enabled = !isBusy,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("delete_room_button")
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar habitación")
                }
            }
        }
    }
}

@Composable
private fun StepperBox(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Disminuir $label", modifier = Modifier.size(18.dp))
                }
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Aumentar $label", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
