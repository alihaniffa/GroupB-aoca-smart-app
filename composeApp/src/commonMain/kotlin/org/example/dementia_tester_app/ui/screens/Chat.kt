// Local working chat UI - conversation screen with contact picker and last message update
package org.example.dementia_tester_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.* // Includes MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.dementia_tester_app.ui.components.FormColors
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

data class FirebaseChatMessage(
    val chatName: String = "",
    val text: String = "",
    val isFromUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

private val sessionChats = mutableStateListOf(
    ChatItem("Dr. Smith", "Your next appointment is scheduled for tomorrow at 10:00 AM.", "10:30 AM", 0),
    ChatItem("Nurse Johnson", "How are you feeling today? Don't forget to take your medication.", "Yesterday", 0),
    ChatItem("Caregiver Support", "We've sent you the resources we discussed during our last conversation.", "Jul 19", 0),
    ChatItem("Memory Clinic", "Your test results have been uploaded to your profile.", "Jul 15", 0),
    ChatItem("Medication Reminder", "It's time to take your evening medication.", "Jul 10", 0)
)
private val sessionMessages = mutableStateMapOf<String, SnapshotStateList<ChatMessage>>()

@Composable
fun Chat() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedChat by remember { mutableStateOf<ChatItem?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }

    val chats = sessionChats

    LaunchedEffect(Unit) {
        val authUser = Firebase.auth.currentUser
        if (authUser == null) return@LaunchedEffect
        val currentUserId = authUser.uid
        val db = FirebaseFirestore.getInstance()

        chats.forEachIndexed { index, chat ->
            val sanitizedChatName = chat.name.replace(Regex("[^A-Za-z0-9]"), "_")
            val roomId = "room_${currentUserId}_${sanitizedChatName}"

            db.collection("chatRooms")
                .document(roomId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    // Guard: Verify user hasn't switched accounts while the network request was in flight
                    if (Firebase.auth.currentUser?.uid != currentUserId) return@addOnSuccessListener

                    if (!snapshot.isEmpty) {
                        val latestDoc = snapshot.documents[0]
                        val text = latestDoc.getString("text")

                        if (text != null) {
                            chats[index] = chats[index].copy(
                                lastMessage = text,
                                time = "Now"
                            )
                        }
                    }
                }
        }
    }

    fun updateLastMessage(chatName: String, newMessage: String) {
        val index = chats.indexOfFirst { it.name == chatName }

        if (index != -1) {
            chats[index] = chats[index].copy(
                lastMessage = newMessage,
                time = "Now",
                unreadCount = 0
            )
            selectedChat = chats[index]
        } else {
            val newChat = ChatItem(
                name = chatName,
                lastMessage = newMessage,
                time = "Now",
                unreadCount = 0
            )
            chats.add(0, newChat)
            selectedChat = newChat
        }
    }

    if (selectedChat != null) {
        val chatIndex = chats.indexOfFirst { it.name == selectedChat!!.name }
        if (chatIndex != -1 && chats[chatIndex].unreadCount > 0) {
            chats[chatIndex] = chats[chatIndex].copy(unreadCount = 0)
        }

        val authUser = Firebase.auth.currentUser
        val currentUserId = authUser?.uid ?: "anonymous_user"
        val sanitizedChatName = selectedChat!!.name.replace(Regex("[^A-Za-z0-9]"), "_")
        val roomId = "room_${currentUserId}_${sanitizedChatName}"

        ChatConversationScreen(
            chat = selectedChat!!,
            roomId = roomId,
            onBack = { selectedChat = null },
            onMessageSent = { chatName, message ->
                updateLastMessage(chatName, message)
            }
        )
    } else {
        ChatListScreen(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            chats = chats.filter {
                searchQuery.isBlank() ||
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.lastMessage.contains(searchQuery, ignoreCase = true)
            },
            onChatClick = { selectedChat = it },
            onNewChatClick = { showContactPicker = true }
        )
    }

    if (showContactPicker) {
        AlertDialog(
            onDismissRequest = { showContactPicker = false },
            title = {
                Text("Start New Chat")
            },
            text = {
                Column {
                    Text("Who do you want to chat with?")

                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        "Dr. Smith",
                        "Nurse Johnson",
                        "Caregiver Support",
                        "Family Caregiver",
                        "Memory Clinic"
                    ).forEach { contactName ->
                        Text(
                            text = contactName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val existingChat = chats.find { it.name == contactName }

                                    selectedChat = existingChat ?: ChatItem(
                                        name = contactName,
                                        lastMessage = "",
                                        time = "Now",
                                        unreadCount = 0
                                    )

                                    showContactPicker = false
                                }
                                .padding(vertical = 12.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showContactPicker = false }) {
                    Text("Cancel", color = FormColors.green)
                }
            }
        )
    }
}

fun clearChatSessionState() {
    sessionMessages.clear()
    sessionChats.clear()
    sessionChats.addAll(
        listOf(
            ChatItem("Dr. Smith", "Your next appointment is scheduled for tomorrow at 10:00 AM.", "10:30 AM", 0),
            ChatItem("Nurse Johnson", "How are you feeling today? Don't forget to take your medication.", "Yesterday", 0),
            ChatItem("Caregiver Support", "We've sent you the resources we discussed during our last conversation.", "Jul 19", 0),
            ChatItem("Memory Clinic", "Your test results have been uploaded to your profile.", "Jul 15", 0),
            ChatItem("Medication Reminder", "It's time to take your evening medication.", "Jul 10", 0)
        )
    )
}

@Composable
fun ChatListScreen(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    chats: List<ChatItem>,
    onChatClick: (ChatItem) -> Unit,
    onNewChatClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Search chats...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = FormColors.green
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FormColors.green,
                unfocusedBorderColor = FormColors.green,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true
        )

        Button(
            onClick = onNewChatClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FormColors.green
            )
        ) {
            Text("Start New Chat", fontSize = 16.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            if (chats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No chats found", color = Color.Gray)
                }
            } else {
                chats.forEach { chat ->
                    ChatListItem(
                        chat = chat,
                        onChatClick = { onChatClick(chat) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ChatConversationScreen(
    chat: ChatItem,
    roomId: String,
    onBack: () -> Unit,
    onMessageSent: (String, String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }

    // Create a mutable state list for messages tied to this room
    val messages = remember { mutableStateListOf<ChatMessage>() }

    // Load messages from Firestore when the screen enters composition
    LaunchedEffect(roomId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("chatRooms")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp") // Order messages chronologically
            .get()
            .addOnSuccessListener { result ->
                messages.clear()
                val currentUserId = Firebase.auth.currentUser?.uid ?: ""

                for (document in result) {
                    val text = document.getString("text") ?: ""
                    val senderId = document.getString("senderId") ?: ""
                    val isFromUser = (senderId == currentUserId)

                    messages.add(ChatMessage(text, isFromUser))
                }

                // Fallback if empty
                if (messages.isEmpty()) {
                    messages.add(ChatMessage(chat.lastMessage.ifBlank { "Start a new conversation." }, false))
                }
            }
            .addOnFailureListener {
                // Handle failure / fallback to local if needed
                messages.add(ChatMessage("Failed to load message history.", false))
            }
    }

    fun handleSend() {
        if (messageText.isNotBlank()) {
            val sentMessage = messageText.trim()
            val currentUserId = Firebase.auth.currentUser?.uid ?: ""

            // Optimistically update local UI state immediately
            messages.add(ChatMessage(sentMessage, true))

            val db = FirebaseFirestore.getInstance()
            val roomRef = db.collection("chatRooms").document(roomId)

            // Ensure room exists, then save message
            roomRef.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    roomRef.set(
                        mapOf(
                            "participants" to listOf(currentUserId, chat.name),
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )
                    )
                }

                roomRef.collection("messages").add(
                    mapOf(
                        "senderId" to currentUserId,
                        "text" to sentMessage,
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )
                )
            }

            onMessageSent(chat.name, sentMessage)
            messageText = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(8.dp),
                    tint = FormColors.green
                )

                Text(
                    text = chat.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                messages.forEach { message ->
                    ChatBubble(message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Bottom input section
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { handleSend() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FormColors.green,
                        unfocusedBorderColor = FormColors.green,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { handleSend() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FormColors.green
                    )
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (message.isFromUser) FormColors.green else MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
                .widthIn(max = 260.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isFromUser) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
        }
    }
}

data class ChatItem(
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int
)

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean
)

@Composable
fun ChatListItem(
    chat: ChatItem,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onChatClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = chat.lastMessage.ifBlank { "No messages yet" },
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 2
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = chat.time,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(FormColors.green)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.surface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}