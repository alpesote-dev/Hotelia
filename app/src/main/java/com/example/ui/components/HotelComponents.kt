package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.KingBed
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SingleBed
import androidx.compose.material.icons.outlined.Villa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.HotelDateHelper
import com.example.data.Reservation
import com.example.data.ReservationStatus
import com.example.data.Room
import com.example.data.RoomStatus
import com.example.data.RoomType
import com.example.ui.HotelDashboardData
import com.example.ui.theme.StatusAvailableBg
import com.example.ui.theme.StatusAvailableText
import com.example.ui.theme.StatusCleaningBg
import com.example.ui.theme.StatusCleaningText
import com.example.ui.theme.StatusConfirmedBg
import com.example.ui.theme.StatusConfirmedText
import com.example.ui.theme.StatusMaintenanceBg
import com.example.ui.theme.StatusMaintenanceText
import com.example.ui.theme.StatusOccupiedBg
import com.example.ui.theme.StatusOccupiedText
import com.google.firebase.auth.FirebaseUser

@Composable
fun RoomStatusPill(status: RoomStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        RoomStatus.AVAILABLE -> StatusAvailableBg to StatusAvailableText
        RoomStatus.OCCUPIED -> StatusOccupiedBg to StatusOccupiedText
        RoomStatus.CLEANING -> StatusCleaningBg to StatusCleaningText
        RoomStatus.MAINTENANCE -> StatusMaintenanceBg to StatusMaintenanceText
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Text(
                text = status.labelEs,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

@Composable
fun RoomTypeBadge(roomType: RoomType, modifier: Modifier = Modifier) {
    val icon = when (roomType) {
        RoomType.INDIVIDUAL -> Icons.Outlined.SingleBed
        RoomType.DOBLE -> Icons.Outlined.KingBed
        RoomType.SUITE -> Icons.Outlined.Villa
    }
    val (bg, fg) = when (roomType) {
        RoomType.INDIVIDUAL -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
        RoomType.DOBLE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) to MaterialTheme.colorScheme.onPrimaryContainer
        RoomType.SUITE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = roomType.labelEs,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

@Composable
fun RoomInventorySummaryCard(
    rooms: List<Room>,
    modifier: Modifier = Modifier
) {
    val availableCount = remember(rooms) { rooms.count { it.roomStatus == RoomStatus.AVAILABLE } }
    val occupiedCount = remember(rooms) { rooms.count { it.roomStatus == RoomStatus.OCCUPIED } }
    val cleaningCount = remember(rooms) { rooms.count { it.roomStatus == RoomStatus.CLEANING } }
    val avgRate = remember(rooms) {
        if (rooms.isEmpty()) 0.0 else rooms.sumOf { it.nightlyRate } / rooms.size
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("room_inventory_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inventario de habitaciones",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${rooms.size} habitaciones · Tarifa media ${HotelDateHelper.formatCurrency(avgRate)}/noche",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = StatusAvailableBg,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDone,
                            contentDescription = null,
                            tint = StatusAvailableText,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Firestore",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusAvailableText
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InventoryStatItem(
                    label = "Disponibles",
                    count = availableCount,
                    tint = StatusAvailableText,
                    modifier = Modifier.weight(1f)
                )
                InventoryStatItem(
                    label = "Ocupadas",
                    count = occupiedCount,
                    tint = StatusOccupiedText,
                    modifier = Modifier.weight(1f)
                )
                InventoryStatItem(
                    label = "En limpieza",
                    count = cleaningCount,
                    tint = StatusCleaningText,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InventoryStatItem(
    label: String,
    count: Int,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ReservationStatusPill(status: ReservationStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        ReservationStatus.CONFIRMED -> StatusConfirmedBg to StatusConfirmedText
        ReservationStatus.CHECKED_IN -> StatusAvailableBg to StatusAvailableText
        ReservationStatus.CHECKED_OUT -> StatusMaintenanceBg to StatusMaintenanceText
        ReservationStatus.CANCELLED -> StatusOccupiedBg to StatusOccupiedText
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = status.labelEs,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun OccupancyOverviewCard(
    dashboard: HotelDashboardData,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF215146)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Estado del hotel hoy",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFD8B36A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${dashboard.occupancyRatePercent}% ocupación",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${dashboard.occupiedRoomsCount} de ${dashboard.rooms.size} habitaciones ocupadas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }

                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = (dashboard.occupancyRatePercent / 100f).coerceIn(0f, 1f)
                        Canvas(modifier = Modifier.size(68.dp)) {
                            val stroke = 8.dp.toPx()
                            val arcSize = Size(size.width - stroke, size.height - stroke)
                            val topLeft = Offset(stroke / 2f, stroke / 2f)
                            drawArc(
                                color = Color.White.copy(alpha = 0.18f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = Color(0xFFD8B36A),
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "${dashboard.availableRoomsCount}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.16f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricChip(label = "Libres", value = dashboard.availableRoomsCount.toString())
                    MetricChip(label = "Llegadas", value = dashboard.pendingCheckInsToday.size.toString())
                    MetricChip(label = "Salidas", value = dashboard.pendingCheckOutsToday.size.toString())
                    MetricChip(label = "Limpieza", value = dashboard.cleaningRoomsCount.toString())
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f)
        )
    }
}

enum class ReservationCardMode {
    CHECK_IN_QUEUE,
    CHECK_OUT_QUEUE,
    GENERAL
}

@Composable
fun ReservationActionCard(
    reservation: Reservation,
    mode: ReservationCardMode,
    onCardClick: () -> Unit,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val status = reservation.reservationStatus
    val balanceDue = reservation.balanceDue
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val (containerColor, borderStroke) = rememberDesktopCardAppearance(
        isSelected = isSelected,
        interactionSource = interactionSource
    )

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderStroke,
        modifier = modifier
            .fillMaxWidth()
            .desktopPointerFeedback(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onCardClick
            )
            .testTag("reservation_card_${reservation.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "HAB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = reservation.roomNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reservation.guestName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${reservation.roomTypeEnum.labelEs} · ${reservation.guestsCountInt} huésp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                ReservationStatusPill(status = status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${HotelDateHelper.formatShortDate(reservation.checkInDate)} → ${HotelDateHelper.formatShortDate(reservation.checkOutDate)} (${reservation.nightsCount}n)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = if (balanceDue > 0.01) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (balanceDue > 0.01) {
                            "Saldo ${HotelDateHelper.formatCurrency(balanceDue)}"
                        } else {
                            "Pagado"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (balanceDue > 0.01) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!reservation.specialRequests.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = reservation.specialRequests,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            if (mode == ReservationCardMode.CHECK_IN_QUEUE || (mode == ReservationCardMode.GENERAL && status == ReservationStatus.CONFIRMED)) {
                Button(
                    onClick = onCheckInClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("action_check_in_${reservation.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registrar Check-in", style = MaterialTheme.typography.labelLarge)
                }
            } else if (mode == ReservationCardMode.CHECK_OUT_QUEUE || (mode == ReservationCardMode.GENERAL && status == ReservationStatus.CHECKED_IN)) {
                Button(
                    onClick = onCheckOutClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("action_check_out_${reservation.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (balanceDue > 0.01) {
                            "Check-out (${HotelDateHelper.formatCurrency(balanceDue)})"
                        } else {
                            "Completar Check-out"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun RoomGridCard(
    room: Room,
    onCardClick: () -> Unit,
    onMarkAvailableClick: () -> Unit,
    isSelected: Boolean = false,
    activeGuestName: String? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val (containerColor, borderStroke) = rememberDesktopCardAppearance(
        isSelected = isSelected,
        interactionSource = interactionSource
    )

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .desktopPointerFeedback(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onCardClick
            )
            .testTag("room_card_${room.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "HAB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                            )
                            Text(
                                text = room.number,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RoomTypeBadge(roomType = room.roomType)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Piso ${room.floorInt}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${room.capacityInt} pers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                RoomStatusPill(status = room.roomStatus)
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Precio por noche",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${HotelDateHelper.formatCurrency(room.nightlyRate)} / noche",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!activeGuestName.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = StatusOccupiedText,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Huésped: $activeGuestName",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!room.notes.isNullOrBlank()) {
                Text(
                    text = room.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (room.roomStatus == RoomStatus.CLEANING || room.roomStatus == RoomStatus.MAINTENANCE) {
                FilledTonalButton(
                    onClick = onMarkAvailableClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("room_ready_${room.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Marcar disponible", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun EmptyHotelState(
    title: String,
    subtitle: String,
    primaryButtonText: String?,
    onPrimaryClick: (() -> Unit)?,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    isBusy: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_hotel_key),
                contentDescription = "Llave de hotel sobre lino",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (primaryButtonText != null && onPrimaryClick != null) {
                Button(
                    onClick = onPrimaryClick,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("empty_primary_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(primaryButtonText)
                }
            }
            if (secondaryButtonText != null && onSecondaryClick != null) {
                OutlinedButton(
                    onClick = onSecondaryClick,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("empty_secondary_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(secondaryButtonText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(
    user: FirebaseUser?,
    hasRooms: Boolean,
    isBusy: Boolean,
    onSeedStarterHotel: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = (user?.displayName ?: user?.email ?: "H").firstOrNull()?.uppercaseChar() ?: 'H'
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = user?.displayName?.takeIf { it.isNotBlank() } ?: "Gerencia Hotelia",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = user?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            KeyboardShortcutsCard()

            if (!hasRooms) {
                FilledTonalButton(
                    onClick = {
                        onSeedStarterHotel()
                        onDismiss()
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("sheet_seed_hotel_button")
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargar hotel de inicio (8 habitaciones)")
                }
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onSignOut()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }
}
