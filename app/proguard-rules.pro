# Keep Room entities and their constructors
-keep class com.cacl2.schedule.data.local.entity.** { *; }

# Keep data classes used by DataStore
-keepclassmembers class com.cacl2.schedule.model.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }

# Keep widget classes (registered in manifest)
-keep class com.cacl2.schedule.widget.** { *; }
-keep class com.cacl2.schedule.widget.WidgetRemoteViewsService
-keep class com.cacl2.schedule.widget.WidgetRemoteViewsFactory

# Keep line numbers for crash debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile