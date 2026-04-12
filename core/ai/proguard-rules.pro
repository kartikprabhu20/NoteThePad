# kotlinx-serialization specific rules
-keepattributes Annotation, Signature, InnerClasses, EnclosingMethod

# Keep serializable classes and their members
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Keep the serializer for all serializable classes
-keep class *$$serializer { *; }

# Keep specific Gist classes just in case
-keep class com.mintanable.notethepad.feature_ai.data.repository.GistWrapper { *; }
-keep class com.mintanable.notethepad.feature_ai.data.repository.GistFile { *; }
