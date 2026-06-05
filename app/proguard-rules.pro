# Holocron R8 keep rules.
# Debug stays un-minified; if something "works in debug, breaks in release",
# suspect a missing keep here.

# --- HiveMQ MQTT client + its reflective transitive deps (Netty, jctools, RxJava) ---
-keep class com.hivemq.** { *; }
-dontwarn com.hivemq.**
-keep class io.netty.** { *; }
-dontwarn io.netty.**
-keep class org.jctools.** { *; }
-dontwarn org.jctools.**
-dontwarn io.reactivex.**
-dontwarn org.slf4j.**

# --- kotlinx.serialization: keep generated $serializer companions ---
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.tomerklein.holocron.**$$serializer { *; }
-keepclassmembers class dev.tomerklein.holocron.** {
    *** Companion;
}
-keepclasseswithmembers class dev.tomerklein.holocron.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- libphonenumber bundles its metadata as resources; keep them ---
-keep class com.google.i18n.phonenumbers.** { *; }
-dontwarn com.google.i18n.phonenumbers.**
