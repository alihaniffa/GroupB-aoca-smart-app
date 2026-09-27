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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.example.dementia_tester_app.data.ChatResult
import org.example.dementia_tester_app.data.ChatService
import org.example.dementia_tester_app.data.DatabaseResult
import org.example.dementia_tester_app.data.UserProfile
import org.example.dementia_tester_app.data.UserProfileService
import org.example.dementia_tester_app.data.UserType
import org.example.dementia_tester_app.ui.components.FormColors

data class ChatItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int,
    val role: String = ""
)

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean
)

fun getChatRoomId(userId1: String, userId2: String): String {
    val sorted = listOf(userId1, userId2).sorted()
    return "room_${sorted[0]}_${sorted[1]}"
}

fun formatDoctorName(rawName: String): String {
    val trimmed = rawName.trim()
    val clean = when {
        trimmed.startsWith("Dr.", ignoreCase = true) -> trimmed.substring(3).trim()
        trimmed.startsWith("Dr ", ignoreCase = true) -> trimmed.substring(3).trim()
        trimmed.startsWith("Doctor ", ignoreCase = true) -> trimmed.substring(7).trim()
        trimmed.equals("Assigned Doctor", ignoreCase = true) -> ""
        trimmed.equals("Doctor", ignoreCase = true) -> ""
        else -> trimmed
    }
    if (clean.isBlank()) {
        return "Doctor"
    }
    val capitalized = clean.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
        word.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
    }
    return "Dr. $capitalized"
}

fun getContactDisplayName(contact: UserProfile): String {
    return when (contact.userType) {
        UserType.DOCTOR -> {
            val raw = contact.name.trim().ifBlank {
                contact.email.trim().substringBefore("@")
            }
            formatDoctorName(raw)
        }
        UserType.CAREGIVER -> {
            val raw = contact.name.trim().ifBlank {
                contact.email.trim().substringBefore("@").ifBlank {
                    "Caregiver"
                }
            }
            raw.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
                word.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
            }
        }
        UserType.USER -> {
            val raw = contact.name.trim().ifBlank {
                contact.email.trim().substringBefore("@").ifBlank {
                    "Patient ${contact.userId.take(6)}"
                }
            }
            raw.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
                word.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
            }
        }
    }
}

private val sessionChats = mutableStateListOf<ChatItem>()

fun clearChatSessionState() {
    sessionChats.clear()
}

@Composable
fun Chat() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedChat by remember { mutableStateOf<ChatItem?>(null) }
    var isLoadingContacts by remember { mutableStateOf(false) }
    var currentUserType by remember { mutableStateOf(UserType.USER) }

    val chats = sessionChats
    val chatService = remember { ChatService() }
    val userProfileService = remember { UserProfileService() }

    fun refreshChats() {
        val currentUserId = chatService.getCurrentUserId() ?: return
        isLoadingContacts = true

        userProfileService.getCurrentUserProfile { profileResult ->
            if (profileResult !is DatabaseResult.Success) {
                isLoadingContacts = false
                return@getCurrentUserProfile
            }

            val currentProfile = profileResult.data
            val userType = currentProfile.userType
            currentUserType = userType

            val handleContacts: (List<UserProfile>) -> Unit = { rawList ->
                val distinctContacts = rawList
                    .filter { it.userId.isNotBlank() && it.userId != currentUserId }
                    .distinctBy { it.userId }

                val validUserIds = distinctContacts.map { it.userId }.toSet()
                chats.removeAll { it.id !in validUserIds }

                distinctContacts.forEach { contact ->
                    val roleLabel = when (contact.userType) {
                        UserType.DOCTOR -> "Doctor"
                        UserType.CAREGIVER -> "Caregiver"
                        UserType.USER -> "Patient"
                    }
                    val displayName = getContactDisplayName(contact)

                    val existingIndex = chats.indexOfFirst { it.id == contact.userId }
                    if (existingIndex == -1) {
                        chats.add(
                            ChatItem(
                                id = contact.userId,
                                name = displayName,
                                lastMessage = "",
                                time = "",
                                unreadCount = 0,
                                role = roleLabel
                            )
                        )
                    } else {
                        chats[existingIndex] = chats[existingIndex].copy(
                            name = displayName,
                            role = roleLabel
                        )
                    }
                }

                // Query latest messages for each chat room
                chats.forEach { chatItem ->
                    val roomId = getChatRoomId(currentUserId, chatItem.id)
                    chatService.getLatestMessage(roomId) { latestResult ->
                        if (latestResult is ChatResult.Success && latestResult.data != null) {
                            val msg = latestResult.data
                            val idx = chats.indexOfFirst { it.id == chatItem.id }
                            if (idx != -1) {
                                chats[idx] = chats[idx].copy(
                                    lastMessage = msg.text,
                                    time = "Recent"
                                )
                            }
                        }
                    }
                }

                isLoadingContacts = false
            }

            when (userType) {
                UserType.DOCTOR -> {
                    // Doctors can only chat to their assigned patients and assigned caregivers
                    userProfileService.getAllUsers { assignedResult ->
                        if (assignedResult is DatabaseResult.Success) {
                            val assignedPatients = assignedResult.data
                            val doctorContacts = mutableListOf<UserProfile>()
                            doctorContacts.addAll(assignedPatients)

                            val caregiverIds = assignedPatients
                                .map { it.assignedCaregiverId.trim() }
                                .filter { it.isNotBlank() }
                                .distinct()

                            if (caregiverIds.isEmpty()) {
                                handleContacts(doctorContacts.toList())
                            } else {
                                var pendingCaregivers = caregiverIds.size
                                caregiverIds.forEach { cgId ->
                                    userProfileService.getUserProfile(cgId) { cgResult ->
                                        if (cgResult is DatabaseResult.Success) {
                                            doctorContacts.add(cgResult.data)
                                        } else {
                                            doctorContacts.add(
                                                UserProfile(
                                                    userId = cgId,
                                                    name = "",
                                                    userType = UserType.CAREGIVER
                                                )
                                            )
                                        }
                                        pendingCaregivers--
                                        if (pendingCaregivers <= 0) {
                                            handleContacts(doctorContacts.toList())
                                        }
                                    }
                                }
                            }
                        } else {
                            handleContacts(emptyList())
                        }
                    }
                }
                UserType.CAREGIVER -> {
                    // Caregivers can only chat to their assigned patients and assigned doctors
                    userProfileService.getPatientsForCurrentCaregiver { patientsResult ->
                        if (patientsResult is DatabaseResult.Success) {
                            val assignedPatients = patientsResult.data
                            val caregiverContacts = mutableListOf<UserProfile>()
                            caregiverContacts.addAll(assignedPatients)

                            val docIds = assignedPatients
                                .map { it.assignedDoctorId.trim() }
                                .filter { it.isNotBlank() }
                                .distinct()

                            if (docIds.isEmpty()) {
                                handleContacts(caregiverContacts.toList())
                            } else {
                                var pendingDoctors = docIds.size
                                docIds.forEach { docId ->
                                    userProfileService.getUserProfile(docId) { docResult ->
                                        if (docResult is DatabaseResult.Success) {
                                            caregiverContacts.add(docResult.data)
                                        } else {
                                            caregiverContacts.add(
                                                UserProfile(
                                                    userId = docId,
                                                    name = "",
                                                    userType = UserType.DOCTOR
                                                )
                                            )
                                        }
                                        pendingDoctors--
                                        if (pendingDoctors <= 0) {
                                            handleContacts(caregiverContacts.toList())
                                        }
                                    }
                                }
                            }
                        } else {
                            handleContacts(emptyList())
                        }
                    }
                }
                UserType.USER -> {
                    // Patients can only chat to their assigned doctor and assigned caregiver
                    val assignedDocId = currentProfile.assignedDoctorId.trim()
                    val assignedCaregiverId = currentProfile.assignedCaregiverId.trim()

                    val targetList = mutableListOf<Pair<String, UserType>>()
                    if (assignedDocId.isNotBlank()) targetList.add(assignedDocId to UserType.DOCTOR)
                    if (assignedCaregiverId.isNotBlank()) targetList.add(assignedCaregiverId to UserType.CAREGIVER)

                    if (targetList.isEmpty()) {
                        handleContacts(emptyList())
                    } else {
                        val patientContacts = mutableListOf<UserProfile>()
                        var pending = targetList.size

                        targetList.forEach { (id, roleType) ->
                            userProfileService.getUserProfile(id) { result ->
                                if (result is DatabaseResult.Success) {
                                    patientContacts.add(result.data)
                                } else {
                                    patientContacts.add(
                                        UserProfile(
                                            userId = id,
                                            name = "",
                                            userType = roleType
                                        )
                                    )
                                }
                                pending--
                                if (pending <= 0) {
                                    handleContacts(patientContacts.toList())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshChats()
    }

    fun updateLastMessage(
        chatId: String,
        newMessage: String
    ) {
        val index = chats.indexOfFirst { it.id == chatId }
        if (index != -1) {
            chats[index] = chats[index].copy(
                lastMessage = newMessage,
                time = "Now",
                unreadCount = 0
            )
            selectedChat = chats[index]
        }
    }

    if (selectedChat != null) {
        val currentChat = selectedChat!!
        val chatIndex = chats.indexOfFirst { it.id == currentChat.id }

        if (chatIndex != -1 && chats[chatIndex].unreadCount > 0) {
            chats[chatIndex] = chats[chatIndex].copy(unreadCount = 0)
        }

        val displayChat = if (chatIndex != -1) chats[chatIndex] else currentChat
        val currentUserId = chatService.getCurrentUserId() ?: "anonymous_user"
        val roomId = getChatRoomId(currentUserId, displayChat.id)

        ChatConversationScreen(
            chat = displayChat,
            roomId = roomId,
            onBack = {
                selectedChat = null
                refreshChats()
            },
            onMessageSent = { chatId, message ->
                updateLastMessage(chatId = chatId, newMessage = message)
            }
        )
    } else {
        val filteredChats = chats
            .sortedWith(
                compareByDescending<ChatItem> { it.lastMessage.isNotBlank() }
                    .thenBy { it.name.lowercase() }
            )
            .filter {
                searchQuery.isBlank() ||
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.role.contains(searchQuery, ignoreCase = true) ||
                        it.lastMessage.contains(searchQuery, ignoreCase = true)
            }

        val emptyMessage = when (currentUserType) {
            UserType.USER -> "No assigned doctor or caregiver found."
            UserType.CAREGIVER -> "No assigned patients or doctors found."
            UserType.DOCTOR -> "No assigned patients or caregivers found."
        }

        ChatListScreen(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            chats = filteredChats,
            isLoading = isLoadingContacts,
            onChatClick = { selectedChat = it },
            emptyStateMessage = emptyMessage
        )
    }
}

@Composable
fun ChatListScreen(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    chats: List<ChatItem>,
    isLoading: Boolean,
    onChatClick: (ChatItem) -> Unit,
    emptyStateMessage: String = "No chats found"
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
            placeholder = {
                Text("Search chats...")
            },
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

        if (isLoading && chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FormColors.green)
            }
        } else {
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
                        Text(
                            text = emptyStateMessage,
                            color = Color.Gray
                        )
                    }
                } else {
                    chats.forEach { chat ->
                        ChatListItem(
                            chat = chat,
                            onChatClick = {
                                onChatClick(chat)
                            }
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )
                    }
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
    var sendError by remember { mutableStateOf<String?>(null) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val chatService = remember { ChatService() }

    LaunchedEffect(roomId) {
        val currentUserId = chatService.getCurrentUserId() ?: ""
        while (isActive) {
            chatService.getMessages(roomId) { result ->
                when (result) {
                    is ChatResult.Success -> {
                        val fetched = result.data.map { message ->
                            ChatMessage(
                                text = message.text,
                                isFromUser = currentUserId.isNotBlank() &&
                                        message.senderId == currentUserId
                            )
                        }
                        if (fetched.isNotEmpty() || messages.isEmpty()) {
                            messages.clear()
                            messages.addAll(fetched)
                        }
                    }
                    is ChatResult.Error -> {
                        // Keep current messages if fetch fails
                    }
                }
            }
            delay(2000)
        }
    }

    fun handleSend() {
        val sentMessage = messageText.trim()
        if (sentMessage.isBlank()) return

        val currentUserId = chatService.getCurrentUserId()
        if (currentUserId.isNullOrBlank()) return

        messages.add(
            ChatMessage(
                text = sentMessage,
                isFromUser = true
            )
        )
        messageText = ""

        chatService.sendMessage(
            roomId = roomId,
            recipientId = chat.id,
            text = sentMessage
        ) { result ->
            when (result) {
                is ChatResult.Success -> {
                    sendError = null
                    onMessageSent(chat.id, sentMessage)
                }
                is ChatResult.Error -> {
                    sendError = result.message
                }
            }
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

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val headerName = if (chat.role.equals("Doctor", ignoreCase = true)) {
                            formatDoctorName(chat.name)
                        } else {
                            chat.name
                        }
                        Text(
                            text = headerName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (chat.role.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = FormColors.green.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = chat.role,
                                    color = FormColors.green,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No messages yet. Send a message to start chatting!",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    messages.forEach { message ->
                        ChatBubble(message)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (sendError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Failed to send: ${sendError}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Dismiss",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { sendError = null }
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { handleSend() }),
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
                    colors = ButtonDefaults.buttonColors(containerColor = FormColors.green)
                ) {
                    Text("Send")
                }
            }
        }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (message.isFromUser) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (message.isFromUser) {
                        FormColors.green
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(12.dp)
                .widthIn(max = 260.dp)
        ) {
            Text(
                text = message.text,
                color =
                    if (message.isFromUser) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                fontSize = 14.sp
            )
        }
    }
}

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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (chat.role.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = FormColors.green.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = chat.role,
                                color = FormColors.green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = chat.lastMessage.ifBlank {
                        "Tap to start conversation"
                    },
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 2
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (chat.time.isNotBlank()) {
                    Text(
                        text = chat.time,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(FormColors.green)
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
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
