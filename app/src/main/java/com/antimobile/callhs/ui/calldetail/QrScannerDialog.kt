package com.antimobile.callhs.ui.calldetail

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.QrScanHistoryStore
import com.antimobile.callhs.util.hasPermission
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Màn QUÉT MÃ QR toàn màn hình (Dialog) cho tính năng gửi tin nhắn theo mẫu.
 *  - Mở camera trực tiếp (CameraX) để quét; giải mã bằng ML Kit Barcode Scanning bản BUNDLED — model nhúng sẵn
 *    trong APK, chạy HOÀN TOÀN OFFLINE, KHÔNG cần Google Play Services trên máy. Bắt mã mờ/nghiêng/nhỏ/trên màn
 *    hình gần như tức thì (vượt trội ZXing cho quét trực tiếp).
 *  - Hoặc "Chọn ảnh từ thư viện" để đọc mã QR trong ảnh có sẵn (cũng qua ML Kit).
 * Quét/đọc được → gọi [onResult] MỘT LẦN với văn bản đọc được, rồi nơi gọi tự chuyển sang gửi tin.
 *
 * Khung phân tích 1280×720 (mức ML Kit khuyến nghị), luôn lấy khung MỚI NHẤT; ML Kit tự lo xoay/nhị phân hoá/
 * dò vị trí nên quét cả mã LỆCH TÂM (khung ngắm chỉ để gợi ý canh). Kèm nút ĐÈN FLASH; CHẠM để lấy nét (hiện
 * VÒNG LẤY NÉT tại điểm chạm) và CHỤM hai ngón để phóng to — cả hai TỰ xử lý trong Compose (xem [CameraPreview]).
 */
@Composable
fun QrScannerOverlay(onResult: (String) -> Unit, onDismiss: () -> Unit) {
    QrScannerContent(onResult = onResult, onClose = onDismiss)
}

/** Kích thước khung phân tích mong muốn — 720p đủ nét cho mã QR dày mà vẫn nhẹ để quét theo thời gian thực. */
private val ANALYSIS_TARGET = Size(1280, 720)

@Composable
private fun QrScannerContent(onResult: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    // Nhuộm thanh status + nav của CỬA SỔ ACTIVITY sang ĐEN (icon trắng) khi mở màn quét; trả lại khi đóng.
    // Dùng activity window (không phải Dialog) vì MIUI CHỈ tôn trọng navigationBarColor của activity — đúng
    // cách MainActivity đang ép nav bar TRẮNG. Nhờ đó thanh điều hướng chuyển đen ổn định, không chớp tắt.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevStatus = window?.statusBarColor
        val prevNav = window?.navigationBarColor
        val prevLightStatus = controller?.isAppearanceLightStatusBars
        val prevLightNav = controller?.isAppearanceLightNavigationBars
        window?.let { w ->
            w.statusBarColor = android.graphics.Color.BLACK
            w.navigationBarColor = android.graphics.Color.BLACK
            w.isNavigationBarContrastEnforced = false
        }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            window?.let { w ->
                prevStatus?.let { w.statusBarColor = it }
                prevNav?.let { w.navigationBarColor = it }
            }
            prevLightStatus?.let { controller?.isAppearanceLightStatusBars = it }
            prevLightNav?.let { controller?.isAppearanceLightNavigationBars = it }
        }
    }

    // active = false ngay khi ĐÓNG/HUỶ màn (đồng bộ trên luồng chính) và khi màn bị gỡ (onDispose). Nhờ đó một
    // kết quả quét ML Kit đã xếp hàng trên luồng chính NHƯNG chạy SAU cú tap Đóng sẽ bị bỏ qua → không điều hướng
    // oan (mở sheet/gửi mẫu) sau khi người dùng đã huỷ. (handled chỉ chống giao 2 LẦN, không chống giao MUỘN.)
    val active = remember { AtomicBoolean(true) }
    DisposableEffect(Unit) { onDispose { active.set(false) } }
    val requestClose: () -> Unit = { active.set(false); onClose() }
    BackHandler(onBack = requestClose)

    // Chỉ giao kết quả MỘT LẦN (khung camera bắn liên tục + có thể trùng với ảnh chọn).
    val handled = remember { AtomicBoolean(false) }
    val latestResult by rememberUpdatedState(onResult)
    val deliver: (String) -> Unit = remember {
        { text ->
            if (active.get() && handled.compareAndSet(false, true)) {
                // Điểm HỘI TỤ duy nhất của mọi luồng quét (camera + ảnh) → ghi vào LỊCH SỬ quét đúng MỘT LẦN
                // (nhờ cờ `handled`) trước khi giao kết quả về nơi gọi. Ghi trực tiếp: mảng ≤15 chuỗi ngắn nên
                // rẻ; SharedPreferences.apply() lưu đĩa bất đồng bộ. Kiểm tra active lần nữa ở block hoãn phòng
                // trường hợp Đóng chen vào GIỮA lúc này và lúc block chạy.
                QrScanHistoryStore.record(context, text)
                mainExecutor.execute { if (active.get()) latestResult(text) }
            }
        }
    }

    var hasCameraPermission by remember { mutableStateOf(hasPermission(context, Manifest.permission.CAMERA)) }
    var permissionAsked by remember { mutableStateOf(false) }
    var decodingImage by remember { mutableStateOf(false) }
    // Trạng thái đèn suy ra từ PHẦN CỨNG (torchState), không tự lật lạc quan → icon không "nói dối".
    var torchOn by remember { mutableStateOf(false) }
    // Mặc định CÓ đèn (hiện nút); chỉ ẩn khi biết CHẮC máy không có đèn (sau khi cam gắn xong).
    var hasFlash by remember { mutableStateOf(true) }

    // Camera controller — tạo MỘT LẦN; đặt khung phân tích 720p (đủ nét cho mã dày) và luôn lấy khung MỚI NHẤT.
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(ANALYSIS_TARGET, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                )
                .build()
            // Ta TỰ xử lý chạm-lấy-nét (để VẼ vòng lấy nét ở điểm chạm) & chụm-phóng-to trong Compose → tắt
            // bản dựng sẵn của CameraX để không xử lý HAI LẦN.
            isTapToFocusEnabled = false
            isPinchToZoomEnabled = false
        }
    }

    // Theo dõi trạng thái THẬT của đèn: đồng bộ icon, tự về OFF khi camera rebind (vào nền → quay lại), và
    // ẩn nút trên máy không có đèn. Chỉ đổi [hasFlash] khi cameraInfo đã sẵn sàng (tránh ẩn nhầm nút).
    DisposableEffect(cameraController) {
        val observer = Observer<Int> { state ->
            torchOn = state == TorchState.ON
            cameraController.cameraInfo?.let { hasFlash = it.hasFlashUnit() }
        }
        cameraController.torchState.observeForever(observer)
        onDispose { cameraController.torchState.removeObserver(observer) }
    }

    // Bộ quét ML Kit — tạo MỘT LẦN, chỉ nhận diện QR (giới hạn định dạng → nhanh hơn), đóng khi rời màn.
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }
    DisposableEffect(scanner) { onDispose { scanner.close() } }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !permissionAsked) {
            permissionAsked = true
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            decodingImage = true
            scope.launch {
                val text = decodeImageUri(context, uri, scanner)
                decodingImage = false
                if (text != null) deliver(text)
                else CallActions.toast(context, appStrings().qrScanner.noQrInImage)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(controller = cameraController, scanner = scanner, lifecycleOwner = lifecycleOwner, onDecoded = deliver)
            ScanReticle()
        } else {
            NoCameraMessage()
        }

        // Thanh status + nav phủ ĐEN đặc lên camera (TextureView cho vẽ đè); icon hệ thống (trắng) vẽ lên trên.
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars).background(Color.Black)
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars).background(Color.Black)
        )

        TopBar(
            onClose = requestClose,
            showTorch = hasCameraPermission && hasFlash,
            torchOn = torchOn,
            onToggleTorch = {
                // KHÔNG tự lật state — để observer torchState cập nhật theo phần cứng (icon luôn khớp thực tế).
                runCatching { cameraController.enableTorch(!torchOn) }
            }
        )

        BottomBar(
            decoding = decodingImage,
            onPickImage = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }
}

/**
 * Xem trước camera (ML Kit phân tích khung ở luồng nền, giao kết quả về luồng chính) + cử chỉ TỰ xử lý:
 *  - CHẠM → lấy nét đúng điểm (`meteringPointFactory` của PreviewView ánh xạ toạ độ chạm theo FILL_CENTER/xoay)
 *    + hiện VÒNG LẤY NÉT có hoạt ảnh tại chỗ chạm.
 *  - CHỤM 2 ngón → phóng to/thu nhỏ (`setZoomRatio`, kẹp trong [min,max] của `zoomState`).
 * Lớp cử chỉ là Box PHỦ TRÊN xem trước (nút bấm ở thanh trên/dưới tự nuốt chạm nên không kích nhầm lấy nét).
 */
@androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
@Composable
private fun CameraPreview(
    controller: LifecycleCameraController,
    scanner: BarcodeScanner,
    lifecycleOwner: LifecycleOwner,
    onDecoded: (String) -> Unit
) {
    val context = LocalContext.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val decodedCb by rememberUpdatedState(onDecoded)

    // Giữ tham chiếu PreviewView để lấy `meteringPointFactory`. COMPATIBLE (TextureView) thay vì SurfaceView:
    // KHÔNG "đục" surface riêng lọt sau thanh status/nav, nhờ đó hệ thống tô được 2 thanh màu ĐEN đặc.
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            this.controller = controller
        }
    }
    // Cú chạm lấy nét gần nhất (px trong khung xem trước) + số thứ tự để KHỞI ĐỘNG LẠI hoạt ảnh mỗi lần chạm.
    var focusTap by remember { mutableStateOf<FocusTap?>(null) }

    DisposableEffect(lifecycleOwner) {
        controller.setImageAnalysisAnalyzer(analysisExecutor, QrMlKitAnalyzer(scanner) { text -> decodedCb(text) })
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })

        // Lớp CỬ CHỈ phủ trên xem trước.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(previewView, controller) {
                    detectTapGestures { offset ->
                        runCatching {
                            val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                            controller.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                        }
                        // Đổi seq → key() dựng lại FocusRing → hoạt ảnh chạy lại kể cả chạm liên tiếp cùng chỗ.
                        focusTap = FocusTap(offset, (focusTap?.seq ?: 0) + 1)
                    }
                }
                .pointerInput(controller) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) {
                            val z = controller.zoomState.value
                            val cur = z?.zoomRatio ?: 1f
                            val min = z?.minZoomRatio ?: 1f
                            val max = z?.maxZoomRatio ?: 1f
                            runCatching { controller.setZoomRatio((cur * zoom).coerceIn(min, max)) }
                        }
                    }
                }
        )

        focusTap?.let { tap ->
            // key(seq): mỗi cú chạm dựng FocusRing MỚI → hoạt ảnh chạy lại từ đầu.
            key(tap.seq) {
                FocusRing(position = tap.position, onFinished = { if (focusTap?.seq == tap.seq) focusTap = null })
            }
        }
    }
}

/** Cú chạm lấy nét: vị trí (px trong khung xem trước) + [seq] tăng dần để khởi động lại hoạt ảnh vòng lấy nét. */
private data class FocusTap(val position: Offset, val seq: Int)

/** Cạnh vòng lấy nét. */
private val FOCUS_RING_SIZE = 76.dp

/**
 * Vòng LẤY NÉT hiện ở điểm chạm: nảy nhỏ vào (scale 1.4→1) + hiện dần, giữ một nhịp rồi mờ dần & tự gỡ
 * ([onFinished]). Chỉ là phản hồi THỊ GIÁC — việc lấy nét thật do CameraX xử lý ở nơi gọi.
 */
@Composable
private fun FocusRing(position: Offset, onFinished: () -> Unit) {
    val density = LocalDensity.current
    val ringPx = with(density) { FOCUS_RING_SIZE.toPx() }
    val scale = remember { Animatable(1.4f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
        alpha.animateTo(1f, tween(120))
        delay(560)
        alpha.animateTo(0f, tween(220))
        onFinished()
    }
    Box(
        modifier = Modifier
            .offset { IntOffset((position.x - ringPx / 2f).roundToInt(), (position.y - ringPx / 2f).roundToInt()) }
            .size(FOCUS_RING_SIZE)
            .graphicsLayer {
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
            }
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Vòng trong mảnh cho cảm giác "tâm ngắm".
        Box(
            modifier = Modifier
                .size(FOCUS_RING_SIZE * 0.5f)
                .border(1.dp, Color(0xB3FFFFFF), CircleShape)
        )
    }
}

/** Khung ngắm vuông ở giữa màn hình — CHỈ để GỢI Ý người dùng canh mã; ML Kit quét TOÀN khung nên mã lệch tâm vẫn bắt. */
@Composable
private fun BoxScope.ScanReticle() {
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(3.dp, Color.White, RoundedCornerShape(20.dp))
    )
}

@Composable
private fun BoxScope.TopBar(
    onClose: () -> Unit,
    showTorch: Boolean,
    torchOn: Boolean,
    onToggleTorch: () -> Unit
) {
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = appStrings().callList.close, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(appStrings().qr.scan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.weight(1f))
        if (showTorch) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (torchOn) Color.White else Color(0x33FFFFFF))
                    .clickable(onClick = onToggleTorch),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    contentDescription = if (torchOn) appStrings().qrScanner.torchOff else appStrings().qrScanner.torchOn,
                    tint = if (torchOn) Color.Black else Color.White,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxScope.BottomBar(decoding: Boolean, onPickImage: () -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = appStrings().qrScanner.instruction,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable(enabled = !decoding, onClick = onPickImage)
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (decoding) {
                CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Rounded.Image, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
            Text(
                text = if (decoding) appStrings().qrScanner.decoding else appStrings().qrScanner.pickImage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun BoxScope.NoCameraMessage() {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = Color(0xB3FFFFFF), modifier = Modifier.size(64.dp))
        Text(
            text = appStrings().qrScanner.noCameraPerm,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------------
// Phân tích khung & giải mã
// ---------------------------------------------------------------------------------------------------

/**
 * Bộ phân tích khung bằng ML Kit. Mỗi khung → [InputImage] (kèm góc xoay) → [BarcodeScanner.process] (BẤT ĐỒNG
 * BỘ trên luồng nền của ML Kit). Có mã QR → giao CHUỖI đầu tiên đọc được. Phải ĐÓNG [ImageProxy] ở
 * `onComplete` để khung kế tiếp được cấp; với STRATEGY_KEEP_ONLY_LATEST, ML Kit xử lý tuần tự từng khung MỚI NHẤT.
 *
 * ML Kit tự lo xoay ảnh, nhị phân hoá thích nghi & dò vị trí bằng mô hình học sẵn → bắt cả mã mờ, nghiêng, nhỏ,
 * tương phản thấp hay LỆCH TÂM (không cần cắt vùng giữa hay đảo sáng thủ công như ZXing trước đây).
 */
@ExperimentalGetImage
private class QrMlKitAnalyzer(
    private val scanner: BarcodeScanner,
    private val onDecoded: (String) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val media = image.image
        if (media == null) {
            image.close()
            return
        }
        val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }?.let(onDecoded)
            }
            // Đóng ImageProxy DUY NHẤT ở đây (chạy sau cả thành công lẫn thất bại) → không rò khung, không kẹt luồng.
            .addOnCompleteListener { image.close() }
    }
}

/** Giải mã QR từ ảnh trong thư viện (thu nhỏ ảnh lớn để nhanh & tránh tràn bộ nhớ) — cũng qua ML Kit. */
private suspend fun decodeImageUri(context: Context, uri: Uri, scanner: BarcodeScanner): String? = withContext(Dispatchers.IO) {
    val bitmap = runCatching {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // cần đọc pixel → không dùng bitmap phần cứng
            decoder.isMutableRequired = false
            val maxDim = maxOf(info.size.width, info.size.height)
            if (maxDim > 2000) decoder.setTargetSampleSize((maxDim + 1999) / 2000)
        }
    }.getOrNull() ?: return@withContext null
    try {
        // Tasks.await: chờ ML Kit xong trên luồng IO này (bọc trong try để lỗi/không-thấy-mã đều trả null gọn gàng).
        val barcodes = Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)))
        barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
    } catch (e: Exception) {
        null
    } finally {
        bitmap.recycle()
    }
}

/** Truy ngược Activity từ Context (qua chuỗi ContextWrapper) để điều khiển thanh hệ thống của cửa sổ activity. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
