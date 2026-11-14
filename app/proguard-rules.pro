# MovizNow Admin R8 / ProGuard rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class com.moviznow.admin.data.model.** { *; }
-keep class com.moviznow.admin.BuildConfig { *; }
-keep class com.google.firebase.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
