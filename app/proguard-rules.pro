##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic signatures for the script's nested model collections.
-keepattributes Signature
-keepattributes *Annotation*

-dontwarn sun.misc.**

# Gson's bundled rules preserve @SerializedName fields. Annotate persisted model
# fields with their existing JSON names instead of keeping entire model classes.

##---------------End: proguard configuration for Gson  ----------

##---------------Begin: proguard configuration for pdfbox-android  ----------
-keep class org.spongycastle.** { *; }
-dontwarn org.spongycastle.**
-dontwarn sun.reflect.**
