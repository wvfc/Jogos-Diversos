# Regras ProGuard padrão. O build de release não está minificado neste projeto,
# mas o arquivo é mantido para futuras customizações.

# Mantém metadados de serialização do kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
