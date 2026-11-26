package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HotelDateHelper
import com.example.data.HotelRepository
import com.example.data.Reservation
import com.example.data.ReservationStatus
import com.example.data.Room
import com.example.data.RoomStatus
import com.example.data.RoomType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class HotelDashboardData(
    val rooms: List<Room>,
    val reservations: List<Reservation>,
    val todayIso: String,
    val pendingCheckInsToday: List<Reservation>,
    val pendingCheckOutsToday: List<Reservation>,
    val inHouseStays: List<Reservation>,
    val availableRoomsCount: Int,
    val occupiedRoomsCount: Int,
    val cleaningRoomsCount: Int,
    val maintenanceRoomsCount: Int,
    val occupancyRatePercent: Int
)

class HotelViewModel(
    private val repository: HotelRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val roomsState: StateFlow<UiState<List<Room>>> = repository.observeRooms(currentUserId)
        .combine(MutableStateFlow(Unit)) { rooms, _ -> rooms }
        .catch { /* handled below via two-tier unwrap */ throw it }
        .let { flow ->
            kotlinx.coroutines.flow.flow<UiState<List<Room>>> {
                flow.collect { emit(UiState.Success(it)) }
            }.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing rooms", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "No se pudieron cargar las habitaciones."))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val reservationsState: StateFlow<UiState<List<Reservation>>> =
        kotlinx.coroutines.flow.flow<UiState<List<Reservation>>> {
            repository.observeReservations(currentUserId).collect { emit(UiState.Success(it)) }
        }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing reservations", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "No se pudieron cargar las reservas."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val dashboardState: StateFlow<UiState<HotelDashboardData>> =
        combine(roomsState, reservationsState) { rState, resState ->
            when {
                rState is UiState.Error -> UiState.Error(rState.message)
                resState is UiState.Error -> UiState.Error(resState.message)
                rState is UiState.Success && resState is UiState.Success -> {
                    val rooms = rState.data
                    val reservations = resState.data
                    val today = HotelDateHelper.todayIso()

                    val pendingCheckIns = reservations.filter {
                        it.reservationStatus == ReservationStatus.CONFIRMED && it.checkInDate <= today
                    }
                    val pendingCheckOuts = reservations.filter {
                        it.reservationStatus == ReservationStatus.CHECKED_IN && it.checkOutDate <= today
                    }
                    val inHouse = reservations.filter {
                        it.reservationStatus == ReservationStatus.CHECKED_IN
                    }

                    val availableCount = rooms.count { it.roomStatus == RoomStatus.AVAILABLE }
                    val occupiedCount = rooms.count { it.roomStatus == RoomStatus.OCCUPIED }
                    val cleaningCount = rooms.count { it.roomStatus == RoomStatus.CLEANING }
                    val maintenanceCount = rooms.count { it.roomStatus == RoomStatus.MAINTENANCE }
                    val occPercent = if (rooms.isEmpty()) 0 else ((occupiedCount * 100f) / rooms.size).toInt()

                    UiState.Success(
                        HotelDashboardData(
                            rooms = rooms,
                            reservations = reservations,
                            todayIso = today,
                            pendingCheckInsToday = pendingCheckIns,
                            pendingCheckOutsToday = pendingCheckOuts,
                            inHouseStays = inHouse,
                            availableRoomsCount = availableCount,
                            occupiedRoomsCount = occupiedCount,
                            cleaningRoomsCount = cleaningCount,
                            maintenanceRoomsCount = maintenanceCount,
                            occupancyRatePercent = occPercent
                        )
                    )
                }
                else -> UiState.Loading
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun seedStarterHotel() = runAction("Hotel de inicio configurado con éxito") {
        repository.seedStarterHotel(currentUserId)
    }

    fun createRoom(
        number: String,
        type: RoomType,
        floor: Int,
        nightlyRate: Double,
        capacity: Int,
        status: RoomStatus,
        notes: String?
    ) = runAction("Habitación $number agregada") {
        repository.createRoom(currentUserId, number, type, floor, nightlyRate, capacity, status, notes)
    }

    fun updateRoom(
        roomId: String,
        number: String,
        type: RoomType,
        floor: Int,
        nightlyRate: Double,
        capacity: Int,
        status: RoomStatus,
        notes: String?
    ) = runAction("Habitación $number actualizada") {
        repository.updateRoom(currentUserId, roomId, number, type, floor, nightlyRate, capacity, status, notes)
    }

    fun updateRoomStatus(room: Room, newStatus: RoomStatus) =
        runAction("Habitación ${room.number} marcada como ${newStatus.labelEs}") {
            repository.updateRoomStatus(currentUserId, room.id, newStatus)
        }

    fun deleteRoom(room: Room) = runAction("Habitación ${room.number} eliminada") {
        repository.deleteRoom(currentUserId, room.id)
    }

    fun createReservation(
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
    ) = runAction(
        if (checkInImmediately) "Check-in directo completado para ${guestName.trim()}"
        else "Reserva confirmada para ${guestName.trim()}"
    ) {
        repository.createReservation(
            userId = currentUserId,
            guestName = guestName,
            guestEmail = guestEmail,
            guestPhone = guestPhone,
            guestDocument = guestDocument,
            room = room,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            guestsCount = guestsCount,
            paidAmount = paidAmount,
            specialRequests = specialRequests,
            checkInImmediately = checkInImmediately
        )
    }

    fun updateReservation(
        reservationId: String,
        guestName: String,
        guestEmail: String?,
        guestPhone: String?,
        guestDocument: String?,
        room: Room,
        checkInDate: String,
        checkOutDate: String,
        guestsCount: Int,
        paidAmount: Double,
        specialRequests: String?
    ) = runAction("Reserva actualizada") {
        repository.updateReservation(
            userId = currentUserId,
            reservationId = reservationId,
            guestName = guestName,
            guestEmail = guestEmail,
            guestPhone = guestPhone,
            guestDocument = guestDocument,
            room = room,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            guestsCount = guestsCount,
            paidAmount = paidAmount,
            specialRequests = specialRequests
        )
    }

    fun performCheckIn(reservation: Reservation, additionalPayment: Double) =
        runAction("Check-in completado · Hab. ${reservation.roomNumber}") {
            repository.performCheckIn(currentUserId, reservation, additionalPayment)
        }

    fun performCheckOut(reservation: Reservation, finalPayment: Double) =
        runAction("Check-out completado · Hab. ${reservation.roomNumber} en limpieza") {
            repository.performCheckOut(currentUserId, reservation, finalPayment)
        }

    fun cancelReservation(reservation: Reservation) =
        runAction("Reserva de ${reservation.guestName} cancelada") {
            repository.cancelReservation(currentUserId, reservation.id)
        }

    fun deleteReservation(reservation: Reservation) =
        runAction("Reserva eliminada") {
            repository.deleteReservation(currentUserId, reservation.id)
        }

    private fun runAction(successMessage: String, block: suspend () -> Result<Unit>) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val result = block()
                result.onSuccess {
                    _events.emit(successMessage)
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Ocurrió un error al guardar los cambios.")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    private companion object {
        const val TAG = "HotelViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
