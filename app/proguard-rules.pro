# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ZXing — chỉ dùng để TẠO mã QR (thuần Java, đường mã hoá không dùng reflection nên R8 tự giữ
# QRCodeWriter và loại bỏ phần giải mã/định dạng khác). Chỉ chặn cảnh báo cho các lớp tuỳ chọn.
-dontwarn com.google.zxing.**

# ------------------------------------------------------------------------------------------------
# ML Kit Barcode Scanning (bundled) — màn QUÉT MÃ QR
# ------------------------------------------------------------------------------------------------
# ML Kit đăng ký các "component" bằng REFLECTION: MlKitComponentDiscoveryService đọc tên các lớp
# *Registrar (BarcodeRegistrar/CommonComponentRegistrar/VisionCommonRegistrar) từ manifest rồi gọi
# Class.forName(tên).newInstance(). Consumer-rule mặc định (-keep class * implements ComponentRegistrar)
# CHỈ giữ TÊN lớp mà KHÔNG giữ constructor; R8 full-mode (bật mặc định từ AGP 8/9) coi <init>() là
# "không được dùng" (chỉ gọi qua reflection) nên XOÁ nó → newInstance() ném lỗi → component
# BarcodeScanner không được đăng ký → BarcodeScanning.getClient() thao tác trên đối tượng NULL →
# NullPointerException. Lỗi CHỈ xảy ra ở bản RELEASE (debug không minify). Giữ constructor để
# discovery khởi tạo được các registrar.
-keepclassmembers class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}