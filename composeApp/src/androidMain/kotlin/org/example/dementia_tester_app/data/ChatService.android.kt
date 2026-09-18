package org.example.dementia_tester_app.data

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.ServerValue
import com.google.firebase.database.database

actual class ChatService actual constructor() : ChatServiceInterface {

    actual override fun getCurrentUserId(): String? {
        return Firebase.auth.currentUser?.uid
    }

    actual override fun getLatestMessage(
        roomId: String,
        callback: (ChatResult<ChatMessageData?>) -> Unit
    ) {
        val currentUserId = getCurrentUserId()

        if (currentUserId == null) {
            callback(ChatResult.Error("User is not signed in"))
            return
        }

        val messagesRef = Firebase.database.reference
            .child("chatRooms")
            .child(roomId)
            .child("messages")

        messagesRef
            .orderByChild("timestamp")
            .limitToLast(1)
            .get()
            .addOnSuccessListener { snapshot ->
                // Make sure the user has not changed while the request was running.
                if (getCurrentUserId() != currentUserId) {
                    callback(ChatResult.Error("User session changed"))
                    return@addOnSuccessListener
                }

                var latestMessage: ChatMessageData? = null

                for (child in snapshot.children) {
                    val text = child
                        .child("text")
                        .getValue(String::class.java)
                        ?: ""

                    val senderId = child
                        .child("senderId")
                        .getValue(String::class.java)
                        ?: ""

                    latestMessage = ChatMessageData(
                        text = text,
                        senderId = senderId
                    )
                }

                callback(
                    ChatResult.Success(latestMessage)
                )
            }
            .addOnFailureListener { exception ->
                callback(
                    ChatResult.Error(
                        exception.message ?: "Failed to load latest message"
                    )
                )
            }
    }

    actual override fun getMessages(
        roomId: String,
        callback: (ChatResult<List<ChatMessageData>>) -> Unit
    ) {
        val currentUserId = getCurrentUserId()

        if (currentUserId == null) {
            callback(ChatResult.Error("User is not signed in"))
            return
        }

        Firebase.database.reference
            .child("chatRooms")
            .child(roomId)
            .child("messages")
            .get()
            .addOnSuccessListener { snapshot ->
                if (getCurrentUserId() != currentUserId) {
                    callback(ChatResult.Error("User session changed"))
                    return@addOnSuccessListener
                }

                val messages = mutableListOf<ChatMessageData>()

                for (child in snapshot.children) {
                    val text = child
                        .child("text")
                        .getValue(String::class.java)
                        ?: ""

                    val senderId = child
                        .child("senderId")
                        .getValue(String::class.java)
                        ?: ""

                    messages.add(
                        ChatMessageData(
                            text = text,
                            senderId = senderId
                        )
                    )
                }

                callback(
                    ChatResult.Success(messages)
                )
            }
            .addOnFailureListener { exception ->
                callback(
                    ChatResult.Error(
                        exception.message ?: "Failed to load messages"
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
        val currentUserId = getCurrentUserId()

        if (currentUserId == null) {
            callback(ChatResult.Error("User is not signed in"))
            return
        }

        val trimmedText = text.trim()

        if (trimmedText.isBlank()) {
            callback(ChatResult.Error("Message cannot be empty"))
            return
        }

        val roomRef = Firebase.database.reference
            .child("chatRooms")
            .child(roomId)

        val messageId = roomRef
            .child("messages")
            .push()
            .key

        if (messageId == null) {
            callback(ChatResult.Error("Failed to create message ID"))
            return
        }

        val updates = hashMapOf<String, Any>(
            "participants/$currentUserId" to true,
            "participants/$recipientId" to true,
            "messages/$messageId/senderId" to currentUserId,
            "messages/$messageId/text" to trimmedText,
            "messages/$messageId/timestamp" to ServerValue.TIMESTAMP
        )

        roomRef
            .updateChildren(updates)
            .addOnSuccessListener {
                callback(
                    ChatResult.Success(Unit)
                )
            }
            .addOnFailureListener { exception ->
                callback(
                    ChatResult.Error(
                        exception.message ?: "Failed to send message"
                    )
                )
            }
    }
}