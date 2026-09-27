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
        trimmed.equals("Assigned Doctor", ignoreCase = true) -> ""
        else -> trimmed
    }
    val fallback = clean.ifBlank { "Doctor" }
    val capitalized = fallback.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
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
    var showContactPicker by remember { mutableStateOf(false) }
    var contactSearchQuery by remember { mutableStateOf("") }
    var isLoadingContacts by remember { mutableStateOf(false) }

    val chats = sessionChats
    val availableContacts = remember { mutableStateListOf<UserProfile>() }
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

            val handleContacts: (List<UserProfile>) -> Unit = { rawList ->
                val distinctContacts = rawList
                    .filter { it.userId.isNotBlank() && it.userId != currentUserId }
                    .distinctBy { it.userId }

                availableContacts.clear()
                availableContacts.addAll(distinctContacts)

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
                    val doctorContacts = mutableListOf<UserProfile>()
                    userProfileService.getAllUsers { assignedResult ->
                        if (assignedResult is DatabaseResult.Success) {
                            doctorContacts.addAll(assignedResult.data)
                        }
                        userProfileService.getUnassignedPatients { unassignedResult ->
                            if (unassignedResult is DatabaseResult.Success) {
                                doctorContacts.addAll(unassignedResult.data)
                            }
                            userProfileService.getAllCaregivers { caregiversResult ->
                                if (caregiversResult is DatabaseResult.Success) {
                                    doctorContacts.addAll(caregiversResult.data)
                                }
                                handleContacts(doctorContacts)
                            }
                        }
                    }
                }
                UserType.CAREGIVER -> {
                    val caregiverContacts = mutableListOf<UserProfile>()
                    userProfileService.getPatientsForCurrentCaregiver { patientsResult ->
                        if (patientsResult is DatabaseResult.Success) {
                            caregiverContacts.addAll(patientsResult.data)
                        }
                        userProfileService.getAllDoctors { doctorsResult ->
                            if (doctorsResult is DatabaseResult.Success) {
                                caregiverContacts.addAll(doctorsResult.data)
                            }
                            handleContacts(caregiverContacts)
                        }
                    }
                }
                UserType.USER -> {
                    val patientContacts = mutableListOf<UserProfile>()
                    val assignedDocId = currentProfile.assignedDoctorId.trim()
                    val assignedCaregiverId = currentProfile.assignedCaregiverId.trim()

                    // Query assigned doctor profile to obtain doctor full name and details
                    if (assignedDocId.isNotBlank()) {
                        userProfileService.getUserProfile(assignedDocId) { docResult ->
                            val docProfile = if (docResult is DatabaseResult.Success) {
                                docResult.data
                            } else {
                                UserProfile(
                                    userId = assignedDocId,
                                    name = "",
                                    userType = UserType.DOCTOR
                                )
                            }
                            val idx = patientContacts.indexOfFirst { it.userId == assignedDocId }
                            if (idx != -1) {
                                patientContacts[idx] = docProfile
                            } else {
                                patientContacts.add(0, docProfile)
                            }
                            handleContacts(patientContacts.toList())

                            userProfileService.getAllDoctors { doctorsResult ->
                                if (doctorsResult is DatabaseResult.Success) {
                                    doctorsResult.data.forEach { doc ->
                                        if (doc.userId != assignedDocId && patientContacts.none { it.userId == doc.userId }) {
                                            patientContacts.add(doc)
                                        }
                                    }
                                    handleContacts(patientContacts.toList())
                                }
                            }
                        }
                    } else {
                        userProfileService.getAllDoctors { doctorsResult ->
                            if (doctorsResult is DatabaseResult.Success) {
                                patientContacts.addAll(doctorsResult.data)
                                handleContacts(patientContacts.toList())
                            }
                        }
                    }

                    if (assignedCaregiverId.isNotBlank()) {
                        userProfileService.getUserProfile(assignedCaregiverId) { cgResult ->
                            if (cgResult is DatabaseResult.Success) {
                                val idx = patientContacts.indexOfFirst { it.userId == assignedCaregiverId }
                                if (idx != -1) {
                                    patientContacts[idx] = cgResult.data
                                } else {
                                    patientContacts.add(cgResult.data)
                                }
                                handleContacts(patientContacts.toList())
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

        ChatListScreen(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            chats = filteredChats,
            isLoading = isLoadingContacts,
            onChatClick = { selectedChat = it },
            onNewChatClick = { showContactPicker = true }
        )
    }

    if (showContactPicker) {
        val filteredContacts = availableContacts.filter {
            contactSearchQuery.isBlank() ||
                    it.name.contains(contactSearchQuery, ignoreCase = true) ||
                    it.email.contains(contactSearchQuery, ignoreCase = true) ||
                    it.userType.name.contains(contactSearchQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = {
                showContactPicker = false
                contactSearchQuery = ""
            },
            title = {
                Text("Start New Chat", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = contactSearchQuery,
                        onValueChange = { contactSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        placeholder = { Text("Search by name or email...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = FormColors.green
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FormColors.green,
                            unfocusedBorderColor = FormColors.green
                        )
                    )

                    if (isLoadingContacts && availableContacts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = FormColors.green)
                        }
                    } else if (filteredContacts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (availableContacts.isEmpty())
                                    "No contacts found. When other accounts register, they will appear here."
                                else
                                    "No matching contacts found.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            filteredContacts.forEach { contact ->
                                val roleLabel = when (contact.userType) {
                                    UserType.DOCTOR -> "Doctor"
                                    UserType.CAREGIVER -> "Caregiver"
                                    UserType.USER -> "Patient"
                                }
                                val displayName = getContactDisplayName(contact)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val existingChat = chats.find { it.id == contact.userId }
                                            selectedChat = existingChat ?: ChatItem(
                                                id = contact.userId,
                                                name = displayName,
                                                lastMessage = "",
                                                time = "",
                                                unreadCount = 0,
                                                role = roleLabel
                                            ).also { chats.add(0, it) }

                                            showContactPicker = false
                                            contactSearchQuery = ""
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (contact.email.isNotBlank() && contact.email != displayName) {
                                            Text(
                                                text = contact.email,
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = FormColors.green.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = roleLabel,
                                            color = FormColors.green,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showContactPicker = false
                        contactSearchQuery = ""
                    }
                ) {
                    Text("Cancel", color = FormColors.green)
                }
            }
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
            Text(
                "Start New Chat",
                fontSize = 16.sp
            )
        }

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
                            "No chats found",
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
