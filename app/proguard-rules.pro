# Standard rules for Android
-keep class android.support.v4.os.BuildCompat {}
-keep class com.google.android.gms.location.LocationServices { *; }

# Rules for Kotlin/Coroutines
-keepnames class * extends kotlinx.coroutines.CoroutineContext

# Rules for Firebase/Firestore/Data Models (CRITICAL FIX for Release Build Crashes)

# 1. Keep all classes, constructors, and fields for all data models used by Firestore
# This addresses the "No properties to serialize found" error in release builds.
-keep class com.refreshme.data.** { *; }

# 2. Keep the names and all members (fields/accessors) of data classes
# The explicit keeping of members ensures R8 doesn't strip or rename the properties.
-keepclassmembers class com.refreshme.data.** {
  <init>(...); # Keep all constructors
  <fields>;    # Keep all fields
  <methods>;   # Keep all methods (including getters/setters for var properties)
}

# HOTFIX 3.0.9: Firestore-serialized model classes that live OUTSIDE
# com.refreshme.data.** were silently being obfuscated. R8 was renaming
# fields like `userId` → `a`, which broke Firestore security rules that
# inspect specific field names (e.g. the /legalAcceptances/{uid}/acceptances/
# rule requires request.resource.data.userId == userId). The server then
# rejected every write with PERMISSION_DENIED.
#
# Keep these classes AND their fields un-obfuscated. Mirror the pattern used
# for com.refreshme.data.** above.
-keep class com.refreshme.legal.LegalAcceptance { *; }
-keep class com.refreshme.legal.AcceptanceStatus { *; }
-keep class com.refreshme.chat.ChatMessage { *; }
-keep class com.refreshme.booking.Booking { *; }
-keep class com.refreshme.User { *; }

-keepclassmembers class com.refreshme.legal.LegalAcceptance {
  <init>(...);
  <fields>;
  <methods>;
}
-keepclassmembers class com.refreshme.legal.AcceptanceStatus {
  <init>(...);
  <fields>;
  <methods>;
}
-keepclassmembers class com.refreshme.chat.ChatMessage {
  <init>(...);
  <fields>;
  <methods>;
}
-keepclassmembers class com.refreshme.booking.Booking {
  <init>(...);
  <fields>;
  <methods>;
}
-keepclassmembers class com.refreshme.User {
  <init>(...);
  <fields>;
  <methods>;
}

# Generic safeguard: any field tagged with a Firestore annotation
# (@PropertyName, @ServerTimestamp, @DocumentId) keeps its original name,
# so future Firestore models don't silently regress even if they land
# outside the packages above.
-keepclassmembers class * {
  @com.google.firebase.firestore.PropertyName <fields>;
  @com.google.firebase.firestore.ServerTimestamp <fields>;
  @com.google.firebase.firestore.DocumentId <fields>;
}
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }

# 3. Rules for Parcelable (if used in data classes)
-keepnames class * implements android.os.Parcelable { *; }
-keepclassmembers class * implements android.os.Parcelable {
  <init>(android.os.Parcel);
}

# 4. Rules for Fragments referenced via XML/strings (Navigation Component)
-keep class com.refreshme.**Fragment {
    public <init>();
}

# Strip debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Rules for Retrofit/OkHttp/GSON/Jackson (if serialization is used elsewhere)
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn com.google.firebase.**
-dontwarn org.apache.http.legacy.**
-dontwarn com.google.android.gms.location.**

# Rules for Databinding
-keep class com.refreshme.databinding.** { *; }
-keepnames class * extends androidx.databinding.ViewDataBinding {
    public void set*(...);
    public * get*();
}
