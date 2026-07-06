# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.shop.billing.** { *; }

-dontwarn com.itextpdf.bouncycastle.BouncyCastleFactory
-dontwarn org.slf4j.impl.StaticLoggerBinder
