const {
    initializeTestEnvironment,
    assertFails,
    assertSucceeds,
} = require("@firebase/rules-unit-testing");
const fs = require("fs");

describe("Dementia Tester Chat Security Rules", () => {
    let testEnv;

    beforeAll(async () => {
        testEnv = await initializeTestEnvironment({
            projectId: "dementia-tester-test",
            firestore: {
                rules: fs.readFileSync("firestore.rules", "utf8"),
            },
        });
    });

    afterAll(async () => {
        await testEnv.cleanup();
    });

    beforeEach(async () => {
        await testEnv.clearFirestore();
    });

    test("Base customer cannot read a chat room they are not a participant in", async () => {
        const customerContext = testEnv.authenticatedContext("customer_uid_123");
        const db = customerContext.firestore();

        // Attempting to read a room where they are missing from participants
        await assertFails(
            db.collection("chatRooms").doc("room_unauthorized").get()
        );
    });

    test("Elevated/Doctor or participant can read/write messages in their room", async () => {
        const customerContext = testEnv.authenticatedContext("customer_uid_123");

        // Pre-seed a room where customer is a participant
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await context.firestore().collection("chatRooms").doc("room_01").set({
                participants: ["customer_uid_123", "doctor_uid_456"]
            });
        });

        const db = customerContext.firestore();

        // Customer should be able to send a message
        await assertSucceeds(
            db.collection("chatRooms").doc("room_01").collection("messages").add({
                senderId: "customer_uid_123",
                text: "Hello doctor",
                timestamp: Date.now()
            })
        );
    });
});