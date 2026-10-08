# Tài Liệu Tổng Quan Dự Án - Booking Schedule (Backend)

Tài liệu này được thiết kế tối ưu để làm ngữ cảnh (context) cho NotebookLLM hoặc bất kỳ AI nào khác khi cần phân tích, hiểu về cấu trúc, nghiệp vụ và tiến độ hiện tại của dự án. 

## 1. Thông Tin Chung
- **Tên dự án:** Booking Schedule (Hệ thống đặt lịch khám bệnh / phòng khám)
- **Tính chất:** Đồ án tốt nghiệp.
- **Vai trò:** Backend System (Cung cấp RESTful API).

## 2. Công Nghệ Sử Dụng (Tech Stack)
- **Ngôn ngữ:** Java 21
- **Framework chính:** Spring Boot 3.2.5
- **Database:** MySQL
- **ORM:** Spring Data JPA / Hibernate
- **Security:** Spring Security, OAuth2 Resource Server, Spring Cloud OpenFeign
- **Thư viện hỗ trợ:** Lombok, MapStruct, Spring Validation, Spotless.
- **Build tool:** Maven

## 3. Kiến Trúc Dự Án
Dự án được tổ chức theo kiến trúc MVC / Layered Architecture chuẩn của Spring Boot:
- `controller/`: Chứa các RestController định nghĩa các API endpoints.
- `service/`: Chứa interface và implementation xử lý business logic.
- `repository/`: Kế thừa JpaRepository để thao tác với cơ sở dữ liệu.
- `entity/`: Các lớp ánh xạ trực tiếp với các bảng trong MySQL.
- `dto/`: Phân chia thành `request` và `response` để hứng và trả dữ liệu.
- `mapper/`: Sử dụng MapStruct để map giữa Entity và DTO.
- `exception/`: Chứa GlobalExceptionHandler và các Custom Exception (AppException, ErrorCode) để chuẩn hóa định dạng lỗi trả về.
- `enums/`: Chứa các giá trị cố định (Role, Status,...).

## 4. Phân Tích Cơ Sở Dữ Liệu & Thực Thể (Domain Models)
Dự án bao gồm các thực thể chính sau, phản ánh quy trình nghiệp vụ của phòng khám:

1. **User / Actor (Người dùng hệ thống):**
   - `User`: Chứa thông tin tài khoản và cá nhân (username, password, email, phone...).
   - `Role` & `ERole`: Phân quyền người dùng gồm `SUPER_ADMIN`, `ADMIN`, `STAFF`, `DOCTOR`, `PATIENT`.
   - `Doctor`: Bác sĩ, có liên kết 1-1 với `User` và thuộc về một `Specialty` (Chuyên khoa).
   - `Patient`: Bệnh nhân, liên kết 1-1 với `User`.

2. **Cơ sở vật chất & Dịch vụ (Master Data):**
   - `Specialty`: Chuyên khoa của phòng khám (Ví dụ: Mắt, Da liễu, Răng hàm mặt...).
   - `Service`: Dịch vụ khám/chữa bệnh (Khám tổng quát, Xét nghiệm máu...).
   - `Room`: Phòng khám bệnh.

3. **Lịch khám & Đặt lịch (Core Business):**
   - `DoctorSchedule`: Lịch làm việc của bác sĩ theo ngày.
   - `TimeSlot`: Các khung giờ cụ thể trong một ngày (Ví dụ: 08:00 - 08:30).
   - `Appointment`: Cuộc hẹn/đơn đặt lịch khám. Liên kết với `Patient`, `Doctor`, `Service`, `TimeSlot`. 
     - Trạng thái (`EAppointmentStatus`): `PENDING` (chờ xác nhận), `CONFIRMED` (đã xác nhận), `IN_PROGRESS` (đang khám), `COMPLETED` (đã xong), `CANCELLED` (đã hủy), `NO_SHOW` (không đến).

4. **Tài chính & Thanh toán:**
   - `Payment`: Giao dịch thanh toán cho `Appointment`. Trạng thái: `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`.
   - `Promotion`: Mã giảm giá, khuyến mãi có thể áp dụng khi thanh toán.

5. **Hệ thống hỗ trợ:**
   - `Review`: Đánh giá của bệnh nhân về bác sĩ/dịch vụ sau khi khám xong.
   - `Notification`: Thông báo hệ thống cho người dùng.
   - `SystemSetting`: Cài đặt cấu hình chung của hệ thống.

## 5. Tình Trạng Hiện Tại (Current Progress)
- **Đã hoàn thiện:**
  - Khởi tạo thành công cấu trúc chuẩn của dự án Spring Boot.
  - Khai báo đầy đủ toàn bộ các lớp `Entity`, `Enum`, `Repository` và `DTO`.
  - Thiết lập thành công `GlobalExceptionHandler` và chuẩn hóa định dạng trả về chung qua class `ApiResponse`.
  - Khởi tạo module `User` (`UserController`, `UserService`) với các chức năng CRUD cơ bản (thêm, sửa, xóa người dùng).
- **Chưa hoàn thiện:** 
  - Các module còn lại, luồng đăng nhập (Authentication), nghiệp vụ đặt lịch (Booking) và tích hợp các bên thứ 3 (Thanh toán, Gửi Email...).

---

## 6. Lộ Trình Phát Triển Cụ Thể (Next Implementation Steps)

Dưới đây là các bước cụ thể tiếp theo để hoàn thiện đồ án từ đầu đến cuối:

### Bước 1: Thiết lập Authentication & Authorization (Bảo mật)
- Code tính năng Đăng nhập, trả về Token.
- Cấu hình Spring Security kết hợp với OAuth2 Resource Server để validate JWT.
- Phân quyền (Role-based access control) cho các API:
  - Admin/Staff: Toàn quyền hệ thống.
  - Doctor: Chỉ được xem lịch và cập nhật trạng thái khám của mình.
  - Patient: Tự đặt lịch, xem lịch sử cá nhân.

### Bước 2: Hoàn thiện CRUD cho Danh mục (Master Data)
- Viết Controller và Service quản lý `Specialty`, `Service`, `Room`.
- Viết API quản lý `Promotion` và `SystemSetting`.
*(Làm tương tự như UserController hiện tại để quen tay).*

### Bước 3: Nghiệp vụ Quản lý Bác sĩ & Lịch làm việc
- Viết API để Admin/Staff tạo profile `Doctor` (Tạo User xong tự sinh Doctor profile).
- Cấu hình API để Bác sĩ hoặc Staff đăng ký lịch làm việc (`DoctorSchedule`).
- Logic sinh ra các `TimeSlot` tương ứng tự động khi lịch làm việc được tạo thành công.

### Bước 4: Core Business - Luồng Đặt Lịch Khám (Booking Flow)
Đây là nghiệp vụ quan trọng nhất trong đồ án:
- **API Lấy danh sách khung giờ trống:** Bệnh nhân chọn Bác sĩ/Chuyên khoa và Ngày, hệ thống trả về các `TimeSlot` chưa bị ai đặt.
- **API Tạo Appointment (Đặt lịch):**
  - Bệnh nhân gửi request.
  - Hệ thống kiểm tra `TimeSlot` còn trống không (để chống booking trùng).
  - Khóa (Lock) `TimeSlot` lại.
  - Tạo bản ghi `Appointment` với trạng thái `PENDING`.

### Bước 5: Luồng Thanh Toán (Payment)
- Viết module tính toán số tiền khám (giá Service trừ đi Promotion nếu có).
- Sinh bản ghi `Payment` với trạng thái `PENDING`.
- Cấu hình tích hợp VNPay hoặc MoMo Sandbox (hoặc làm thanh toán Mock nếu không bắt buộc).
- Viết webhook/callback để khi thanh toán `SUCCESS`, trạng thái của `Appointment` được tự động chuyển sang `CONFIRMED`.

### Bước 6: Quản lý Trạng thái Khám Bệnh & Đánh giá (Review)
- API cho Bác sĩ cập nhật trạng thái `Appointment` (từ `CONFIRMED` -> `IN_PROGRESS` -> `COMPLETED`).
- Khi trạng thái là `COMPLETED`, mở khóa tính năng cho phép Bệnh nhân gửi `Review` đánh giá bác sĩ.

### Bước 7: Hệ thống Thông báo (Notification)
- Cài đặt lưu thông báo vào database bảng `Notification` khi lịch được đặt, bị hủy, hoặc sắp đến hạn.
- (Nâng cao): Gửi Email nhắc lịch bằng `JavaMailSender` (hoặc gửi qua RabbitMQ/Kafka nếu muốn show skill).

### Bước 8: Báo Cáo & Thống Kê (Dashboard / Analytics)
- API thống kê doanh thu phòng khám theo khoảng thời gian (từ ngày... đến ngày...).
- API thống kê top bác sĩ được book nhiều nhất.
- API thống kê tỷ lệ trạng thái lịch hẹn (Số lịch bị hủy vs số lịch thành công).
