const fs = require("node:fs");
const path = require("node:path");
const { after, before, beforeEach, test } = require("node:test");

const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} = require("@firebase/rules-unit-testing");
const { get, ref, set, update } = require("firebase/database");


const PROJECT_ID = "demo-avora-alidogukan";
const DEVICE_ID = "avora-001";
const OWNER_UID = "owner-user-001";
const OTHER_UID = "other-user-001";
const RULES_PATH = path.join(__dirname, "..", "firebase-database.rules.json");

let testEnvironment;

function authenticatedDatabase(uid, deviceId = DEVICE_ID) {
  return testEnvironment.authenticatedContext(uid, {
    avora_device_id: deviceId,
  }).database();
}

function unclaimedDatabase(uid) {
  return testEnvironment.authenticatedContext(uid).database();
}

function validFeedback(id, uid = OWNER_UID) {
  return {
    id,
    type: "problem",
    area: "general",
    area_label: "Genel uygulama",
    subject: "Deneme geri bildirimi",
    description: "Bu açıklama güvenlik kuralı testi için yeterince uzundur.",
    contact_email: "owner@example.com",
    diagnostics: {
      app_version: "2.10.0",
      android_version: "16",
      android_sdk: 36,
      manufacturer: "Google",
      model: "Pixel 7 Pro",
    },
    status: "new",
    created_at: Date.now(),
    device_id: DEVICE_ID,
    source: "android",
    user_id: uid,
  };
}

function validGrowthPhoto(id, overrides = {}) {
  return {
    id,
    zone_id: "zone-001",
    season_id: "season-zone-001-2026",
    note: "Aynı açıdan gelişim takibi",
    related_application_id: "plant_assistant",
    analysis_title: "Gelişim dengeli görünüyor",
    analysis_meta: "Görsel güveni %87",
    analysis_context: "Yaprak yoğunluğu ve gövde dengesi değerlendirildi.",
    analysis_advice: "Üç gün sonra aynı açıdan tekrar fotoğraf çekin.",
    analysis_goal: "growth_status",
    analysis_confidence: 87,
    growth_score: 76,
    growth_stage: "Vejetatif gelişim",
    growth_trend: "FIRST_RECORD",
    growth_score_delta: 0,
    growth_signals: "Yeni yaprak oluşumu\nCanlı yaprak rengi",
    growth_previous_captured_at_epoch: 0,
    captured_at_epoch: 1788271200,
    photo_kept_on_owner_phone: true,
    metadata_updated_at_epoch: 1788271260,
    ...overrides,
  };
}

before(async () => {
  testEnvironment = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    database: {
      rules: fs.readFileSync(RULES_PATH, "utf8"),
    },
  });
});

beforeEach(async () => {
  await testEnvironment.clearDatabase();
});

after(async () => {
  await testEnvironment.cleanup();
});

test("only the claimed device owner can read or write the device", async () => {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await set(ref(context.database(), `devices/${DEVICE_ID}/status`), {
      online: true,
    });
  });

  const owner = authenticatedDatabase(OWNER_UID);
  const otherDevice = authenticatedDatabase(OWNER_UID, "avora-002");
  const otherUser = unclaimedDatabase(OTHER_UID);
  const anonymous = testEnvironment.unauthenticatedContext().database();

  await assertSucceeds(get(ref(owner, `devices/${DEVICE_ID}`)));
  await assertFails(get(ref(otherDevice, `devices/${DEVICE_ID}`)));
  await assertFails(get(ref(otherUser, `devices/${DEVICE_ID}`)));
  await assertFails(get(ref(anonymous, `devices/${DEVICE_ID}`)));
  await assertSucceeds(
    update(ref(owner, `devices/${DEVICE_ID}/commands`), { auto_mode: true }),
  );
  await assertFails(
    update(ref(otherUser, `devices/${DEVICE_ID}/commands`), {
      auto_mode: false,
    }),
  );
});

test("owner can create only a bounded feedback schema", async () => {
  const owner = authenticatedDatabase(OWNER_UID);
  const validId = "123e4567-e89b-12d3-a456-426614174000";
  const validPath = `devices/${DEVICE_ID}/user_feedback/${validId}`;

  await assertSucceeds(set(ref(owner, validPath), validFeedback(validId)));

  const extraId = "123e4567-e89b-12d3-a456-426614174001";
  const extra = validFeedback(extraId);
  extra.api_key = "must-not-be-accepted";
  await assertFails(
    set(
      ref(owner, `devices/${DEVICE_ID}/user_feedback/${extraId}`),
      extra,
    ),
  );

  const wrongUserId = "123e4567-e89b-12d3-a456-426614174002";
  await assertFails(
    set(
      ref(owner, `devices/${DEVICE_ID}/user_feedback/${wrongUserId}`),
      validFeedback(wrongUserId, OTHER_UID),
    ),
  );

  const deliveryId = "123e4567-e89b-12d3-a456-426614174003";
  const forgedDelivery = validFeedback(deliveryId);
  forgedDelivery.email_delivery = { status: "sent" };
  await assertFails(
    set(
      ref(owner, `devices/${DEVICE_ID}/user_feedback/${deliveryId}`),
      forgedDelivery,
    ),
  );
});

test("feedback cannot be edited or submitted by an unclaimed user", async () => {
  const owner = authenticatedDatabase(OWNER_UID);
  const otherUser = unclaimedDatabase(OTHER_UID);
  const id = "123e4567-e89b-12d3-a456-426614174004";
  const feedbackPath = `devices/${DEVICE_ID}/user_feedback/${id}`;

  await assertSucceeds(set(ref(owner, feedbackPath), validFeedback(id)));
  await assertFails(update(ref(owner, feedbackPath), { subject: "Değiştirildi" }));
  await assertFails(
    set(
      ref(otherUser, `devices/${DEVICE_ID}/user_feedback/${id}-other`),
      validFeedback(id),
    ),
  );
  await assertSucceeds(set(ref(owner, feedbackPath), null));
});

test("growth photo metadata accepts only the bounded owner schema", async () => {
  const owner = authenticatedDatabase(OWNER_UID);
  const otherUser = unclaimedDatabase(OTHER_UID);
  const validId = "growth-photo-001";
  const basePath = `devices/${DEVICE_ID}/garden_journal/photo_metadata`;

  await assertSucceeds(
    set(ref(owner, `${basePath}/${validId}`), validGrowthPhoto(validId)),
  );

  const incompleteId = "growth-photo-incomplete";
  const incomplete = validGrowthPhoto(incompleteId, {
    growth_score: -1,
    growth_stage: "",
    growth_trend: "",
    growth_score_delta: 0,
    growth_signals: "",
    growth_previous_captured_at_epoch: 0,
  });
  await assertSucceeds(
    set(ref(owner, `${basePath}/${incompleteId}`), incomplete),
  );
  await assertFails(
    set(
      ref(otherUser, `${basePath}/growth-photo-other`),
      validGrowthPhoto("growth-photo-other"),
    ),
  );

  const outOfRange = validGrowthPhoto("growth-photo-score", {
    growth_score: 101,
  });
  await assertFails(set(ref(owner, `${basePath}/${outOfRange.id}`), outOfRange));

  const mismatchedGoal = validGrowthPhoto("growth-photo-health", {
    analysis_goal: "health_screening",
  });
  await assertFails(
    set(ref(owner, `${basePath}/${mismatchedGoal.id}`), mismatchedGoal),
  );

  const oversizedSignals = validGrowthPhoto("growth-photo-signals", {
    growth_signals: "x".repeat(1001),
  });
  await assertFails(
    set(ref(owner, `${basePath}/${oversizedSignals.id}`), oversizedSignals),
  );

  const unknownField = validGrowthPhoto("growth-photo-secret");
  unknownField.api_key = "must-not-be-accepted";
  await assertFails(
    set(ref(owner, `${basePath}/${unknownField.id}`), unknownField),
  );

  await assertSucceeds(set(ref(owner, `${basePath}/${validId}`), null));
});

test("backend delivery state remains read-only to the Android owner", async () => {
  const owner = authenticatedDatabase(OWNER_UID);
  const id = "123e4567-e89b-12d3-a456-426614174005";
  const feedbackPath = `devices/${DEVICE_ID}/user_feedback/${id}`;

  await assertSucceeds(set(ref(owner, feedbackPath), validFeedback(id)));
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    const database = context.database();
    await set(ref(database, `${feedbackPath}/email_delivery`), {
      status: "sent",
      sender: "raspberry_pi",
    });
    await set(
      ref(database, `devices/${DEVICE_ID}/user_feedback_email_state`),
      { activation_epoch: 123456 },
    );
    await set(
      ref(database, `devices/${DEVICE_ID}/user_feedback/legacy-feedback`),
      {
        type: "problem",
        subject: "Eski kayıt",
        created_at: 1,
      },
    );
  });

  await assertFails(
    update(ref(owner, `${feedbackPath}/email_delivery`), { status: "failed" }),
  );
  await assertFails(
    update(ref(owner, `devices/${DEVICE_ID}/user_feedback_email_state`), {
      activation_epoch: 0,
    }),
  );

  const snapshot = await get(ref(owner, `devices/${DEVICE_ID}`));
  const unchangedDevice = snapshot.val();
  unchangedDevice.settings = { language: "tr" };
  await assertSucceeds(set(ref(owner, `devices/${DEVICE_ID}`), unchangedDevice));
});
