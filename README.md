# CallHS — trợ lý cuộc gọi cục bộ

CallHS là ứng dụng Android viết bằng Kotlin/Jetpack Compose để xem, phân tích và tổ chức lịch sử
cuộc gọi. Ứng dụng theo hướng **local-first**: nhật ký cuộc gọi và danh bạ được đọc trực tiếp từ
provider của Android; dữ liệu do người dùng tạo được lưu cục bộ trên thiết bị.

> CallHS không thay thế ứng dụng Điện thoại mặc định. Riêng tính năng chặn cuộc gọi sử dụng
> `ROLE_CALL_SCREENING` và `CallScreeningService` chính thức của Android sau khi người dùng chủ
> động cấp vai trò này.

## Tính năng chính

- Nhật ký cuộc gọi có tìm kiếm, tìm bằng giọng nói, lọc loại cuộc gọi, phạm vi SIM, nhóm theo ngày
  và tự làm mới khi provider hệ thống thay đổi.
- Chi tiết số điện thoại, dòng thời gian, thống kê theo số/SIM/nhà mạng, chi phí ước tính và phân
  tích các số gọi lặp.
- Danh bạ chỉ đọc, hiển thị tên/ảnh, tìm kiếm không dấu và các hành động mở ứng dụng hệ thống.
- Nhóm phân loại số điện thoại do người dùng tự tạo, có icon, màu, thành viên và menu nhấn giữ.
- Mẫu tin nhắn, QR, chia sẻ liên hệ, tra cứu Zalo/web, danh bạ cơ quan và các tiện ích liên quan.
- Theme sáng/tối/hệ thống, cỡ chữ tuỳ chỉnh và giao diện song ngữ Việt/Anh.
- Sao lưu/khôi phục có chọn từng nhóm dữ liệu và ba chế độ `REPLACE`, `ADD`, `UPDATE`.
- Chặn cuộc gọi và spam theo quy tắc, có lịch sử riêng, thông báo và nhiều phương thức xử lý.

## Chặn cuộc gọi và spam

Từ Cài đặt, người dùng mở màn Chặn cuộc gọi và chọn CallHS làm ứng dụng sàng lọc cuộc gọi nếu
vai trò chưa được cấp. Sau đó có thể:

- Màn chính có thẻ **Tìm hiểu cách CallHS chặn cuộc gọi** để giải thích thứ tự kiểm tra, bốn khu vực
  quản lý rõ ràng: **Danh sách cho phép**, **Danh sách chặn**, **Xử lý theo danh bạ**,
  **Quy tắc nâng cao**, và mục **Các vấn đề thường gặp** để tự kiểm tra lỗi chặn/thông báo.
- Nhập tay hoặc chọn số từ Danh bạ/Call Log. Hai picker chỉ là nguồn; mỗi số được lưu thành một mục
  độc lập. Chuyển giữa Danh sách cho phép và Danh sách chặn là transaction, không tạo dữ liệu chồng nhau.
- Chọn cách xử lý riêng cho số trong Danh bạ và số ngoài Danh bạ bằng bottom sheet có nhãn đầy đủ.
  Hai danh sách số cụ thể luôn được kiểm tra trước các lựa chọn này.
- Tạo rule nâng cao với action `BLOCK/ALLOW`, scope `Trong danh bạ/Ngoài danh bạ/Tất cả` và matcher
  đầu số, đuôi số, chuỗi chứa, độ dài, nhà mạng, vùng quốc gia hoặc điều kiện đặc biệt.
- Bộ lọc cuộc gọi có dấu hiệu spam là một rule `BLOCK` opt-in. Bộ lọc dùng các tín hiệu cục bộ: số Việt Nam
  hoàn chỉnh thuộc nhóm `022`, `023`, `024`, `028`, `059`, `099`; đầu số di động Việt Nam mà danh mục
  CallHS chưa nhận diện; hoặc Android 11+ báo xác minh số gọi thất bại. Đây không phải danh sách số lừa đảo
  đã được xác nhận và có thể chặn nhầm số hợp lệ; Danh sách cho phép luôn được ưu tiên. Lịch sử ghi chính xác
  dấu hiệu đã khớp bằng reason codec versioned; nguồn và giới hạn được ghi tại tài liệu kiến trúc.
- Sắp thứ tự rule nâng cao; rule đầu tiên khớp được áp dụng.
- Với mã gọi, chọn preset quốc tế ngoài `+84`, từng mã E.164 được hỗ trợ hoặc đầu số Việt Nam
  `024`, họ `022x`, `028`, `059`, `099`.
- Chọn số từ 1.000 cuộc gọi gần nhất bằng tìm kiếm, lọc loại/SIM và timeline như CallListScreen.
- Chọn phương thức: chặn và từ chối, chặn không gửi tín hiệu từ chối hoặc chỉ tắt tiếng.
- Xem hai tab Quy tắc/Lịch sử, mở chi tiết số và nhấn giữ item để chuyển danh sách, bật/tắt, sắp thứ tự
  hoặc xoá bằng `ContextMenuOverlay` chuẩn của app.
- Nút cài đặt trên thanh tiêu đề mở màn riêng cho bảo vệ/pause, phương thức và notification.
- Tạm ngưng bảo vệ trong 10 phút, 30 phút hoặc 1 giờ, có mốc bắt đầu/kết thúc và đồng hồ đếm ngược;
  hết giờ thì bộ chặn tự hoạt động lại.
- Chế độ **Chặn đến khi gọi lặp** chỉ chạy khi số không nằm trong hai danh sách, không khớp Quy tắc
  nâng cao và Danh bạ xác nhận `NOT_IN_CONTACTS`: chặn trước ngưỡng 2/3/4 rồi cho qua trong cửa sổ phút đã nhập.
- Notification chỉ còn **Tắt** hoặc **Mỗi lần chặn**. Channel `blocked_calls_urgent_sound_v3` dùng
  `IMPORTANCE_HIGH`, heads-up, rung và WAV riêng; app đăng cảnh báo ngay sau response Telecom rồi cập nhật
  count im lặng sau khi Room hoàn tất.
- Sao lưu danh sách số, action/scope/order của quy tắc, cấu hình và lịch sử bằng backup format v4.
- Room dùng schema phát triển version 1 chứa trực tiếp cấu trúc mới nhất; chưa duy trì migration trước khi
  ứng dụng được public.

Thứ tự cố định là: bảo vệ → Danh sách cho phép → Danh sách chặn → lựa chọn Trong/Ngoài danh bạ
→ quy tắc nâng cao đầu tiên khớp → xử lý số ngoài danh bạ gọi lặp → mặc định cho qua. Lookup Danh bạ
`UNKNOWN` không bao giờ được hiểu là “ngoài danh bạ”.

`SILENCE_ONLY` và mọi kết quả `ALLOW` không được tính là sự kiện đã chặn, vì vậy không tăng lịch sử hoặc phát
thông báo “đã chặn”. Chi tiết kiến trúc, storage key, giới hạn nền tảng và chuẩn UI nằm tại
[docs/CALL_BLOCKING.md](docs/CALL_BLOCKING.md).

> Giới hạn Android: AOSP thông thường không chuyển cuộc gọi ẩn số hoặc cuộc gọi non-`tel`/VoIP
> của ứng dụng khác tới `CallScreeningService`. Hai điều kiện này chỉ hoạt động best-effort trên
> thiết bị/OEM có cung cấp callback tương ứng và không được bảo đảm.

> Android chỉ chuyển cuộc gọi từ số đã lưu tới `CallScreeningService` khi `READ_CONTACTS` còn hiệu lực.
> Vì vậy quyền Danh bạ là một phần của độ bao phủ sàng lọc, không chỉ dùng để hiển thị tên và ảnh.

> Quy tắc mã gọi chỉ đối chiếu số caller ID mà Android chuyển cho ứng dụng. Caller ID có thể bị giả mạo;
> mã gọi khớp không xác minh vị trí, danh tính, nhà mạng hoặc kết luận một cuộc gọi là lừa đảo.

## Dữ liệu và quyền riêng tư

| Dữ liệu | Nguồn | Cách sử dụng |
|---|---|---|
| Nhật ký cuộc gọi | `CallLog` hệ thống | Chỉ đọc; CallHS không sửa, xoá hoặc khôi phục vào provider hệ thống |
| Danh bạ | `ContactsContract` | Chỉ đọc; dùng để hiển thị, chọn rule và xác định số lạ |
| Danh sách số, rule và lịch sử chặn | Room `callhs.db` | Dữ liệu app sở hữu, có thể sao lưu/khôi phục |
| Cấu hình theme, ngôn ngữ, bộ chặn | `SharedPreferences` | Lưu cục bộ; phần được hỗ trợ có thể đưa vào backup |

Quyền chính gồm `READ_CALL_LOG`, `READ_CONTACTS`, `READ_PHONE_STATE`, `READ_PHONE_NUMBERS`,
`CAMERA` và `POST_NOTIFICATIONS`. Vai trò sàng lọc cuộc gọi được xin riêng qua `RoleManager`,
không phải quyền ngầm và không biến CallHS thành default dialer.

## Tech stack

- Kotlin 2.2.10, Jetpack Compose BOM 2026.06.00, Material 3 và Material Icons Extended.
- Navigation Compose 2.9.8, Lifecycle/ViewModel + Coroutines.
- Room 2.7.2 với `exportSchema=true`; không dùng destructive migration.
- CameraX, ML Kit Barcode Scanning và ZXing cho luồng QR.
- AGP 9.1.1, Gradle 9.3.1, Java 11 bytecode; `compileSdk 36.1`, `minSdk 29`, `targetSdk 36`.

## Cấu trúc chính

```text
app/src/main/java/com/antimobile/callhs/
├─ MainActivity.kt
├─ data/
│  ├─ repository/        Nhật ký cuộc gọi hệ thống
│  ├─ contacts/          Danh bạ chỉ đọc, tìm kiếm và lập chỉ mục
│  ├─ local/             Room: nhóm phân loại, danh sách số, rule và lịch sử chặn
│  ├─ blocking/          Matcher, repository, settings, notifier, screening service
│  ├─ backup/            Backup JSON, parser và restore theo section
│  ├─ agency/            Danh bạ cơ quan
│  ├─ legal/             Nội dung pháp lý
│  └─ donate/            Dữ liệu luồng ủng hộ
├─ i18n/                 Hợp đồng chuỗi + bản dịch Việt/Anh
├─ ui/
│  ├─ calllist/          Danh sách cuộc gọi và context menu chuẩn
│  ├─ calldetail/        Chi tiết cuộc gọi và công cụ
│  ├─ callhistory/       Toàn bộ cuộc gọi của một số
│  ├─ contacts/          Danh bạ
│  ├─ category/          Danh sách và editor nhóm
│  ├─ blocking/          Tổng quan, hướng dẫn quy trình, danh sách số, xử lý theo danh bạ, rule và lịch sử
│  ├─ settings/          Cài đặt, backup, ngôn ngữ, theme, cỡ chữ
│  ├─ stats/…            Các màn thống kê
│  ├─ components/        Component dùng chung đã duyệt
│  ├─ navigation/        AppNav và routes
│  └─ theme/             Palette, typography và theme
└─ util/                 Chuẩn hoá số, SIM, QR, SMS, Intent và tiện ích thuần
```

## Chuẩn UI dùng chung

- Dùng palette, typography và font scale trong `ui/theme`; không tạo tone màu hoặc cỡ chữ riêng.
- Dialog/sheet dùng `AppDialog` và `AppBottomSheet`; item lựa chọn dùng `FilterOptionRow` theo sheet
  lọc cuộc gọi.
- Tab chính và lựa chọn ngưỡng ngắn dùng `Segmented`; lựa chọn có nội dung dài dùng
  `AppBottomSheet` + `FilterOptionRow`. Card dùng `PanelCard` và press highlight hiện có.
- Menu nhấn giữ phải theo `ListCallItem` + `ContextMenuOverlay`, bao gồm cách đo bounds, ẩn item gốc
  và dựng lifted item không có padding kép.
- Màn editor phải theo `CategoryEditorScreen`: top bar/insets, bottom save bar, đóng IME khi Done hoặc
  tap ngoài và nội dung vẫn cuộn được khi bàn phím mở.
- Mọi text người dùng nhìn thấy phải đi qua hợp đồng i18n; storage key không được dịch.

## Build và kiểm thử

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug       # khi có thiết bị/emulator
```

Room schema mới nhất được xuất duy nhất tại `app/schemas/.../1.json`. Trong giai đoạn chưa public,
thay đổi entity được cập nhật trực tiếp vào baseline v1 và dữ liệu cài thử được tạo mới.

## Tài liệu chuyên sâu

- [Kiến trúc tính năng chặn cuộc gọi](docs/CALL_BLOCKING.md)
