# BusTrackv2: Smart Bus Tracking App

BusTrackv2 is a comprehensive native Android application designed to provide real-time bus tracking, dynamic routing updates, and intelligent conversational assistance for daily commuters, while also streamlining operations for bus operators and transit managers. 

By integrating Firebase for real-time updates, Google Maps for live location tracking, and advanced LLM integration for conversational scheduling and tracking inquiries, BusTrackv2 revolutionizes the public transit experience.

## ✨ Key Features

### For Passengers 🚍
- **Live Vehicle Tracking:** View the real-time location of buses on a map.
- **AI Chatbot Assistant:** Talk naturally to the integrated chatbot (powered by major LLMs like OpenAI, Gemini, and DeepSeek) to ask for bus schedules, routing, or real-time platform updates! (e.g., *"Where is vehicle 104?"*, *"What's the schedule from Depot A to B today?"*)
- **Platform & Depot Updates:** Receive live information regarding specific depots and platform numbers.
- **Push Notifications:** Setup alerts to notify you when a specific bus is on the way.

### For Operators 🛠️
- **Operator Dashboard:** Dedicated interface (`OperatorMainPageActivity`) for drivers and operators to broadcast their active vehicle status.
- **Real-Time Data Sync:** Instantly sync vehicle locations, service states, and schedules via Firebase Firestore and Realtime Database. 

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin
- **Minimum SDK:** Android 7.0 (API Level 24)
- **Target SDK:** Android 15 (API Level 35)
- **UI & Architecture:** ViewBinding, Material Components, AndroidX Navigation Component, ViewModel/LiveData
- **Asynchrony:** Kotlin Coroutines
- **Location & Mapping:** Google Play Services Maps API (`18.2.0`), Location API
- **Backend (BaaS):** Firebase (Firestore, Realtime Database, Authentication, Cloud Messaging, Crashlytics, Analytics)
- **Networking:** OkHttp (for third-party AI REST APIs)

## 🚀 Getting Started

To run the application locally on your machine, follow these steps:

### Prerequisites
1. Android Studio (Latest stable version recommended).
2. A Firebase Project configured for Android.
3. API Keys for AI Chatbot providers and Google Maps SDK.

### Setup Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/rajeshmamilla/Smart-Bus-Tracking-App.git
   ```

2. **Add `google-services.json`:**
   Ensure you have a Firebase project setup and place the downloaded `google-services.json` file inside the `app/` directory of the project.

3. **Provide API Keys Safely:**
   Do **not** hardcode your API keys inside the Kotlin files. Instead, specify them locally. The app requires keys for Maps and AI interaction:
   - Add your Google Maps API key inside your `local.properties` or Android Manifest.
   - For the Chatbot APIs (DeepSeek, OpenAI, Gemini, OpenRouter), implement secure local storage (e.g., pulling from `local.properties` via `BuildConfig`) to protect your keys from accidental commits.

4. **Build and Run:**
   Sync the Gradle files and build the project. Run the app on an Android Emulator (with Google Play Services) or a physical Android device.

## 🤝 Contributing
Contributions, issues, and feature requests are welcome!

## 📜 License
This project is for educational and proprietary use. Please refer to the author for usage rights.
