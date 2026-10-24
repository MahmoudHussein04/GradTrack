# Gson reads these models by reflection: keep them and their field names.
-keep class com.gradtrack.app.data.model.** { *; }
-keepclassmembers class com.gradtrack.app.data.model.** { <fields>; }
-keepattributes Signature, *Annotation*
-keep class com.gradtrack.app.data.local.** { *; }
