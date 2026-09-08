package org.example.dementia_tester_app.data

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseDatabase.FIRDatabase
import cocoapods.FirebaseDatabase.FIRServerValue
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class ChatService actual constructor() : ChatServiceInterface {

    actual override fun getCurrentUserId(): String? {
        return FIRAuth.auth()
            ?.currentUser()
            ?.uid()
    }

    private fun databaseReference() =
        FIRDatabase.database()
            ?.reference()

    actual override fun getLatestMessage(
        roomId: String,
        callback: (ChatResult<ChatMessageData?>) -> Unit
    ) {
        val currentUserId =
            getCurrentUserId()

        if (currentUserId == null) {
            callback(
                ChatResult.Error(
                    "User is not signed in"
                )
            )
            return
        }

        val rootRef =
            databaseReference()

        if (rootRef == null) {
            callback(
                ChatResult.Error(
                    "Firebase Database is not initialized"
                )
            )
            return
        }

        val messagesRef =
            rootRef
                .child("chatRooms")
                .child(roomId)
                .child("messages")

        val query =
            messagesRef
                .queryOrderedByChild(
                    "timestamp"
                )
                .queryLimitedToLast(
                    1u
                )

        FirebaseDatabaseIosHelper.observeValueOnce(
            query = query
        ) { snapshot ->

            if (
                getCurrentUserId() !=
                currentUserId
            ) {
                callback(
                    ChatResult.Error(
                        "User session changed"
                    )
                )
                return@observeValueOnce
            }

            if (snapshot == null) {
                callback(
                    ChatResult.Error(
                        "Failed to read latest message"
                    )
                )
                return@observeValueOnce
            }

            val snapshotValue =
                snapshot.value

            if (
                snapshotValue !is Map<*, *>
            ) {
                callback(
                    ChatResult.Success(
                        null
                    )
                )
                return@observeValueOnce
            }

            var latestMessage:
                    ChatMessageData? =
                null

            for (
            (_, rawMessage)
            in snapshotValue
            ) {
                val message =
                    rawMessage as? Map<*, *>
                        ?: continue

                val text =
                    message["text"]
                            as? String
                        ?: ""

                val senderId =
                    message["senderId"]
                            as? String
                        ?: ""

                latestMessage =
                    ChatMessageData(
                        text = text,
                        senderId = senderId
                    )
            }

            callback(
                ChatResult.Success(
                    latestMessage
                )
            )
        }
    }

    actual override fun getMessages(
        roomId: String,
        callback: (ChatResult<List<ChatMessageData>>) -> Unit
    ) {
        val currentUserId =
            getCurrentUserId()

        if (currentUserId == null) {
            callback(
                ChatResult.Error(
                    "User is not signed in"
                )
            )
            return
        }

        val rootRef =
            databaseReference()

        if (rootRef == null) {
            callback(
                ChatResult.Error(
                    "Firebase Database is not initialized"
                )
            )
            return
        }

        val messagesRef =
            rootRef
                .child("chatRooms")
                .child(roomId)
                .child("messages")

        FirebaseDatabaseIosHelper.observeValueOnce(
            query = messagesRef
        ) { snapshot ->

            if (
                getCurrentUserId() !=
                currentUserId
            ) {
                callback(
                    ChatResult.Error(
                        "User session changed"
                    )
                )
                return@observeValueOnce
            }

            if (snapshot == null) {
                callback(
                    ChatResult.Error(
                        "Failed to read messages"
                    )
                )
                return@observeValueOnce
            }

            val result =
                mutableListOf<ChatMessageData>()

            val snapshotValue =
                snapshot.value

            if (
                snapshotValue is Map<*, *>
            ) {
                for (
                (_, rawMessage)
                in snapshotValue
                ) {
                    val message =
                        rawMessage as? Map<*, *>
                            ?: continue

                    val text =
                        message["text"]
                                as? String
                            ?: ""

                    val senderId =
                        message["senderId"]
                                as? String
                            ?: ""

                    result.add(
                        ChatMessageData(
                            text = text,
                            senderId = senderId
                        )
                    )
                }
            }

            callback(
                ChatResult.Success(
                    result
                )
            )
        }
    }

    actual override fun sendMessage(
        roomId: String,
        recipientId: String,
        text: String,
        callback: (ChatResult<Unit>) -> Unit
    ) {
        val currentUserId =
            getCurrentUserId()

        if (currentUserId == null) {
            callback(
                ChatResult.Error(
                    "User is not signed in"
                )
            )
            return
        }

        val trimmedText =
            text.trim()

        if (trimmedText.isBlank()) {
            callback(
                ChatResult.Error(
                    "Message cannot be empty"
                )
            )
            return
        }

        val rootRef =
            databaseReference()

        if (rootRef == null) {
            callback(
                ChatResult.Error(
                    "Firebase Database is not initialized"
                )
            )
            return
        }

        val roomRef =
            rootRef
                .child("chatRooms")
                .child(roomId)

        val messageRef =
            roomRef
                .child("messages")
                .childByAutoId()

        val messageId =
            messageRef.key()

        if (messageId == null) {
            callback(
                ChatResult.Error(
                    "Failed to create message ID"
                )
            )
            return
        }

        val timestamp =
            FIRServerValue.timestamp()

        val updates:
                Map<Any?, Any?> =
            mapOf(
                "participants/$currentUserId" to
                        true,

                "participants/$recipientId" to
                        true,

                "messages/$messageId/senderId" to
                        currentUserId,

                "messages/$messageId/text" to
                        trimmedText,

                "messages/$messageId/timestamp" to
                        timestamp
            )

        roomRef.updateChildValues(
            values = updates,
            withCompletionBlock = {
                    error,
                    _ ->

                if (error == null) {
                    callback(
                        ChatResult.Success(
                            Unit
                        )
                    )
                } else {
                    callback(
                        ChatResult.Error(
                            error.localizedDescription
                        )
                    )
                }
            }
        )
    }
}