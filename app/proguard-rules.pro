-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic signatures for the script's nested model collections.
-keepattributes Signature
-keepattributes *Annotation*

-dontwarn sun.misc.**

# Persisted database JSON uses these class and field names. Do not rename,
# remove, or otherwise transform the model schema in minified releases.
-keep class com.brokenshotgun.runlines.domain.model.** { *; }

# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

##---------------End: proguard configuration for Gson  ----------

##---------------Begin: proguard configuration for pdfbox-android  ----------
-keep class org.spongycastle.** { *; }
-dontwarn org.spongycastle.**
-dontwarn sun.reflect.**
