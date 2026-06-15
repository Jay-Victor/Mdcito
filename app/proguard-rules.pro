-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-keep class com.mdcito.app.data.db.entity.** { *; }

-keepclassmembers class * {
    @androidx.room.Query <methods>;
    @androidx.room.Insert <methods>;
    @androidx.room.Update <methods>;
    @androidx.room.Delete <methods>;
}

-dontwarn kotlinx.coroutines.**
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn org.apache.xmlbeans.**
-dontwarn schemaorg_apache_xmlbeans.**
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }

# commonmark-java GFM extensions: keep node types and renderers accessed via reflection/factory pattern
-keep class org.commonmark.ext.gfm.strikethrough.** { *; }
-keep class org.commonmark.ext.gfm.tables.** { *; }
-keep class org.commonmark.ext.task.list.items.** { *; }
-keep class org.commonmark.ext.heading.anchor.** { *; }
-keep class org.commonmark.ext.autolink.** { *; }

# 第三方库缺失类警告抑制（非 Android 平台依赖）
-dontwarn aQute.bnd.annotation.baseline.BaselineIgnore
-dontwarn aQute.bnd.annotation.spi.ServiceConsumer
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-dontwarn com.github.luben.zstd.ZstdInputStream
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn java.awt.Shape
-dontwarn org.osgi.framework.Bundle
-dontwarn org.osgi.framework.BundleContext
-dontwarn org.osgi.framework.FrameworkUtil
-dontwarn org.osgi.framework.ServiceReference
-dontwarn org.osgi.framework.wiring.BundleRevision
-dontwarn org.tukaani.xz.MemoryLimitException
-dontwarn org.tukaani.xz.SingleXZInputStream
-dontwarn org.tukaani.xz.XZInputStream
-dontwarn sun.security.x509.X509Key
