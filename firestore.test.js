const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("predictions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("subscriptions").get());
});

test("UserProfile: owner can create and update within bounds; cross-user and ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice FC",
      accuracyPoints: 0,
      totalPredictions: 0,
      correctPredictions: 0,
      currentStreak: 0,
      adBoostTokens: 1,
      notificationsEnabled: true,
      goalAlertsEnabled: true,
      kickoffAlertsEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Update notification preferences
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      goalAlertsEnabled: false,
      updatedAt: pastDate(1000),
    })
  );

  // Update stats / boost tokens
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      accuracyPoints: 25,
      totalPredictions: 1,
      correctPredictions: 1,
      currentStreak: 1,
      adBoostTokens: 2,
      updatedAt: pastDate(500),
    })
  );

  // Cross-user read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: pastDate(500) }));

  // Ghost field / immutable createdAt mutation rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(200),
      updatedAt: pastDate(200),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      isAdmin: true,
      accuracyPoints: 0,
      totalPredictions: 0,
      correctPredictions: 0,
      currentStreak: 0,
      adBoostTokens: 0,
      notificationsEnabled: true,
      goalAlertsEnabled: true,
      kickoffAlertsEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("MatchPrediction: owner create, pending edit, terminal settlement, and lock enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);
  const predRef = aliceDb.collection("users").doc(ALICE_UID).collection("predictions").doc("match_704382");

  // Create prediction (omits null logo URLs and omits @DocumentId 'id')
  await assertSucceeds(
    predRef.set({
      userId: ALICE_UID,
      matchId: "match_704382",
      leagueCode: "eng.1",
      leagueName: "Premier League",
      homeTeam: "Arsenal",
      awayTeam: "Chelsea",
      kickoffIso: "2026-11-24T19:45:00Z",
      pick: "HOME",
      predictedHomeScore: 2,
      predictedAwayScore: 1,
      confidence: "HIGH",
      status: "PENDING",
      pointsAwarded: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // List subcollection succeeds for owner, fails for other user
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("predictions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("predictions").get());

  // Edit pending prediction succeeds
  await assertSucceeds(
    predRef.update({
      pick: "DRAW",
      predictedHomeScore: 1,
      predictedAwayScore: 1,
      confidence: "BOOSTED",
      updatedAt: pastDate(3000),
    })
  );

  // Settle prediction to WON succeeds
  await assertSucceeds(
    predRef.update({
      status: "WON",
      pointsAwarded: 50,
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state locking: subsequent edit on WON prediction must fail
  await assertFails(
    predRef.update({
      pointsAwarded: 100,
      updatedAt: pastDate(500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("predictions").doc("match_999").set({
      userId: ALICE_UID,
      matchId: "match_999",
      leagueCode: "eng.1",
      leagueName: "Premier League",
      homeTeam: "Liverpool",
      awayTeam: "Man City",
      kickoffIso: "2026-11-25T20:00:00Z",
      pick: "HOME",
      predictedHomeScore: 2,
      predictedAwayScore: 1,
      confidence: "STANDARD",
      status: "PENDING",
      pointsAwarded: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("MatchSubscription: owner create, toggle alerts, sync score, cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);
  const subRef = aliceDb.collection("users").doc(ALICE_UID).collection("subscriptions").doc("match_704382");

  await assertSucceeds(
    subRef.set({
      userId: ALICE_UID,
      matchId: "match_704382",
      leagueCode: "eng.1",
      homeTeam: "Arsenal",
      awayTeam: "Chelsea",
      lastKnownHomeScore: 0,
      lastKnownAwayScore: 0,
      lastKnownStatus: "PRE",
      notifyGoals: true,
      notifyKickoff: true,
      notifyFullTime: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    subRef.update({
      lastKnownHomeScore: 1,
      lastKnownAwayScore: 0,
      lastKnownStatus: "LIVE",
      updatedAt: pastDate(1000),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subscriptions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subscriptions").doc("match_704382").delete());
});
