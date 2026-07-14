# callHS — Nhật ký cuộc gọi

Ứng dụng Android **production**, tập trung 100% vào **một tính năng: xem nhật ký & chi tiết cuộc gọi**,
giao diện Material 3 "expressive" theo phong cách app Điện thoại của Google (không có bottom nav).

> Ứng dụng **chỉ đọc** nhật ký cuộc gọi (`READ_CALL_LOG`). Không ghi/sửa/xoá dữ liệu hệ thống.
> Các hành động (Gọi/Nhắn tin) chỉ **mở app hệ thống qua Intent** — không tự gọi/gửi.

## Tính năng
**Màn danh sách (Call history):**
- Tiêu đề + **vuốt xuống để làm mới** (pull-to-refresh); danh sách còn **tự nạp lại** khi nhật ký cuộc gọi trên máy thay đổi.
- **Thanh tìm kiếm** theo tên/số + **tìm bằng giọng nói** (mic → nhận dạng giọng nói).
- **Bộ lọc chip**: Tất cả • Nhỡ • Đến • Đi.
- **Nhóm theo ngày**: Hôm nay / Hôm qua / Trước đó trong tuần / Trước đó.
- Mỗi dòng: avatar (ảnh liên hệ → chữ cái → icon người), tên/số, `[mũi tên] loại cuộc gọi • thời lượng`
  (đi = xanh lá, đến = xanh dương, nhỡ/từ chối = đỏ), thời gian, **nút gọi** xanh.

**Màn chi tiết:**
- **Top bar co giãn** khi cuộn: mặc định hiện số; cuộn lên hiện avatar + tên + số · khu vực. Nút quay lại • Yêu thích • Sao chép số.
- Avatar lớn, tên, **số định dạng `0123 456 789`**, chip loại số (Di động) / nhà mạng / khu vực / SIM. Nhấn đúp header để sao chép.
- **Thẻ hành động**: số + **Gọi** • **Nhắn tin** • Cuộc gọi video • Thêm vào liên hệ • Tìm qua Zalo.
- **Thẻ "Lịch sử cuộc gọi"**: 6 cuộc gần nhất (bấm để mở rộng chi tiết) + "Hiển thị thêm" → màn tất cả cuộc gọi.
- **Thẻ "Công cụ"**: Chia sẻ liên hệ • Chặn số.
- **Bottom sheet "Tuần này"**: kéo lên xem Tổng cuộc gọi + Tổng thời lượng (7 ngày gần nhất).

Hành động thật: Gọi → trình quay số (`ACTION_DIAL`), Nhắn tin → app SMS (`ACTION_SENDTO`), Sao chép số,
Chia sẻ số (`ACTION_SEND`), **Tìm qua Zalo** (mở thẳng Zalo tới số; chưa cài thì mở zalo.me). Các mục
Video/Chặn số/Thêm liên hệ hiện **toast "sẽ sớm cập nhật"** — giữ đúng bố cục UI, chưa tự thay đổi dữ liệu hệ thống.

## Tech stack
- Kotlin 2.2.10, Jetpack Compose (BOM 2026.06.00), Material 3, Material Icons Extended
- Navigation Compose 2.9.8, ViewModel + Coroutines, edge-to-edge (`enableEdgeToEdge`)
- AGP 9.1.1, Gradle 9.3.1, JDK 21 — `compileSdk 36.1`, `minSdk 29`, `targetSdk 36`
- Nhật ký cuộc gọi CHỈ ĐỌC từ hệ thống. Ảnh liên hệ tải bằng `ContentResolver` (downsample tránh OOM).
- Tông màu: nền sáng, xanh lá (gọi đi / nút gọi), xanh dương (gọi đến), đỏ (nhỡ) — theo thiết kế Google Phone.

## Cấu trúc
```
app/src/main/java/com/antimobile/callhs/
├─ MainActivity.kt              (enableEdgeToEdge + nền trắng gốc + AppNav)
├─ data/
│  ├─ model/CallModels.kt
│  └─ repository/  CallLogRepository (loadRecent / loadDetail, gán nhãn SIM)
├─ util/   TimeFormat, Permissions, CallActions, PhoneFormat, Carrier, DeviceInfo, CallResult
└─ ui/
   ├─ theme/        Color, Theme, Type  (tông xanh-lá/xanh-dương/đỏ, tắt dynamic color)
   ├─ components/   CallUi.kt (avatar, icon loại, các state, xin quyền) • StatsSheet.kt ("Tuần này")
   ├─ navigation/   AppNav.kt  (list → detail → allcalls, hiệu ứng chuyển màn)
   ├─ calllist/     CallListScreen + CallListViewModel + ListCallItem
   ├─ calldetail/   CallDetailScreen + CallDetailViewModel + DetailCallItem
   └─ callhistory/  AllCallsScreen + AllCallsCallItem  (xem tất cả cuộc gọi của 1 số)
```

## Build & chạy
```
./gradlew :app:assembleDebug
./gradlew :app:installDebug      # khi có thiết bị/emulator
```

## Ghi chú kỹ thuật
- AGP 9 dùng Kotlin tích hợp sẵn; `androidx.lifecycle` ghim 2.9.4 (2.11.0 đòi compileSdk 37).
- Provider call log **không nhận `LIMIT` trong sortOrder** → giới hạn số dòng trong code.
- **Tương thích Android 10→16 (API 29-36):** đã rà toàn bộ. Edge-to-edge bật cho mọi phiên bản;
  không phụ thuộc package-visibility (Android 11+); mọi intent bọc `runCatching`; cột CallLog đọc an toàn.
- **Quyền `READ_CALL_LOG`:** khi bị từ chối vĩnh viễn (Android 11+ bỏ ô "Không hỏi lại"), nút CTA đổi
  thành **"Mở Cài đặt"** → tránh kẹt cứng ở màn xin quyền; quay lại từ Cài đặt tự nạp dữ liệu (`ON_START`).
- Đã verify end-to-end trên emulator `Medium_Phone_API_36.1` (danh sách, chi tiết, thống kê).
