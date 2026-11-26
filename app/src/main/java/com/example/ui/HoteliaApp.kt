package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.HotelDateHelper
import com.example.data.HotelRepository
import com.example.data.Reservation
import com.example.data.Room
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.AccountSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReservationsScreen
import com.example.ui.screens.RoomsScreen
import com.example.ui.sheets.CheckInSheet
import com.example.ui.sheets.CheckOutSheet
import com.example.ui.sheets.ReservationDetailSheet
import com.example.ui.sheets.ReservationFormSheet
import com.example.ui.sheets.RoomFormSheet
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_RESERVATIONS = "reservations"
const val ROUTE_ROOMS = "rooms"

private data class HotelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val hotelDestinations = listOf(
    HotelDestination(
        route = ROUTE_DASHBOARD,
        label = "Hoy",
        selectedIcon = Icons.Filled.Today,
        unselectedIcon = Icons.Outlined.Today
    ),
    HotelDestination(
        route = ROUTE_RESERVATIONS,
        label = "Reservas",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    ),
    HotelDestination(
        route = ROUTE_ROOMS,
        label = "Habitaciones",
        selectedIcon = Icons.Filled.Bed,
        unselectedIcon = Icons.Outlined.Bed
    )
)

private sealed interface ActiveSheet {
    data object Account : ActiveSheet
    data class CheckIn(val reservation: Reservation) : ActiveSheet
    data class CheckOut(val reservation: Reservation) : ActiveSheet
    data class ReservationDetail(val reservation: Reservation) : ActiveSheet
    data class ReservationForm(
        val existing: Reservation? = null,
        val preselectedRoomId: String? = null
    ) : ActiveSheet
    data class RoomForm(val existing: Room? = null) : ActiveSheet
}

@Composable
fun HoteliaApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            AuthenticatedHotelContent(
                user = user,
                currentUserId = user.uid
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedHotelContent(
    user: FirebaseUser,
    currentUserId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: HotelViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                HotelViewModel(HotelRepository(db), currentUserId)
            }
        }
    )

    val dashboardUiState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD
    var activeSheet by remember { mutableStateOf<ActiveSheet?>(null) }
    val searchFocusRequester = remember { FocusRequester() }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val todaySubtitle = remember { HotelDateHelper.formatTodayHeader() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    keyEvent.key == Key.Escape && activeSheet != null -> {
                        activeSheet = null
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.One -> {
                        navigateTo(ROUTE_DASHBOARD)
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.Two -> {
                        navigateTo(ROUTE_RESERVATIONS)
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.Three -> {
                        navigateTo(ROUTE_ROOMS)
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.N -> {
                        val data = (dashboardUiState as? UiState.Success)?.data
                        if (data != null) {
                            activeSheet = if (currentRoute == ROUTE_ROOMS || data.rooms.isEmpty()) {
                                ActiveSheet.RoomForm(null)
                            } else {
                                ActiveSheet.ReservationForm(null)
                            }
                            true
                        } else {
                            false
                        }
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.F -> {
                        if (currentRoute != ROUTE_RESERVATIONS) {
                            navigateTo(ROUTE_RESERVATIONS)
                        }
                        scope.launch {
                            delay(80)
                            runCatching { searchFocusRequester.requestFocus() }
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        val useNavigationRail = maxWidth >= 700.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Spacer(modifier = Modifier.size(12.dp))
                    hotelDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTo(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand)
                                .testTag("nav_rail_${dest.route}")
                        )
                    }
                }
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        title = {
                            Column {
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = todaySubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { activeSheet = ActiveSheet.Account },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .testTag("account_button")
                            ) {
                                val initial = (user.displayName ?: user.email ?: "H")
                                    .firstOrNull()?.uppercaseChar() ?: 'H'
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initial.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            hotelDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("nav_bar_${dest.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    val data = (dashboardUiState as? UiState.Success)?.data
                    if (data != null) {
                        val isRoomsTab = currentRoute == ROUTE_ROOMS
                        ExtendedFloatingActionButton(
                            onClick = {
                                activeSheet = if (isRoomsTab || data.rooms.isEmpty()) {
                                    ActiveSheet.RoomForm(null)
                                } else {
                                    ActiveSheet.ReservationForm(null)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            text = {
                                Text(
                                    if (isRoomsTab || data.rooms.isEmpty()) "Habitación" else "Nueva reserva"
                                )
                            },
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand)
                                .testTag("primary_fab")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (val state = dashboardUiState) {
                        UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Sincronizando recepción...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "No se pudo conectar con Firestore",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { signOut(credentialManager, scope) }
                                    ) {
                                        Text("Volver a iniciar sesión")
                                    }
                                }
                            }
                        }

                        is UiState.Success -> {
                            val data = state.data
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_DASHBOARD,
                                modifier = Modifier
                                    .widthIn(max = 1280.dp)
                                    .fillMaxHeight()
                            ) {
                                composable(ROUTE_DASHBOARD) {
                                    DashboardScreen(
                                        dashboard = data,
                                        isBusy = isBusy,
                                        onSeedStarterHotel = { viewModel.seedStarterHotel() },
                                        onAddFirstRoom = { activeSheet = ActiveSheet.RoomForm(null) },
                                        onReservationClick = { res ->
                                            activeSheet = ActiveSheet.ReservationDetail(res)
                                        },
                                        onCheckInClick = { res ->
                                            activeSheet = ActiveSheet.CheckIn(res)
                                        },
                                        onCheckOutClick = { res ->
                                            activeSheet = ActiveSheet.CheckOut(res)
                                        },
                                        onEditReservation = { res ->
                                            activeSheet = ActiveSheet.ReservationForm(res)
                                        },
                                        onCancelReservation = { res ->
                                            viewModel.cancelReservation(res)
                                        },
                                        onDeleteReservation = { res ->
                                            viewModel.deleteReservation(res)
                                        },
                                        onMarkRoomAvailable = { room ->
                                            viewModel.updateRoomStatus(room, com.example.data.RoomStatus.AVAILABLE)
                                        },
                                        onOpenRoomsInventory = { navigateTo(ROUTE_ROOMS) }
                                    )
                                }
                                composable(ROUTE_RESERVATIONS) {
                                    ReservationsScreen(
                                        reservations = data.reservations,
                                        hasRooms = data.rooms.isNotEmpty(),
                                        isBusy = isBusy,
                                        searchFocusRequester = searchFocusRequester,
                                        onCreateReservation = { activeSheet = ActiveSheet.ReservationForm(null) },
                                        onReservationClick = { res ->
                                            activeSheet = ActiveSheet.ReservationDetail(res)
                                        },
                                        onCheckInClick = { res ->
                                            activeSheet = ActiveSheet.CheckIn(res)
                                        },
                                        onCheckOutClick = { res ->
                                            activeSheet = ActiveSheet.CheckOut(res)
                                        },
                                        onEditReservation = { res ->
                                            activeSheet = ActiveSheet.ReservationForm(res)
                                        },
                                        onCancelReservation = { res ->
                                            viewModel.cancelReservation(res)
                                        },
                                        onDeleteReservation = { res ->
                                            viewModel.deleteReservation(res)
                                        }
                                    )
                                }
                                composable(ROUTE_ROOMS) {
                                    RoomsScreen(
                                        rooms = data.rooms,
                                        reservations = data.reservations,
                                        isBusy = isBusy,
                                        onSeedStarterHotel = { viewModel.seedStarterHotel() },
                                        onAddRoom = { activeSheet = ActiveSheet.RoomForm(null) },
                                        onRoomClick = { room ->
                                            activeSheet = ActiveSheet.RoomForm(room)
                                        },
                                        onChangeRoomStatus = { room, status ->
                                            viewModel.updateRoomStatus(room, status)
                                        },
                                        onNewReservationForRoom = { room ->
                                            activeSheet = ActiveSheet.ReservationForm(
                                                existing = null,
                                                preselectedRoomId = room.id
                                            )
                                        },
                                        onOpenReservation = { res ->
                                            activeSheet = ActiveSheet.ReservationDetail(res)
                                        }
                                    )
                                }
                            }

                            when (val sheet = activeSheet) {
                                null -> Unit
                                ActiveSheet.Account -> {
                                    AccountSheet(
                                        user = user,
                                        hasRooms = data.rooms.isNotEmpty(),
                                        isBusy = isBusy,
                                        onSeedStarterHotel = { viewModel.seedStarterHotel() },
                                        onSignOut = { signOut(credentialManager, scope) },
                                        onDismiss = { activeSheet = null }
                                    )
                                }

                                is ActiveSheet.CheckIn -> {
                                    CheckInSheet(
                                        reservation = sheet.reservation,
                                        isBusy = isBusy,
                                        onConfirmCheckIn = { extraPayment ->
                                            viewModel.performCheckIn(sheet.reservation, extraPayment)
                                            activeSheet = null
                                        },
                                        onDismiss = { activeSheet = null }
                                    )
                                }

                                is ActiveSheet.CheckOut -> {
                                    CheckOutSheet(
                                        reservation = sheet.reservation,
                                        isBusy = isBusy,
                                        onConfirmCheckOut = { finalPayment ->
                                            viewModel.performCheckOut(sheet.reservation, finalPayment)
                                            activeSheet = null
                                        },
                                        onDismiss = { activeSheet = null }
                                    )
                                }

                                is ActiveSheet.ReservationDetail -> {
                                    val freshRes = data.reservations.firstOrNull { it.id == sheet.reservation.id }
                                        ?: sheet.reservation
                                    ReservationDetailSheet(
                                        reservation = freshRes,
                                        isBusy = isBusy,
                                        onCheckIn = { activeSheet = ActiveSheet.CheckIn(freshRes) },
                                        onCheckOut = { activeSheet = ActiveSheet.CheckOut(freshRes) },
                                        onEdit = { activeSheet = ActiveSheet.ReservationForm(freshRes) },
                                        onCancelReservation = {
                                            viewModel.cancelReservation(freshRes)
                                            activeSheet = null
                                        },
                                        onDeleteReservation = {
                                            viewModel.deleteReservation(freshRes)
                                            activeSheet = null
                                        },
                                        onDismiss = { activeSheet = null }
                                    )
                                }

                                is ActiveSheet.ReservationForm -> {
                                    ReservationFormSheet(
                                        existing = sheet.existing,
                                        rooms = data.rooms,
                                        isBusy = isBusy,
                                        preselectedRoomId = sheet.preselectedRoomId,
                                        onSave = { guestName, email, phone, doc, room, checkIn, checkOut, guests, paid, notes, walkIn ->
                                            val editing = sheet.existing
                                            if (editing == null) {
                                                viewModel.createReservation(
                                                    guestName = guestName,
                                                    guestEmail = email,
                                                    guestPhone = phone,
                                                    guestDocument = doc,
                                                    room = room,
                                                    checkInDate = checkIn,
                                                    checkOutDate = checkOut,
                                                    guestsCount = guests,
                                                    paidAmount = paid,
                                                    specialRequests = notes,
                                                    checkInImmediately = walkIn
                                                )
                                            } else {
                                                viewModel.updateReservation(
                                                    reservationId = editing.id,
                                                    guestName = guestName,
                                                    guestEmail = email,
                                                    guestPhone = phone,
                                                    guestDocument = doc,
                                                    room = room,
                                                    checkInDate = checkIn,
                                                    checkOutDate = checkOut,
                                                    guestsCount = guests,
                                                    paidAmount = paid,
                                                    specialRequests = notes
                                                )
                                            }
                                            activeSheet = null
                                        },
                                        onDismiss = { activeSheet = null }
                                    )
                                }

                                is ActiveSheet.RoomForm -> {
                                    RoomFormSheet(
                                        existing = sheet.existing,
                                        isBusy = isBusy,
                                        onSave = { number, type, floor, rate, capacity, status, notes ->
                                            val editing = sheet.existing
                                            if (editing == null) {
                                                viewModel.createRoom(number, type, floor, rate, capacity, status, notes)
                                            } else {
                                                viewModel.updateRoom(editing.id, number, type, floor, rate, capacity, status, notes)
                                            }
                                            activeSheet = null
                                        },
                                        onDelete = sheet.existing?.let { roomToDelete ->
                                            {
                                                viewModel.deleteRoom(roomToDelete)
                                                activeSheet = null
                                            }
                                        },
                                        onDismiss = { activeSheet = null }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
