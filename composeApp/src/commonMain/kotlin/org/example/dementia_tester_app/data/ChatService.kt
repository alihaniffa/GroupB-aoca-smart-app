package org.example.dementia_tester_app.data

data class ChatMessageData(
    val text: String,
    val senderId: String
)

sealed class ChatResult<out T> {

    data class Success<T>(
        val data: T
    ) : ChatResult<T>()

    data class Error(
        val message: String
    ) : ChatResult<Nothing>()
}

interface ChatServiceInterface {

    fun getCurrentUserId(): String?

    fun getLatestMessage(
        roomId: String,
        callback: (ChatResult<ChatMessageData?>) -> Unit
    )

    fun getMessages(
        roomId: String,
        callback: (ChatResult<List<ChatMessageData>>) -> Unit
    )

    fun sendMessage(
        roomId: String,
        recipientId: String,
        text: String,
        callback: (ChatResult<Unit>) -> Unit
    )
}

expect class ChatService() : ChatServiceInterface {

    override fun getCurrentUserId(): String?

    override fun getLatestMessage(
        roomId: String,
        callback: (ChatResult<ChatMessageData?>) -> Unit
    )

    override fun getMessages(
        roomId: String,
        callback: (ChatResult<List<ChatMessageData>>) -> Unit
    )

    override fun sendMessage(
        roomId: String,
        recipientId: String,
        text: String,
        callback: (ChatResult<Unit>) -> Unit
    )
}