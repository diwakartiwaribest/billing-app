# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.shop.billing.** { *; }

-keep class com.itextpdf.** { *; }
-keep interface com.itextpdf.** { *; }
-keep class com.itextpdf.html2pdf.** { *; }

# iText optionally references Jackson for JSON utilities — not needed in our usage
-dontwarn com.fasterxml.jackson.**

-dontwarn com.itextpdf.bouncycastle.BouncyCastleFactory
-dontwarn org.slf4j.impl.StaticLoggerBinder
