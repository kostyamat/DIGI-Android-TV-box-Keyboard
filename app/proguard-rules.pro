# 1. Повна заборона на зміну імен у вашому пакеті
-keep class com.kostyamat.r2r_q.** { *; }
-keep interface com.kostyamat.r2r_q.** { *; }

# 2. Захист моделей даних (JSON парсинг)
# Якщо ви використовуєте Gson, Kotlin Serialization або подібне - це критично!
-keepclassmembers class com.kostyamat.r2r_q.** {
    @com.google.gson.annotations.SerializedName <fields>;
    private <fields>;
    public <fields>;
}

# 3. Захист системних компонентів Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.view.InputMethodService
-keep public class * extends android.accessibilityservice.AccessibilityService
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 4. Захист метаданих для ресурсів
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 5. Запобігання видаленню методів, які викликає система
-keepclassmembers class * extends android.accessibilityservice.AccessibilityService {
   protected void onServiceConnected();
   public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent);
   public void onInterrupt();
}

# 6. Збереження логів (якщо ви хочете бачити їх у релізі через logcat)
-dontoptimize
-dontobfuscate
