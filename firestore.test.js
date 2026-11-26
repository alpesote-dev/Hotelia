const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_manager_123";
const BOB_UID = "bob_manager_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function validRoomPayload(overrides = {}) {
  const now = pastDate(10000);
  return {
    number: "101",
    type: "DOBLE",
    floor: 1,
    nightlyRate: 145.0,
    capacity: 2,
    status: "AVAILABLE",
    notes: "Vista al jardin",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validReservationPayload(overrides = {}) {
  const now = pastDate(10000);
  return {
    guestName: "Elena Morales",
    guestPhone: "+34 612 345 678",
    guestDocument: "48291034K",
    roomId: "room_101",
    roomNumber: "101",
    roomType: "DOBLE",
    checkInDate: "2026-11-25",
    checkOutDate: "2026-11-28",
    guestsCount: 2,
    nightlyRate: 145.0,
    totalAmount: 435.0,
    paidAmount: 145.0,
    status: "CONFIRMED",
    specialRequests: "Cama king y almohadas extra",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated access: denies reading or writing rooms and reservations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/rooms`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/reservations`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/rooms`).doc("room_101").set(validRoomPayload())
  );
});

test("Cross-user isolation: Bob cannot read, create, update, or delete Alice's rooms or reservations", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection(`users/${ALICE_UID}/rooms`).doc("room_101").set(validRoomPayload());
    await db
      .collection(`users/${ALICE_UID}/reservations`)
      .doc("res_1")
      .set(validReservationPayload());
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/rooms`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/rooms`).doc("room_101").get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/rooms`).doc("room_102").set(validRoomPayload({ number: "102" }))
  );
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/rooms`).doc("room_101").update({ status: "CLEANING", updatedAt: pastDate() })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/reservations`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/reservations`).doc("res_1").delete());
});

test("Rooms: owner can create, list, edit, update status, and cannot delete occupied room", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const roomsCol = aliceDb.collection(`users/${ALICE_UID}/rooms`);

  await assertSucceeds(roomsCol.doc("room_101").set(validRoomPayload()));
  await assertSucceeds(roomsCol.get());

  // Ghost field & @DocumentId 'id' rejection
  await assertFails(
    roomsCol.doc("room_102").set(validRoomPayload({ id: "room_102", number: "102" }))
  );
  await assertFails(
    roomsCol.doc("room_103").set(validRoomPayload({ number: "103", isAdmin: true }))
  );

  // Future timestamp rejected
  await assertFails(
    roomsCol.doc("room_104").set(validRoomPayload({ number: "104", createdAt: new Date(Date.now() + 60000) }))
  );

  // Status update succeeds, immutable createdAt change fails
  await assertSucceeds(
    roomsCol.doc("room_101").update({ status: "OCCUPIED", updatedAt: pastDate(1000) })
  );
  await assertFails(
    roomsCol.doc("room_101").update({ createdAt: pastDate(500), updatedAt: pastDate(500) })
  );

  // Cannot delete OCCUPIED room, can delete AVAILABLE room
  await assertFails(roomsCol.doc("room_101").delete());
  await assertSucceeds(
    roomsCol.doc("room_101").update({ status: "AVAILABLE", updatedAt: pastDate(500) })
  );
  await assertSucceeds(roomsCol.doc("room_101").delete());
});

test("Reservations lifecycle: create -> batch check-in -> batch check-out -> terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const roomsCol = aliceDb.collection(`users/${ALICE_UID}/rooms`);
  const resCol = aliceDb.collection(`users/${ALICE_UID}/reservations`);

  // Orphan reservation without existing room fails
  await assertFails(resCol.doc("res_orphan").set(validReservationPayload({ roomId: "missing_room" })));

  // Create room first
  await assertSucceeds(roomsCol.doc("room_101").set(validRoomPayload()));

  // Invalid date order rejected (checkOutDate <= checkInDate)
  await assertFails(
    resCol.doc("res_bad_dates").set(validReservationPayload({ checkInDate: "2026-11-28", checkOutDate: "2026-11-25" }))
  );

  // Valid reservation create
  await assertSucceeds(resCol.doc("res_1").set(validReservationPayload()));
  await assertSucceeds(resCol.get());

  // State shortcutting: CONFIRMED -> CHECKED_OUT directly must fail
  await assertFails(
    resCol.doc("res_1").update({
      status: "CHECKED_OUT",
      paidAmount: 435.0,
      actualCheckOutAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Atomic Batch Check-In: CONFIRMED -> CHECKED_IN + room AVAILABLE -> OCCUPIED
  const checkInBatch = aliceDb.batch();
  checkInBatch.update(resCol.doc("res_1"), {
    status: "CHECKED_IN",
    paidAmount: 200.0,
    actualCheckInAt: pastDate(1000),
    updatedAt: pastDate(1000),
  });
  checkInBatch.update(roomsCol.doc("room_101"), {
    status: "OCCUPIED",
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(checkInBatch.commit());

  // Cannot delete active CHECKED_IN reservation
  await assertFails(resCol.doc("res_1").delete());

  // Atomic Batch Check-Out: CHECKED_IN -> CHECKED_OUT + room OCCUPIED -> CLEANING
  const checkOutBatch = aliceDb.batch();
  checkOutBatch.update(resCol.doc("res_1"), {
    status: "CHECKED_OUT",
    paidAmount: 435.0,
    actualCheckOutAt: pastDate(500),
    updatedAt: pastDate(500),
  });
  checkOutBatch.update(roomsCol.doc("room_101"), {
    status: "CLEANING",
    updatedAt: pastDate(500),
  });
  await assertSucceeds(checkOutBatch.commit());

  // Terminal state lock: cannot edit or revert CHECKED_OUT reservation
  await assertFails(
    resCol.doc("res_1").update({
      guestName: "Tampered Name",
      updatedAt: pastDate(100),
    })
  );
  await assertFails(
    resCol.doc("res_1").update({
      status: "CONFIRMED",
      updatedAt: pastDate(100),
    })
  );
});

test("Walk-in batch create (status CHECKED_IN) + room OCCUPIED succeeds atomically", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const roomsCol = aliceDb.collection(`users/${ALICE_UID}/rooms`);
  const resCol = aliceDb.collection(`users/${ALICE_UID}/reservations`);

  await assertSucceeds(roomsCol.doc("room_201").set(validRoomPayload({ number: "201", floor: 2 })));

  const walkInBatch = aliceDb.batch();
  walkInBatch.set(
    resCol.doc("res_walkin"),
    validReservationPayload({
      roomId: "room_201",
      roomNumber: "201",
      status: "CHECKED_IN",
      actualCheckInAt: pastDate(1000),
    })
  );
  walkInBatch.update(roomsCol.doc("room_201"), {
    status: "OCCUPIED",
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(walkInBatch.commit());
});

test("Room inventory model: supports INDIVIDUAL/DOBLE/SUITE, statuses AVAILABLE/OCCUPIED/CLEANING, and rejects invalid type/status/nightlyRate", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const roomsCol = aliceDb.collection(`users/${ALICE_UID}/rooms`);

  await assertSucceeds(
    roomsCol.doc("room_ind").set(
      validRoomPayload({
        number: "101",
        type: "INDIVIDUAL",
        nightlyRate: 85.0,
        capacity: 1,
        status: "AVAILABLE",
      })
    )
  );
  await assertSucceeds(
    roomsCol.doc("room_dob").set(
      validRoomPayload({
        number: "202",
        type: "DOBLE",
        floor: 2,
        nightlyRate: 140.0,
        capacity: 2,
        status: "OCCUPIED",
      })
    )
  );
  await assertSucceeds(
    roomsCol.doc("room_sui").set(
      validRoomPayload({
        number: "301",
        type: "SUITE",
        floor: 3,
        nightlyRate: 260.0,
        capacity: 4,
        status: "CLEANING",
      })
    )
  );

  // Full room edit changing type, nightlyRate, and status
  await assertSucceeds(
    roomsCol.doc("room_ind").update({
      number: "101",
      type: "DOBLE",
      floor: 1,
      nightlyRate: 115.0,
      capacity: 2,
      status: "CLEANING",
      updatedAt: pastDate(500),
    })
  );

  // Invalid room type rejected
  await assertFails(
    roomsCol.doc("room_bad_type").set(validRoomPayload({ number: "401", type: "PRESIDENTIAL" }))
  );

  // Invalid room status rejected
  await assertFails(
    roomsCol.doc("room_bad_status").set(validRoomPayload({ number: "402", status: "RESERVED" }))
  );

  // Invalid nightlyRate (<= 0) rejected
  await assertFails(
    roomsCol.doc("room_zero_rate").set(validRoomPayload({ number: "403", nightlyRate: 0 }))
  );
  await assertFails(
    roomsCol.doc("room_neg_rate").set(validRoomPayload({ number: "404", nightlyRate: -50 }))
  );
});
