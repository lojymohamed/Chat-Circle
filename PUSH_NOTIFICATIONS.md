# Push Notifications Implementation (OneSignal - Free)

## Current implementation

The app uses OneSignal's free tier for delivery and dashboard test messages. Add your OneSignal App ID to `res/values/strings.xml` under `onesignal_app_id`; it is intentionally blank in source control. No Firebase Functions deployment or billing is required.

## Overview
This implementation adds push notification support using **OneSignal**, a completely free alternative to Firebase Cloud Functions. OneSignal handles push notifications without requiring the Firebase Blaze (pay-as-you-go) plan.

## What's Implemented

### 1. OneSignal Helper
- **OneSignalHelper.kt**: Manages OneSignal initialization and notification handling
- Automatically requests notification permissions (Android 13+)
- Associates users with their Firebase UID for targeted notifications
- Handles notification taps with roomId for navigation

### 2. Dependency Injection
- **RepositoryModule.kt**: Added OneSignalHelper provider
- OneSignalHelper is injected into MainActivity

### 3. MainActivity Integration
- Initializes OneSignal on app launch
- Automatically logs in user with Firebase UID
- Handles notification tap events

### 4. Dependencies
- Added OneSignal SDK to build.gradle.kts
- Kept Firebase Messaging for future compatibility

## Setup Instructions

### 1. Create OneSignal Account
1.Go to [onesignal.com](https://onesignal.com) and sign up for a free account
2. Create a new app
3. Select "Android" as the platform
4. Choose "Firebase Cloud Messaging (FCM)" as the integration method
5. Copy your OneSignal App ID

### 2. Configure Firebase for OneSignal
1. Go to Firebase Console → Project Settings → Cloud Messaging
2. Copy your Server Key and Sender ID
3. Add these to your OneSignal app settings

### 3. Update OneSignal App ID
Replace `YOUR_ONESIGNAL_APP_ID` in `OneSignalHelper.kt` with your actual OneSignal App ID:
```kotlin
companion object {
    private const val ONESIGNAL_APP_ID = "your-actual-app-id-here"
}
```

### 4. Add Google Services JSON
Ensure `google-services.json` is in your `app/` folder (already present if using Firebase)

## How It Works

### 1. User Registration
When the app launches:
1. MainActivity initializes OneSignal
2. OneSignal requests notification permission (Android 13+)
3. User is logged in to OneSignal with their Firebase UID
4. This allows sending targeted notifications to specific users

### 2. Sending Notifications
You can send notifications using OneSignal's REST API from your backend or directly from your app:

**Example REST API Call:**
```bash
curl -X POST https://onesignal.com/api/v1/notifications \
  -H "Content-Type: application/json; charset=utf-8" \
  -H "Authorization: Basic YOUR_REST_API_KEY" \
  -d '{
    "app_id": "YOUR_ONESIGNAL_APP_ID",
    "include_external_user_ids": ["user_firebase_uid"],
    "contents": {"en": "New message from John"},
    "headings": {"en": "Chat Circle"},
    "data": {"roomId": "room_123"}
  }'
```

### 3. Receiving Notifications
When a notification arrives:
1. OneSignal displays the notification
2. When tapped, the NotificationOpenedHandler is called
3. The roomId from notification data is extracted
4. You can navigate to the specific chat room using the roomId

## Sending Notifications from Your App

To send notifications when a new message is sent, you can call OneSignal's REST API:

```kotlin
suspend fun sendNotificationToUser(
    targetUserId: String,
    title: String,
    message: String,
    roomId: String
) {
    val url = "https://onesignal.com/api/v1/notifications"
    val jsonBody = JSONObject().apply {
        put("app_id", ONESIGNAL_APP_ID)
        put("include_external_user_ids", JSONArray().put(targetUserId))
        put("contents", JSONObject().put("en", message))
        put("headings", JSONObject().put("en", title))
        put("data", JSONObject().put("roomId", roomId))
    }

    // Make HTTP POST request with your REST API key
    // Use OkHttp or similar HTTP client
}
```

## Testing

### Test from OneSignal Dashboard
1. Go to OneSignal Dashboard → Messages
2. Click "Send New Message"
3. Enter title and message
4. Select "Send to Subscribed Users" or specific users
5. Send and verify notification appears

### Test with App Closed
1. Build and install the app
2. Launch the app to initialize OneSignal
3. Close the app completely
4. Send a test notification from OneSignal Dashboard
5. Verify notification appears and opens app when tapped

## Advantages of OneSignal

### Free Tier Benefits
- **No cost**: Completely free for up to unlimited subscribers
- **No Blaze plan required**: Works with Firebase Spark (free) plan
- **Easy setup**: Simple integration compared to Cloud Functions
- **Rich features**: Segmentation, A/B testing, analytics included

### Features
- Targeted notifications to specific users (by Firebase UID)
- Scheduled notifications
- In-app messages
- Email notifications
- Analytics and delivery reports
- A/B testing
- Segmentation

## Permissions
- **POST_NOTIFICATIONS**: Required on Android 13 (API 33) and above
- Requested automatically by OneSignal on initialization

## Next Steps

### 1. Implement Notification Sending
Add notification sending logic when messages are created:
- Call OneSignal REST API from your message sending logic
- Include roomId in notification data for navigation

### 2. Implement Navigation
Handle notification tap to navigate to specific chat room:
- Use the roomId from notification data
- Navigate using Navigation Component

### 3. Add Notification Preferences
Allow users to customize notification settings:
- Enable/disable notifications
- Sound preferences
- Quiet hours

## Migration from Cloud Functions
If you previously used Cloud Functions:
1. Remove Cloud Functions code (already done)
2. Replace with OneSignal REST API calls
3. Update any backend code to use OneSignal instead of FCM
4. Test thoroughly before deploying

## Resources
- [OneSignal Documentation](https://documentation.onesignal.com/docs/android-sdk-setup)
- [OneSignal REST API](https://documentation.onesignal.com/docs/create-notification-api)
- [OneSignal Firebase Integration](https://documentation.onesignal.com/docs/firebase-cloud-messaging-android-setup)
