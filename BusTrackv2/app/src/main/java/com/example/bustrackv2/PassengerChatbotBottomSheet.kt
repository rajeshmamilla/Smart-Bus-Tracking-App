package com.example.bustrackv2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth

class PassengerChatbotBottomSheet : BottomSheetDialogFragment() {
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var inputEditText: TextInputEditText
    private lateinit var sendButton: MaterialButton
    private lateinit var loadingSpinner: View
    private val chatAdapter = ChatAdapter()
    private val deepseekApiKey = ""
    private val openaiApiKey = ""
    private val geminiApiKey = ""
    private val openrouterApiKey = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottomsheet_passenger_chatbot, container, false)
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        inputEditText = view.findViewById(R.id.inputEditText)
        sendButton = view.findViewById(R.id.sendButton)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatRecyclerView.adapter = chatAdapter
        sendButton.setOnClickListener { sendMessage() }
        logFcmToken()
        // Firestore connection test
        FirebaseFirestore.getInstance().collection("schedules")
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                Toast.makeText(requireContext(), "Firestore test: ${result.size()} docs", Toast.LENGTH_LONG).show()
                if (!result.isEmpty) {
                    val doc = result.documents[0]
                    Toast.makeText(requireContext(), "First doc: ${doc.data}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Firestore error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        return view
    }

    private fun logFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                android.util.Log.d("FCM", "Token: $token")
            }
        }
    }

    private fun detectIntent(message: String): String {
        val liveTrackingPatterns = listOf(
            Regex("track vehicle number ([A-Z0-9]+)", RegexOption.IGNORE_CASE),
            Regex("track ([A-Z0-9]+)", RegexOption.IGNORE_CASE),
            Regex("live tracking for ([A-Z0-9]+)", RegexOption.IGNORE_CASE),
            Regex("where is ([A-Z0-9]+)", RegexOption.IGNORE_CASE),
            Regex("show ([A-Z0-9]+) location", RegexOption.IGNORE_CASE)
        )
        for (pattern in liveTrackingPatterns) {
            if (pattern.containsMatchIn(message)) return "live_tracking"
        }
        val liveDepotPatterns = listOf(
            Regex("live depot", RegexOption.IGNORE_CASE),
            Regex("depot info", RegexOption.IGNORE_CASE),
            Regex("depot live info", RegexOption.IGNORE_CASE)
        )
        for (pattern in liveDepotPatterns) {
            if (pattern.containsMatchIn(message)) return "live_depot"
        }
        return when {
            message.contains("schedule", ignoreCase = true) && message.contains("from", ignoreCase = true) && message.contains("to", ignoreCase = true) -> "bus_schedule"
            message.contains("platform", ignoreCase = true) && message.contains("search", ignoreCase = true) -> "platform_search"
            message.contains("alert", ignoreCase = true) || message.contains("remind", ignoreCase = true) -> "alert_setup"
            else -> "chat"
        }
    }

    private suspend fun handleIntent(intent: String, message: String): String? {
        return when (intent) {
            "bus_schedule" -> getBusSchedule(message)
            "live_tracking" -> getLiveTracking(message)
            "platform_search" -> getPlatformSearch(message)
            "alert_setup" -> setupAlert(message)
            "live_depot" -> getLiveDepotInfo(message)
            else -> null
        }
    }

    private suspend fun getBusSchedule(message: String): String = withContext(Dispatchers.IO) {
        val params = extractBusScheduleParamsFlexible(message)
        if (params != null) {
            val (from, to, date) = params
            val db = FirebaseFirestore.getInstance()
            val query = db.collection("schedules")
                .whereEqualTo("from", from)
                .whereEqualTo("to", to)
                .whereEqualTo("date", date)
            val result = Tasks.await(query.get())
            if (!result.isEmpty) {
                val schedule = formatBusSchedule(result.documents[0].data ?: emptyMap())
                return@withContext schedule
            }
            return@withContext "No schedule found."
        }
        "Please specify the route and date."
    }

    private suspend fun getLiveTracking(message: String): String = withContext(Dispatchers.IO) {
        val regex = Regex("vehicle number ([A-Z0-9]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(message)
        if (match != null) {
            val vehicleNumber = match.groupValues[1]
            val db = FirebaseFirestore.getInstance()
            val query = db.collection("services")
                .whereEqualTo("vehicleNumber", vehicleNumber)
            val result = Tasks.await(query.get())
            if (!result.isEmpty) {
                val trackingInfo = result.documents[0].data.toString()
                return@withContext trackingInfo
            }
            return@withContext "No tracking info found."
        }
        "Please provide the vehicle number."
    }

    private suspend fun getPlatformSearch(message: String): String = withContext(Dispatchers.IO) {
        val regex = Regex("platform (\\d+) at depot (\\w+)", RegexOption.IGNORE_CASE)
        val match = regex.find(message)
        if (match != null) {
            val (platform, depot) = match.destructured
            val db = FirebaseFirestore.getInstance()
            val query = db.collection("depot_live_updates_a")
                .whereEqualTo("depotName", depot)
                .whereEqualTo("platformNumber", platform)
            val result = Tasks.await(query.get())
            if (!result.isEmpty) {
                val platformInfo = result.documents[0].data.toString()
                return@withContext platformInfo
            }
            return@withContext "No platform info found."
        }
        "Please specify the depot and platform number."
    }

    private suspend fun setupAlert(message: String): String = withContext(Dispatchers.IO) {
        val regex = Regex("alert for bus ([A-Z0-9]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(message)
        if (match != null) {
            val busNumber = match.groupValues[1]
            val db = FirebaseFirestore.getInstance()
            val alert = hashMapOf(
                "busNumber" to busNumber,
                "timestamp" to System.currentTimeMillis()
            )
            Tasks.await(db.collection("bus_alerts").add(alert))
            return@withContext "Alert set for bus $busNumber."
        }
        "Please specify what you want to set an alert for."
    }

    private suspend fun getLiveDepotInfo(message: String): String = withContext(Dispatchers.IO) {
        val params = extractLiveDepotParams(message)
        if (params != null) {
            val (depotName, platformNumber) = params
            val collection = "depot_live_updates_${depotName.lowercase()}"
            val db = FirebaseFirestore.getInstance()
            db.collection(collection).document(platformNumber).get().addOnSuccessListener { doc ->
                showLoading(false)
                if (doc.exists()) {
                    val data = doc.data
                    val currentService = data?.get("current_service") ?: "-"
                    val arrivalTime = data?.get("arrival_time") ?: "-"
                    val departureTime = data?.get("departure_time") ?: "-"
                    val from = data?.get("from") ?: "-"
                    val to = data?.get("to") ?: "-"
                    val reply = """
                        • Current Service: $currentService
                        • Arrival Time: $arrivalTime
                        • Departure Time: $departureTime
                        • From: $from
                        • To: $to
                    """.trimIndent()
                    chatAdapter.addMessage(ChatMessage(reply, false))
                } else {
                    chatAdapter.addMessage(ChatMessage("No live depot info found for depot $depotName platform $platformNumber.", false))
                }
            }.addOnFailureListener { e ->
                showLoading(false)
                chatAdapter.addMessage(ChatMessage("Error: \\${e.localizedMessage}", false))
            }
            return@withContext "Please specify depot and platform, e.g. 'Check live depot info for A and platform 1'"
        } else {
            showLoading(false)
            chatAdapter.addMessage(ChatMessage("Please specify depot and platform, e.g. 'Check live depot info for A and platform 1'", false))
            return@withContext "Please specify depot and platform, e.g. 'Check live depot info for A and platform 1'"
        }
    }

    private fun sendMessage() {
        val userMessage = inputEditText.text?.toString()?.trim()
        if (userMessage.isNullOrEmpty()) return
        chatAdapter.addMessage(ChatMessage(userMessage, true))
        inputEditText.setText("")
        showLoading(true)
        lifecycleScope.launch {
            val intent = detectIntent(userMessage)
            if (intent == "live_tracking") {
                val vehicleNumber = extractVehicleNumber(userMessage)
                if (vehicleNumber != null) {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("services").document(vehicleNumber).get().addOnSuccessListener { doc ->
                        showLoading(false)
                        if (doc.exists()) {
                            chatAdapter.addMessage(ChatMessage(
                                text = "",
                                isUser = false,
                                buttonLabel = "View Live Tracking for $vehicleNumber",
                                vehicleNumber = vehicleNumber
                            ))
                        } else {
                            chatAdapter.addMessage(ChatMessage("No tracking info found for $vehicleNumber.", false))
                        }
                    }.addOnFailureListener { e ->
                        showLoading(false)
                        chatAdapter.addMessage(ChatMessage("Error: ${e.localizedMessage}", false))
                    }
                    return@launch
                } else {
                    showLoading(false)
                    chatAdapter.addMessage(ChatMessage("Please provide the vehicle number.", false))
                    return@launch
                }
            } else if (intent == "bus_schedule") {
                val params = extractBusScheduleParamsFlexible(userMessage)
                if (params != null) {
                    val (from, to, date) = params
                    Toast.makeText(requireContext(), "Querying Firestore for bus schedule", Toast.LENGTH_SHORT).show()
                    listenToBusSchedule(from, to, date) { update ->
                        chatAdapter.addMessage(ChatMessage(update, false))
                        showLoading(false)
                    }
                    return@launch
                } else {
                    Toast.makeText(requireContext(), "Could not extract route and date. Please specify in the format: 'from [A] to [B] on [date]' or 'on [date] from [A] to [B]'", Toast.LENGTH_LONG).show()
                    chatAdapter.addMessage(ChatMessage("Please specify the route and date in the format: 'from [A] to [B] on [date]' or 'on [date] from [A] to [B]'", false))
                    showLoading(false)
                    return@launch
                }
            } else if (intent == "live_depot") {
                val params = extractLiveDepotParams(userMessage)
                if (params != null) {
                    val (depotName, platformNumber) = params
                    val collection = "depot_live_updates_${depotName.lowercase()}"
                    val db = FirebaseFirestore.getInstance()
                    db.collection(collection).document(platformNumber).get().addOnSuccessListener { doc ->
                        showLoading(false)
                        if (doc.exists()) {
                            val data = doc.data
                            val currentService = data?.get("current_service") ?: "-"
                            val arrivalTime = data?.get("arrival_time") ?: "-"
                            val departureTime = data?.get("departure_time") ?: "-"
                            val from = data?.get("from") ?: "-"
                            val to = data?.get("to") ?: "-"
                            val reply = """
                                • Current Service: $currentService
                                • Arrival Time: $arrivalTime
                                • Departure Time: $departureTime
                                • From: $from
                                • To: $to
                            """.trimIndent()
                            chatAdapter.addMessage(ChatMessage(reply, false))
                        } else {
                            chatAdapter.addMessage(ChatMessage("No live depot info found for depot $depotName platform $platformNumber.", false))
                        }
                    }.addOnFailureListener { e ->
                        showLoading(false)
                        chatAdapter.addMessage(ChatMessage("Error: ${e.localizedMessage}", false))
                    }
                    return@launch
                } else {
                    showLoading(false)
                    chatAdapter.addMessage(ChatMessage("Please specify depot and platform, e.g. 'Check live depot info for A and platform 1'", false))
                    return@launch
                }
            }
            val result = handleIntent(intent, userMessage)
            if (result != null && intent != "chat") {
                chatAdapter.addMessage(ChatMessage(result, false))
                showLoading(false)
            } else {
                val (botReply, provider) = getBotReplyWithFallback(userMessage)
                chatAdapter.addMessage(ChatMessage("$botReply\n\n($provider)", false))
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        loadingSpinner.visibility = if (show) View.VISIBLE else View.GONE
        sendButton.isEnabled = !show
        inputEditText.isEnabled = !show
    }

    private suspend fun getBotReplyWithFallback(message: String): Pair<String, String> {
        val deepseekReply = getBotReplyDeepseek(message)
        if (deepseekReply.first) {
            return Pair(deepseekReply.second, "Deepseek")
        } else {
            val openaiReply = getBotReplyOpenAI(message)
            if (!openaiReply.startsWith("OpenAI")) {
                return Pair(openaiReply, "OpenAI")
            } else {
                val geminiReply = getBotReplyGemini(message)
                if (!geminiReply.startsWith("Gemini")) {
                    return Pair(geminiReply, "Gemini")
                } else {
                    val openrouterReply = getBotReplyOpenRouter(message)
                    return Pair(openrouterReply, "OpenRouter")
                }
            }
        }
    }

    // Returns Pair<success, reply>
    private suspend fun getBotReplyDeepseek(message: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .callTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val url = "https://api.deepseek.com/v1/chat/completions"
            val json = JSONObject()
            json.put("model", "deepseek-chat")
            json.put("messages", listOf(mapOf("role" to "user", "content" to message)))
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $deepseekApiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val responseJson = JSONObject(responseBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
                    return@withContext Pair(true, content.trim())
                }
                return@withContext Pair(false, "Sorry, I couldn't get a valid response from the bot. Please try again.")
            } else {
                return@withContext Pair(false, "Deepseek error: ${response.code}")
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "Deepseek error: ${e.localizedMessage}")
        }
    }

    private suspend fun getBotReplyOpenAI(message: String): String = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .callTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val url = "https://api.openai.com/v1/chat/completions"
            val json = JSONObject()
            json.put("model", "gpt-3.5-turbo")
            val messagesArray = org.json.JSONArray()
            messagesArray.put(org.json.JSONObject().put("role", "user").put("content", message))
            json.put("messages", messagesArray)
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $openaiApiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val responseJson = JSONObject(responseBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
                    return@withContext content.trim()
                }
                return@withContext "Sorry, I couldn't get a valid response from OpenAI. Please try again."
            } else if (response.code == 401) {
                return@withContext "OpenAI authentication failed. Please check your API key."
            } else if (response.code == 429) {
                return@withContext "OpenAI rate limit reached. Please wait and try again."
            } else if (response.code == 503) {
                return@withContext "OpenAI is busy. Please try again in a few moments."
            } else {
                return@withContext "OpenAI is currently unavailable (Error ${response.code}). Please try again later."
            }
        } catch (e: java.net.SocketTimeoutException) {
            return@withContext "OpenAI is taking too long to respond. Please try again later."
        } catch (e: Exception) {
            return@withContext "OpenAI network or server error: ${e.localizedMessage}. Please try again later."
        }
    }

    private suspend fun getBotReplyGemini(message: String): String = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .callTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$geminiApiKey"
            val json = org.json.JSONObject()
            val contentsArray = org.json.JSONArray()
            val partsArray = org.json.JSONArray()
            partsArray.put(org.json.JSONObject().put("text", message))
            val userContent = org.json.JSONObject().put("role", "user").put("parts", partsArray)
            contentsArray.put(userContent)
            json.put("contents", contentsArray)
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val responseJson = org.json.JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        return@withContext text.trim()
                    }
                }
                return@withContext "Sorry, I couldn't get a valid response from Gemini. Please try again."
            } else if (response.code == 401) {
                return@withContext "Gemini authentication failed. Please check your API key."
            } else if (response.code == 429) {
                return@withContext "Gemini rate limit reached. Please wait and try again."
            } else if (response.code == 503) {
                return@withContext "Gemini is busy. Please try again in a few moments."
            } else {
                return@withContext "Gemini is currently unavailable (Error ${response.code}). Please try again later."
            }
        } catch (e: java.net.SocketTimeoutException) {
            return@withContext "Gemini is taking too long to respond. Please try again later."
        } catch (e: Exception) {
            return@withContext "Gemini network or server error: ${e.localizedMessage}. Please try again later."
        }
    }

    private suspend fun getBotReplyOpenRouter(message: String): String = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .callTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val url = "https://openrouter.ai/api/v1/chat/completions"
            val json = JSONObject()
            json.put("model", "openai/gpt-3.5-turbo")
            val messagesArray = org.json.JSONArray()
            messagesArray.put(org.json.JSONObject().put("role", "user").put("content", message))
            json.put("messages", messagesArray)
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $openrouterApiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val responseJson = JSONObject(responseBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
                    return@withContext content.trim()
                }
                return@withContext "Sorry, I couldn't get a valid response from OpenRouter. Please try again."
            } else if (response.code == 401) {
                return@withContext "OpenRouter authentication failed. Please check your API key."
            } else if (response.code == 429) {
                return@withContext "OpenRouter rate limit reached. Please wait and try again."
            } else if (response.code == 503) {
                return@withContext "OpenRouter is busy. Please try again in a few moments."
            } else {
                return@withContext "OpenRouter is currently unavailable (Error ${response.code}). Please try again later."
            }
        } catch (e: java.net.SocketTimeoutException) {
            return@withContext "OpenRouter is taking too long to respond. Please try again later."
        } catch (e: Exception) {
            return@withContext "OpenRouter network or server error: ${e.localizedMessage}. Please try again later."
        }
    }

    // Real-time listener for live tracking
    private fun listenToLiveTracking(vehicleNumber: String, onUpdate: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("services")
            .whereEqualTo("vehicleNumber", vehicleNumber)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    onUpdate("Error: ${e.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshots != null && !snapshots.isEmpty) {
                    val trackingInfo = snapshots.documents[0].data.toString()
                    onUpdate(trackingInfo)
                } else {
                    onUpdate("No tracking info found.")
                }
            }
    }

    // Real-time listener for bus schedule
    private fun listenToBusSchedule(from: String, to: String, date: String, onUpdate: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("schedules")
            .whereEqualTo("from", from)
            .whereEqualTo("to", to)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    onUpdate("Error: ${e.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshots != null && !snapshots.isEmpty) {
                    val schedule = formatBusSchedule(snapshots.documents[0].data ?: emptyMap())
                    onUpdate(schedule)
                } else {
                    onUpdate("No schedule found.")
                }
            }
    }

    private fun extractBusScheduleParamsFlexible(message: String): Triple<String, String, String>? {
        val patterns = listOf(
            Regex("from (\\w+) to (\\w+) on ([\\w/ ]+)", RegexOption.IGNORE_CASE),
            Regex("on ([\\w/ ]+) from (\\w+) to (\\w+)", RegexOption.IGNORE_CASE),
            Regex("bus schedules on ([\\w/ ]+) from (\\w+) to (\\w+)", RegexOption.IGNORE_CASE),
            Regex("bus schedules from (\\w+) to (\\w+) on ([\\w/ ]+)", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return when (pattern.pattern) {
                    "from (\\w+) to (\\w+) on ([\\w/ ]+)" -> Triple(match.groupValues[1], match.groupValues[2], normalizeDateToFirestore(match.groupValues[3]))
                    "on ([\\w/ ]+) from (\\w+) to (\\w+)" -> Triple(match.groupValues[2], match.groupValues[3], normalizeDateToFirestore(match.groupValues[1]))
                    "bus schedules on ([\\w/ ]+) from (\\w+) to (\\w+)" -> Triple(match.groupValues[2], match.groupValues[3], normalizeDateToFirestore(match.groupValues[1]))
                    "bus schedules from (\\w+) to (\\w+) on ([\\w/ ]+)" -> Triple(match.groupValues[1], match.groupValues[2], normalizeDateToFirestore(match.groupValues[3]))
                    else -> null
                }
            }
        }
        return null
    }

    private fun normalizeDateToFirestore(date: String): String {
        // Accepts "01/05/2025", "1/5/2025", "01 May 2025", etc. and returns "01/05/2025"
        val inputFormats = listOf("dd/MM/yyyy", "d/M/yyyy", "d/MM/yyyy", "dd/M/yyyy", "dd MMM yyyy", "d MMM yyyy", "dd MMMM yyyy", "d MMMM yyyy")
        for (format in inputFormats) {
            try {
                val sdf = java.text.SimpleDateFormat(format)
                val parsed = sdf.parse(date)
                val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy")
                return outputFormat.format(parsed)
            } catch (_: Exception) {}
        }
        return date // fallback to original if parsing fails
    }

    private fun formatBusSchedule(data: Map<String, Any?>): String {
        val vehicleNumber = data["vehicle_number"] ?: "N/A"
        val busType = data["bus_type"] ?: "N/A"
        val departureTime = data["departure_time"] ?: "N/A"
        val platformNumber = data["platform_number"] ?: "N/A"
        val runOn = data["run_on"] ?: "N/A"
        return """
            • Vehicle Number: $vehicleNumber
            • Bus Type: $busType
            • Departure Time: $departureTime
            • Platform Number: $platformNumber
            • Runs On: $runOn
        """.trimIndent()
    }

    private fun extractVehicleNumber(message: String): String? {
        val patterns = listOf(
            Regex("track vehicle number ([A-Z0-9]+)", RegexOption.IGNORE_CASE),
            Regex("track ([A-Z0-9]+)", RegexOption.IGNORE_CASE),
            Regex("live tracking for ([A-Z0-9]+)", RegexOption.IGNORE_CASE),
            Regex("where is ([A-Z0-9]+)", RegexOption.IGNORE_CASE),
            Regex("show ([A-Z0-9]+) location", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun extractLiveDepotParams(message: String): Pair<String, String>? {
        // Example: "Check live depot info for A and platform 1"
        val regex = Regex("depot info for ([A-Za-z]) and platform (\\d+)", RegexOption.IGNORE_CASE)
        val regex2 = Regex("live depot info for ([A-Za-z]) and platform (\\d+)", RegexOption.IGNORE_CASE)
        val regex3 = Regex("live depot for ([A-Za-z]) and platform (\\d+)", RegexOption.IGNORE_CASE)
        val regex4 = Regex("depot ([A-Za-z]) platform (\\d+)", RegexOption.IGNORE_CASE)
        val patterns = listOf(regex, regex2, regex3, regex4)
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return Pair(match.groupValues[1].uppercase(), match.groupValues[2])
            }
        }
        return null
    }
}

// Simple chat message data class
data class ChatMessage(
    val text: String = "",
    val isUser: Boolean = false,
    val buttonLabel: String? = null,
    val vehicleNumber: String? = null
)

// Simple RecyclerView adapter for chat messages
class ChatAdapter : RecyclerView.Adapter<ChatViewHolder>() {
    private val messages = mutableListOf<ChatMessage>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    override fun getItemCount() = messages.size
    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }
}

class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val userMessageText: android.widget.TextView = itemView.findViewById(R.id.userMessageText)
    private val botMessageText: android.widget.TextView = itemView.findViewById(R.id.botMessageText)
    private val botButton: com.google.android.material.button.MaterialButton? = itemView.findViewById(R.id.botMessageButton)
    fun bind(msg: ChatMessage) {
        if (msg.isUser) {
            userMessageText.visibility = View.VISIBLE
            botMessageText.visibility = View.GONE
            botButton?.visibility = View.GONE
            userMessageText.text = msg.text
        } else if (msg.buttonLabel != null && msg.vehicleNumber != null) {
            userMessageText.visibility = View.GONE
            botMessageText.visibility = View.GONE
            botButton?.visibility = View.VISIBLE
            botButton?.text = msg.buttonLabel
            botButton?.setOnClickListener {
                val context = itemView.context
                val intent = android.content.Intent(context, TrackBusActivity::class.java)
                intent.putExtra("vehicleNumber", msg.vehicleNumber)
                context.startActivity(intent)
            }
        } else {
            userMessageText.visibility = View.GONE
            botMessageText.visibility = View.VISIBLE
            botButton?.visibility = View.GONE
            botMessageText.text = msg.text
        }
    }
} 