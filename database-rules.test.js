const {
    initializeTestEnvironment,
    assertFails,
    assertSucceeds,
} = require("@firebase/rules-unit-testing");
const fs = require("fs");

describe("Dementia Tester Realtime Database Security Rules", () => {
    let testEnv;

    beforeAll(async () => {
        testEnv = await initializeTestEnvironment({
            projectId: "demo-test-project",
            database: {
                host: "127.0.0.1",
                port: 9000,
                rules: fs.readFileSync("database.rules.json", "utf8"),
            },
        });
    });

    afterAll(async () => {
        if (testEnv) {
            await testEnv.cleanup();
        }
    });

    beforeEach(async () => {
        await testEnv.clearDatabase();
    });

    test("Unauthenticated users cannot read or write data", async () => {
        const unauthDb = testEnv.unauthenticatedContext().database();
        await assertFails(unauthDb.ref("UserProfiles/some_user").get());
    });

    test("A regular user can read and write their own profile and records", async () => {
        // Correct usage: call it on testEnv, passing a callback context
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await context.database().ref("UserProfiles/patient_123").set({ userType: "user" });
        });

        const userContext = testEnv.authenticatedContext("patient_123");
        const db = userContext.database();

        await assertSucceeds(
            db.ref("Reminders/patient_123").set({ title: "Take meds" })
        );
    });

    test("An assigned caregiver can access their assigned patient's data, but unassigned cannot", async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            const db = context.database();
            await db.ref("UserProfiles/caregiver_999").set({ userType: "caregiver" });
            await db.ref("CaregiverPatients/caregiver_999/patient_123").set(true);
            await db.ref("Reminders/patient_123").set({ title: "Doctor visit" });
            await db.ref("Reminders/unassigned_patient_777").set({ title: "Checkup" });
        });

        const caregiverDb = testEnv.authenticatedContext("caregiver_999").database();

        await assertSucceeds(caregiverDb.ref("Reminders/patient_123").get());
        await assertFails(caregiverDb.ref("Reminders/unassigned_patient_777").get());
    });

    test("Participants can read and write to chat rooms, outsiders cannot", async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            const db = context.database();
            await db.ref("chatRooms/room_abc/participants/user_111").set(true);
            await db.ref("chatRooms/room_abc/participants/user_222").set(true);
        });

        const participantDb = testEnv.authenticatedContext("user_111").database();
        const outsiderDb = testEnv.authenticatedContext("outsider_333").database();

        await assertSucceeds(participantDb.ref("chatRooms/room_abc").get());
        await assertSucceeds(
            participantDb.ref("chatRooms/room_abc/messages/msg_1").set({
                senderId: "user_111",
                text: "Hello",
                timestamp: Date.now()
            })
        );

        await assertFails(outsiderDb.ref("chatRooms/room_abc").get());
    });
});