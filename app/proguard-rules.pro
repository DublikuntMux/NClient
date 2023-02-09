-ignorewarnings
-keep class * {
    public private *;
}
#glide proguard
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-assumenosideeffects class com.dublikunt.nclientv2.utility.LogUtility {
    public static void d(...);
    public static void i(...);
    public static void e(...);
}
-keep public class * implements com.bumptech.glide.module.GlideModule
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder
