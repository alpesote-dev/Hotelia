package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class RoomType(val labelEs: String, val shortEs: String) {
    STANDARD("Estándar", "Estándar"),
    DELUXE("Deluxe", "Deluxe"),
    SUITE("Suite", "Suite"),
    PENTHOUSE("Penthouse", "Penthouse");

    companion object {
        fun fromRaw(raw: String): RoomType =
            entries.firstOrNull { it.name == raw } ?: STANDARD
    }
}

enum class RoomStatus(val labelEs: String) {
    AVAILABLE("Disponible"),
    OCCUPIED("Ocupada"),
    CLEANING("Limpieza"),
    MAINTENANCE("Mantenimiento");

    companion object {
        fun fromRaw(raw: String): RoomStatus =
            entries.firstOrNull { it.name == raw } ?: AVAILABLE
    }
}

enum class ReservationStatus(val labelEs: String) {
    CONFIRMED("Confirmada"),
    CHECKED_IN("En estancia"),
    CHECKED_OUT("Finalizada"),
    CANCELLED("Cancelada");

    companion object {
        fun fromRaw(raw: String): ReservationStatus =
            entries.firstOrNull { it.name == raw } ?: CONFIRMED
    }
}

@Keep
@IgnoreExtraProperties
data class Room(
    @DocumentId val id: String = "",
    val number: String = "",
    val type: String = RoomType.STANDARD.name,
    val floor: Long = 1L,
    val nightlyRate: Double = 100.0,
    val capacity: Long = 2L,
    val status: String = RoomStatus.AVAILABLE.name,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val roomType: RoomType get() = RoomType.fromRaw(type)
    val roomStatus: RoomStatus get() = RoomStatus.fromRaw(status)
    val floorInt: Int get() = floor.toInt()
    val capacityInt: Int get() = capacity.toInt()
}

@Keep
@IgnoreExtraProperties
data class Reservation(
    @DocumentId val id: String = "",
    val guestName: String = "",
    val guestEmail: String? = null,
    val guestPhone: String? = null,
    val guestDocument: String? = null,
    val roomId: String = "",
    val roomNumber: String = "",
    val roomType: String = RoomType.STANDARD.name,
    val checkInDate: String = "",
    val checkOutDate: String = "",
    val guestsCount: Long = 1L,
    val nightlyRate: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val status: String = ReservationStatus.CONFIRMED.name,
    val specialRequests: String? = null,
    @ServerTimestamp val actualCheckInAt: Timestamp? = null,
    @ServerTimestamp val actualCheckOutAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val reservationStatus: ReservationStatus get() = ReservationStatus.fromRaw(status)
    val roomTypeEnum: RoomType get() = RoomType.fromRaw(roomType)
    val guestsCountInt: Int get() = guestsCount.toInt()
    val nightsCount: Int get() = HotelDateHelper.nightsBetween(checkInDate, checkOutDate)
    val balanceDue: Double get() = (totalAmount - paidAmount).coerceAtLeast(0.0)
}

object HotelDateHelper {
    private val spanishLocale = Locale("es", "ES")

    private fun isoFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    private fun shortDisplayFormat(): SimpleDateFormat =
        SimpleDateFormat("d MMM", spanishLocale)

    private fun fullDisplayFormat(): SimpleDateFormat =
        SimpleDateFormat("EEEE, d 'de' MMMM", spanishLocale)

    private fun timeDisplayFormat(): SimpleDateFormat =
        SimpleDateFormat("HH:mm", spanishLocale)

    fun todayIso(): String = isoFormat().format(Calendar.getInstance().time)

    fun addDaysIso(baseIso: String, days: Int): String {
        val cal = Calendar.getInstance()
        val parsed = runCatching { isoFormat().parse(baseIso) }.getOrNull()
        if (parsed != null) {
            cal.time = parsed
        }
        cal.add(Calendar.DAY_OF_MONTH, days)
        return isoFormat().format(cal.time)
    }

    fun todayOffsetIso(days: Int): String = addDaysIso(todayIso(), days)

    fun isValidIsoDate(value: String): Boolean {
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(value)) return false
        return runCatching { isoFormat().parse(value) != null }.getOrDefault(false)
    }

    fun nightsBetween(startIso: String, endIso: String): Int {
        val start = runCatching { isoFormat().parse(startIso) }.getOrNull() ?: return 1
        val end = runCatching { isoFormat().parse(endIso) }.getOrNull() ?: return 1
        val diffMs = end.time - start.time
        val days = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
        return days.coerceAtLeast(1)
    }

    fun formatShortDate(iso: String): String {
        val parsed = runCatching { isoFormat().parse(iso) }.getOrNull() ?: return iso
        return shortDisplayFormat().format(parsed)
    }

    fun formatTodayHeader(): String {
        val raw = fullDisplayFormat().format(Calendar.getInstance().time)
        return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(spanishLocale) else it.toString() }
    }

    fun formatTimestampTime(ts: Timestamp?): String? {
        val date = ts?.toDate() ?: return null
        return timeDisplayFormat().format(date)
    }

    fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        return formatter.format(amount)
    }
}
