# Keep kotlinx serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.brewkin.beanbook.**$$serializer { *; }
-keepclassmembers class com.brewkin.beanbook.** { *** Companion; }
-keepclasseswithmembers class com.brewkin.beanbook.** { kotlinx.serialization.KSerializer serializer(...); }
