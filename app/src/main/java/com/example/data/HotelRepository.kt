package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HotelRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google antes de operar en el hotel.")
    }

    private fun roomsCollection(userId: String) =
        db.collection("users").document(userId).collection("rooms")

    private fun reservationsCollection(userId: String) =
        db.collection("users").document(userId).collection("reservations")

    fun observeRooms(userId: String): Flow<List<Room>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "La sesión activa no coincide con el usuario solicitado." }
        val path = "users/$uid/rooms"
        emitAll(
            roomsCollection(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Room::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<Room> { it.floor }.thenBy { it.number })
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeReservations(userId: String): Flow<List<Reservation>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "La sesión activa no coincide con el usuario solicitado." }
        val path = "users/$uid/reservations"
        emitAll(
            reservationsCollection(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Reservation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.checkInDate }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createRoom(
        userId: String,
        number: String,
        type: RoomType,
        floor: Int,
        nightlyRate: Double,
        capacity: Int,
        status: RoomStatus,
        notes: String?
    ): Result<Unit> {
        var docPath = "users/$userId/rooms"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa cambió; operación cancelada." }
            val trimmedNumber = number.trim()
            require(trimmedNumber.length in 1..10) { "El número de habitación debe tener entre 1 y 10 caracteres." }
            require(floor in 1..50) { "El piso debe estar entre 1 y 50." }
            require(nightlyRate > 0.0 && nightlyRate <= 100000.0) { "La tarifa debe ser mayor a 0." }
            require(capacity in 1..12) { "La capacidad debe estar entre 1 y 12 huéspedes." }
            val trimmedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
            if (trimmedNotes != null) {
                require(trimmedNotes.length <= 300) { "Las notas no pueden superar 300 caracteres." }
            }

            val docRef = roomsCollection(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "number" to trimmedNumber,
                "type" to type.name,
                "floor" to floor.toLong(),
                "nightlyRate" to nightlyRate,
                "capacity" to capacity.toLong(),
                "status" to status.name,
                "notes" to trimmedNotes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRoom(
        userId: String,
        roomId: String,
        number: String,
        type: RoomType,
        floor: Int,
        nightlyRate: Double,
        capacity: Int,
        status: RoomStatus,
        notes: String?
    ): Result<Unit> {
        var docPath = "users/$userId/rooms/$roomId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa cambió; operación cancelada." }
            require(roomId.isNotBlank()) { "ID de habitación inválido." }
            val trimmedNumber = number.trim()
            require(trimmedNumber.length in 1..10) { "El número de habitación debe tener entre 1 y 10 caracteres." }
            require(floor in 1..50) { "El piso debe estar entre 1 y 50." }
            require(nightlyRate > 0.0 && nightlyRate <= 100000.0) { "La tarifa debe ser mayor a 0." }
            require(capacity in 1..12) { "La capacidad debe estar entre 1 y 12 huéspedes." }
            val trimmedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
            if (trimmedNotes != null) {
                require(trimmedNotes.length <= 300) { "Las notas no pueden superar 300 caracteres." }
            }

            val docRef = roomsCollection(uid).document(roomId)
            docPath = docRef.path
            val updates = mapOf<String, Any>(
                "number" to trimmedNumber,
                "type" to type.name,
                "floor" to floor.toLong(),
                "nightlyRate" to nightlyRate,
                "capacity" to capacity.toLong(),
                "status" to status.name,
                "notes" to (trimmedNotes ?: FieldValue.delete()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRoomStatus(
        userId: String,
        roomId: String,
        newStatus: RoomStatus
    ): Result<Unit> {
        var docPath = "users/$userId/rooms/$roomId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa cambió; operación cancelada." }
            require(roomId.isNotBlank()) { "ID de habitación inválido." }
            val docRef = roomsCollection(uid).document(roomId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRoom(userId: String, roomId: String): Result<Unit> {
        var docPath = "users/$userId/rooms/$roomId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa cambió; operación cancelada." }
            require(roomId.isNotBlank()) { "ID de habitación inválido." }
            val docRef = roomsCollection(uid).document(roomId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createReservation(
        userId: String,
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
    ): Result<Unit> {
        var docPath = "users/$userId/reservations"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa cambió; operación cancelada." }

            val cleanName = guestName.trim()
            require(cleanName.length in 2..80) { "El nombre del huésped debe tener entre 2 y 80 caracteres." }
            require(room.id.isNotBlank()) { "Selecciona una habitación válida." }
            require(HotelDateHelper.isValidIsoDate(checkInDate)) { "Fecha de entrada inválida (YYYY-MM-DD)." }
            require(HotelDateHelper.isValidIsoDate(checkOutDate)) { "Fecha de salida inválida (YYYY-MM-DD)." }
            require(checkOutDate > checkInDate) { "La fecha de salida debe ser posterior a la de entrada." }
            require(guestsCount in 1..12) { "Cantidad de huéspedes inválida." }
            require(paidAmount >= 0.0) { "El monto abonado no puede ser negativo." }

            val cleanEmail = guestEmail?.trim()?.takeIf { it.isNotEmpty() }
            val cleanPhone = guestPhone?.trim()?.takeIf { it.isNotEmpty() }
            val cleanDoc = guestDocument?.trim()?.takeIf { it.isNotEmpty() }
            val cleanNotes = specialRequests?.trim()?.takeIf { it.isNotEmpty() }

            if (cleanEmail != null) require(cleanEmail.length <= 120) { "Correo demasiado largo." }
            if (cleanPhone != null) require(cleanPhone.length <= 30) { "Teléfono demasiado largo." }
            if (cleanDoc != null) require(cleanDoc.length <= 30) { "Documento demasiado largo." }
            if (cleanNotes != null) require(cleanNotes.length <= 300) { "Notas demasiado largas." }

            val nights = HotelDateHelper.nightsBetween(checkInDate, checkOutDate)
            val totalAmount = nights * room.nightlyRate

            val resRef = reservationsCollection(uid).document()
            docPath = resRef.path
            val roomRef = roomsCollection(uid).document(room.id)

            val payload = mapOf(
                "guestName" to cleanName,
                "guestEmail" to cleanEmail,
                "guestPhone" to cleanPhone,
                "guestDocument" to cleanDoc,
                "roomId" to room.id,
                "roomNumber" to room.number,
                "roomType" to room.type,
                "checkInDate" to checkInDate,
                "checkOutDate" to checkOutDate,
                "guestsCount" to guestsCount.toLong(),
                "nightlyRate" to room.nightlyRate,
                "totalAmount" to totalAmount,
                "paidAmount" to paidAmount.coerceAtMost(totalAmount),
                "status" to if (checkInImmediately) ReservationStatus.CHECKED_IN.name else ReservationStatus.CONFIRMED.name,
                "specialRequests" to cleanNotes,
                "actualCheckInAt" to if (checkInImmediately) FieldValue.serverTimestamp() else null,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            if (checkInImmediately) {
                db.runBatch { batch ->
                    batch.set(resRef, payload)
                    batch.update(
                        roomRef,
                        mapOf(
                            "status" to RoomStatus.OCCUPIED.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }.await()
            } else {
                resRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateReservation(
        userId: String,
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
    ): Result<Unit> {
        var docPath = "users/$userId/reservations/$reservationId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa cambió; operación cancelada." }
            require(reservationId.isNotBlank()) { "ID de reserva inválido." }

            val cleanName = guestName.trim()
            require(cleanName.length in 2..80) { "El nombre del huésped debe tener entre 2 y 80 caracteres." }
            require(HotelDateHelper.isValidIsoDate(checkInDate)) { "Fecha de entrada inválida (YYYY-MM-DD)." }
            require(HotelDateHelper.isValidIsoDate(checkOutDate)) { "Fecha de salida inválida (YYYY-MM-DD)." }
            require(checkOutDate > checkInDate) { "La fecha de salida debe ser posterior a la de entrada." }
            require(guestsCount in 1..12) { "Cantidad de huéspedes inválida." }
            require(paidAmount >= 0.0) { "El monto abonado no puede ser negativo." }

            val cleanEmail = guestEmail?.trim()?.takeIf { it.isNotEmpty() }
            val cleanPhone = guestPhone?.trim()?.takeIf { it.isNotEmpty() }
            val cleanDoc = guestDocument?.trim()?.takeIf { it.isNotEmpty() }
            val cleanNotes = specialRequests?.trim()?.takeIf { it.isNotEmpty() }

            val nights = HotelDateHelper.nightsBetween(checkInDate, checkOutDate)
            val totalAmount = nights * room.nightlyRate

            val docRef = reservationsCollection(uid).document(reservationId)
            docPath = docRef.path
            val updates = mapOf<String, Any>(
                "guestName" to cleanName,
                "guestEmail" to (cleanEmail ?: FieldValue.delete()),
                "guestPhone" to (cleanPhone ?: FieldValue.delete()),
                "guestDocument" to (cleanDoc ?: FieldValue.delete()),
                "roomId" to room.id,
                "roomNumber" to room.number,
                "roomType" to room.type,
                "checkInDate" to checkInDate,
                "checkOutDate" to checkOutDate,
                "guestsCount" to guestsCount.toLong(),
                "nightlyRate" to room.nightlyRate,
                "totalAmount" to totalAmount,
                "paidAmount" to paidAmount.coerceAtMost(totalAmount),
                "specialRequests" to (cleanNotes ?: FieldValue.delete()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun performCheckIn(
        userId: String,
        reservation: Reservation,
        additionalPayment: Double
    ): Result<Unit> {
        var docPath = "users/$userId/reservations/${reservation.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa cambió; operación cancelada." }
            require(reservation.id.isNotBlank()) { "Reserva inválida." }
            require(reservation.roomId.isNotBlank()) { "Habitación inválida." }
            require(additionalPayment >= 0.0) { "El pago adicional no puede ser negativo." }

            val newPaid = (reservation.paidAmount + additionalPayment).coerceAtMost(reservation.totalAmount)
            val resRef = reservationsCollection(uid).document(reservation.id)
            docPath = resRef.path
            val roomRef = roomsCollection(uid).document(reservation.roomId)

            db.runBatch { batch ->
                batch.update(
                    resRef,
                    mapOf(
                        "status" to ReservationStatus.CHECKED_IN.name,
                        "paidAmount" to newPaid,
                        "actualCheckInAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    roomRef,
                    mapOf(
                        "status" to RoomStatus.OCCUPIED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun performCheckOut(
        userId: String,
        reservation: Reservation,
        finalPayment: Double
    ): Result<Unit> {
        var docPath = "users/$userId/reservations/${reservation.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa cambió; operación cancelada." }
            require(reservation.id.isNotBlank()) { "Reserva inválida." }
            require(reservation.roomId.isNotBlank()) { "Habitación inválida." }
            require(finalPayment >= 0.0) { "El pago final no puede ser negativo." }

            val newPaid = (reservation.paidAmount + finalPayment).coerceAtMost(reservation.totalAmount)
            val resRef = reservationsCollection(uid).document(reservation.id)
            docPath = resRef.path
            val roomRef = roomsCollection(uid).document(reservation.roomId)

            db.runBatch { batch ->
                batch.update(
                    resRef,
                    mapOf(
                        "status" to ReservationStatus.CHECKED_OUT.name,
                        "paidAmount" to newPaid,
                        "actualCheckOutAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    roomRef,
                    mapOf(
                        "status" to RoomStatus.CLEANING.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun cancelReservation(userId: String, reservationId: String): Result<Unit> {
        var docPath = "users/$userId/reservations/$reservationId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa cambió; operación cancelada." }
            require(reservationId.isNotBlank()) { "Reserva inválida." }
            val resRef = reservationsCollection(uid).document(reservationId)
            docPath = resRef.path
            resRef.update(
                mapOf(
                    "status" to ReservationStatus.CANCELLED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteReservation(userId: String, reservationId: String): Result<Unit> {
        var docPath = "users/$userId/reservations/$reservationId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa cambió; operación cancelada." }
            require(reservationId.isNotBlank()) { "Reserva inválida." }
            val resRef = reservationsCollection(uid).document(reservationId)
            docPath = resRef.path
            resRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedStarterHotel(userId: String): Result<Unit> {
        var docPath = "users/$userId/rooms"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa cambió; operación cancelada." }

            val today = HotelDateHelper.todayIso()
            val yesterday = HotelDateHelper.todayOffsetIso(-1)
            val twoDaysAgo = HotelDateHelper.todayOffsetIso(-2)
            val tomorrow = HotelDateHelper.todayOffsetIso(1)
            val inTwoDays = HotelDateHelper.todayOffsetIso(2)
            val inThreeDays = HotelDateHelper.todayOffsetIso(3)

            val starterRooms = listOf(
                Triple("room_101", "101", mapOf(
                    "number" to "101", "type" to RoomType.INDIVIDUAL.name, "floor" to 1L,
                    "nightlyRate" to 85.0, "capacity" to 1L, "status" to RoomStatus.AVAILABLE.name,
                    "notes" to "Cama individual grande, vista al patio interior",
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )),
                Triple("room_102", "102", mapOf(
                    "number" to "102", "type" to RoomType.INDIVIDUAL.name, "floor" to 1L,
                    "nightlyRate" to 85.0, "capacity" to 1L, "status" to RoomStatus.OCCUPIED.name,
                    "notes" to "Habitación silenciosa, escritorio de trabajo",
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )),
                Triple("room_103", "103", mapOf(
                    "number" to "103", "type" to RoomType.DOBLE.name, "floor" to 1L,
                    "nightlyRate" to 130.0, "capacity" to 2L, "status" to RoomStatus.CLEANING.name,
                    "notes" to "Terraza privada y cafetera espresso",
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )),
                Triple("room_201", "201", mapOf(
                    "number" to "201", "type" to RoomType.DOBLE.name, "floor" to 2L,
                    "nightlyRate" to 140.0, "capacity" to 2L, "status" to RoomStatus.AVAILABLE.name,
                    "notes" to "Balcón soleado, dos camas o cama king",
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )),
                Triple("room_202", "202", mapOf(
                    "number" to "202", "type" to RoomType.DOBLE.name, "floor" to 2L,
                    "nightlyRate" to 145.0, "capacity" to 2L, "status" to RoomStatus.OCCUPIED.name,
                    "notes" to "Cama king, bañera independiente",
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )),
                Triple("room_203", "203", mapOf(
                    "number" to "203", "type" to RoomType.SUITE.name, "floor" to 2L,
                    "nightlyRate" to 220.0, "capacity" to 4L, "status" to RoomStatus.AVAILABLE.name,
                    "notes" to "Sala de estar independiente y minibar",
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )),
                Triple("room_301", "301", mapOf(
                    "number" to "301", "type" to RoomType.SUITE.name, "floor" to 3L,
                    "nightlyRate" to 240.0, "capacity" to 4L, "status" to RoomStatus.CLEANING.name,
                    "notes" to "Suite panorámica en preparación",
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )),
                Triple("room_302", "302", mapOf(
                    "number" to "302", "type" to RoomType.SUITE.name, "floor" to 3L,
                    "nightlyRate" to 290.0, "capacity" to 4L, "status" to RoomStatus.AVAILABLE.name,
                    "notes" to "Suite superior con terraza privada",
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                ))
            )

            db.runBatch { batch ->
                starterRooms.forEach { (id, _, data) ->
                    batch.set(roomsCollection(uid).document(id), data)
                }
            }.await()

            docPath = "users/$uid/reservations"
            val starterReservations = listOf(
                "res_arrival_1" to mapOf(
                    "guestName" to "Sofía Mendoza",
                    "guestPhone" to "+34 615 482 910",
                    "guestDocument" to "49120384L",
                    "roomId" to "room_201",
                    "roomNumber" to "201",
                    "roomType" to RoomType.DOBLE.name,
                    "checkInDate" to today,
                    "checkOutDate" to inThreeDays,
                    "guestsCount" to 2L,
                    "nightlyRate" to 140.0,
                    "totalAmount" to 420.0,
                    "paidAmount" to 140.0,
                    "status" to ReservationStatus.CONFIRMED.name,
                    "specialRequests" to "Llegada estimada 14:30, almohada firme",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                "res_arrival_2" to mapOf(
                    "guestName" to "Mateo Villalobos",
                    "guestEmail" to "mateo.v@correo.com",
                    "guestPhone" to "+34 644 901 233",
                    "roomId" to "room_302",
                    "roomNumber" to "302",
                    "roomType" to RoomType.SUITE.name,
                    "checkInDate" to today,
                    "checkOutDate" to inTwoDays,
                    "guestsCount" to 2L,
                    "nightlyRate" to 290.0,
                    "totalAmount" to 580.0,
                    "paidAmount" to 580.0,
                    "status" to ReservationStatus.CONFIRMED.name,
                    "specialRequests" to "Aniversario, detalle de bienvenida",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                "res_checkout_1" to mapOf(
                    "guestName" to "Lucía Navarro",
                    "guestPhone" to "+34 699 112 780",
                    "guestDocument" to "33948210M",
                    "roomId" to "room_102",
                    "roomNumber" to "102",
                    "roomType" to RoomType.INDIVIDUAL.name,
                    "checkInDate" to twoDaysAgo,
                    "checkOutDate" to today,
                    "guestsCount" to 1L,
                    "nightlyRate" to 85.0,
                    "totalAmount" to 170.0,
                    "paidAmount" to 85.0,
                    "status" to ReservationStatus.CHECKED_IN.name,
                    "specialRequests" to "Factura a nombre de empresa",
                    "actualCheckInAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                "res_inhouse_1" to mapOf(
                    "guestName" to "Carlos & Ana Ortega",
                    "guestPhone" to "+34 622 738 401",
                    "roomId" to "room_202",
                    "roomNumber" to "202",
                    "roomType" to RoomType.DOBLE.name,
                    "checkInDate" to yesterday,
                    "checkOutDate" to inTwoDays,
                    "guestsCount" to 2L,
                    "nightlyRate" to 145.0,
                    "totalAmount" to 435.0,
                    "paidAmount" to 200.0,
                    "status" to ReservationStatus.CHECKED_IN.name,
                    "actualCheckInAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                "res_upcoming_1" to mapOf(
                    "guestName" to "Valeria Ríos",
                    "guestEmail" to "valeria.rios@mail.com",
                    "roomId" to "room_203",
                    "roomNumber" to "203",
                    "roomType" to RoomType.SUITE.name,
                    "checkInDate" to tomorrow,
                    "checkOutDate" to inThreeDays,
                    "guestsCount" to 3L,
                    "nightlyRate" to 220.0,
                    "totalAmount" to 440.0,
                    "paidAmount" to 0.0,
                    "status" to ReservationStatus.CONFIRMED.name,
                    "specialRequests" to "Cuna supletoria solicitada",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            db.runBatch { batch ->
                starterReservations.forEach { (id, data) ->
                    batch.set(reservationsCollection(uid).document(id), data)
                }
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
