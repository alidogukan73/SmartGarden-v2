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
