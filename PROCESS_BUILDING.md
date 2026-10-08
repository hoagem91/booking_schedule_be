# Quá Trình Xây Dựng Dự Án

## Cập nhật `UserService.java`
**Mục tiêu:** Xử lý và hoàn thiện các chức năng liên quan đến tài khoản người dùng trong UserService.

**Các bước đã thực hiện:**
1. **Kiểm tra trạng thái hiện tại:** Đọc và phân tích file `UserService.java` hiện tại, bao gồm các phương thức đã có (`createUser`, `updateUser`, `deleteUser`).
2. **Xác định các chức năng còn thiếu:** Nhận thấy `UserService` thiếu các phương thức cơ bản để lấy danh sách người dùng, lấy thông tin một người dùng cụ thể, và lấy thông tin của người dùng đang đăng nhập.
3. **Bổ sung các phương thức:**
   - Thêm phương thức `getUsers()`: Trả về danh sách tất cả các tài khoản người dùng có trong hệ thống (dưới dạng `UserResponse`).
   - Thêm phương thức `getUser(String id)`: Truy vấn và trả về thông tin chi tiết của một tài khoản dựa trên ID. Ném ra ngoại lệ `USER_NOT_EXISTED` nếu không tìm thấy.
   - Thêm phương thức `getMyInfo()`: Lấy thông tin user hiện tại đang được xác thực trong phiên đăng nhập (thông qua `SecurityContextHolder`), và trả về dữ liệu profile của họ.

**Kết quả:**
- `UserService.java` đã được hoàn thiện hơn, hỗ trợ đầy đủ các thao tác CRUD cơ bản và lấy thông tin tài khoản cá nhân an toàn qua Spring Security Context.

---

## Xây dựng `RoleService` và `RoleController`
**Mục tiêu:** Quản lý các vai trò (roles) trong hệ thống và cung cấp API để phía client có thể lấy danh sách role (thường dùng cho các dropdown chọn role).

**Các bước đã thực hiện:**
1. **Sửa lỗi MapStruct trong `RoleMapper.java`:** Chuyển annotation `@Mapping(componentModel = "spring")` thành `@Mapper(componentModel = "spring")` để MapStruct có thể tự động generate implementation chính xác.
2. **Tạo `RoleService.java`:** Xây dựng service cho Role, inject `RoleRepository` và `RoleMapper`. Viết phương thức `getAllRoles()` để lấy toàn bộ danh sách Role hiện có trong cơ sở dữ liệu và chuyển chúng thành dạng `RoleResponse`.
3. **Tạo `RoleController.java`:** Xây dựng REST API Controller tại endpoint `/roles`. Inject `RoleService` vào controller và xây dựng API `GET` để trả về danh sách role được bọc trong đối tượng `ApiResponse`.

**Kết quả:**
- Đã có luồng chuẩn từ Controller -> Service -> Repository cho entity Role. Client có thể gọi `GET /roles` để lấy dữ liệu.

---

## Xây dựng `AuthenticationService` và `AuthenticationController`
**Mục tiêu:** Xây dựng toàn bộ luồng xác thực JWT cho hệ thống, bao gồm: Đăng nhập, Kiểm tra Token, Đăng xuất, và Làm mới Token.

**Kiến trúc tổng quan:**
```
AuthenticationController  →  AuthenticationService  →  UserRepository / InvalidatedTokenRepository
                                                      ↕
                                               Nimbus JOSE JWT (ký & xác minh token)
CustomJwtDecoder  →  AuthenticationService (introspect)  →  InvalidatedTokenRepository (blacklist check)
```

**Các bước đã thực hiện:**

### 1. Chuẩn bị cấu hình `application.yaml`
- Mở comment và kích hoạt khối cấu hình JWT:
  - `jwt.signerKey`: Khóa bí mật ký token (HMAC-SHA512).
  - `jwt.valid-duration`: Thời hạn Access Token (3600 giây = 1 giờ).
  - `jwt.refreshable-duration`: Thời gian còn hiệu lực để làm mới (36000 giây = 10 giờ).

### 2. Tạo Entity `InvalidatedToken.java`
- Thực thể lưu danh sách đen (blacklist) các token đã bị thu hồi.
- Trường `id` là `jti` (JWT ID – định danh duy nhất của mỗi token).
- Trường `expiryTime` lưu thời điểm token hết hạn, phục vụ việc dọn dẹp DB sau này.

### 3. Tạo `InvalidatedTokenRepository.java`
- Repository JPA đơn giản cho entity `InvalidatedToken`, kế thừa `JpaRepository<InvalidatedToken, String>`.

### 4. Tạo các DTO Request
- `AuthenticationRequest.java`: Chứa `username` và `password` cho chức năng đăng nhập.
- `IntrospectRequest.java`: Chứa `token` để kiểm tra tính hợp lệ.
- `LogoutRequest.java`: Chứa `token` để đưa vào blacklist khi đăng xuất.
- `RefreshTokenRequest.java`: Chứa `token` cũ để đổi lấy token mới.

### 5. Tạo các DTO Response
- `AuthenticationResponse.java`: Trả về `token`, `expiryTime`, và `authenticated`.
- `IntrospectResponse.java`: Trả về `valid` (boolean).

### 6. Xây dựng `AuthenticationService.java` (trái tim của module)
Bốn luồng logic cốt lõi:

| Phương thức | Mô tả |
|---|---|
| `authenticate()` | Xác minh username/password, tạo JWT, trả về token và thời hạn |
| `introspect()` | Xác minh chữ ký, kiểm tra hết hạn, kiểm tra blacklist. Trả về `valid` |
| `logout()` | Parse token → lấy `jti` + `exp` → lưu vào `invalidated_tokens` |
| `refreshToken()` | Kiểm tra token cũ → đưa vào blacklist → query user mới → cấp token mới |

- Phương thức nội bộ `generateToken(User)`: Tạo JWT HS512 với claim `scope` (danh sách role dạng `SCOPE_ADMIN SCOPE_PATIENT`), `jti`, `sub`, `iss`, `iat`, `exp`.
- Phương thức nội bộ `verifyToken(token, allowExpired)`: Tập trung logic xác minh, tránh lặp code.
- Phương thức nội bộ `buildScope(User)`: Build chuỗi scope từ danh sách role của user.

### 7. Xây dựng `AuthenticationController.java`
- Mapping tại `/auth`.
- 4 endpoint POST: `/token`, `/introspect`, `/logout`, `/refresh`.
- Tất cả đều là `PUBLIC_ENDPOINTS` (không cần auth) theo cấu hình trong `SecurityConfig`.

### 8. Cập nhật `CustomJwtDecoder.java`
- Thay thế implementation thủ công bằng `NimbusJwtDecoder` chính thức (lazy init).
- Tích hợp gọi `authenticationService.introspect()` trước khi decode để kiểm tra blacklist.
- Đảm bảo mọi request đã đăng xuất sẽ bị từ chối ngay tại bước decode của Spring Security.

**Kết quả:**
- Luồng xác thực JWT hoàn chỉnh theo chuẩn stateless + blacklist-based logout.
- API sẵn sàng:
  - `POST /booking-schedule/auth/token` → Đăng nhập
  - `POST /booking-schedule/auth/introspect` → Kiểm tra token
  - `POST /booking-schedule/auth/logout` → Đăng xuất
  - `POST /booking-schedule/auth/refresh` → Làm mới token

---

## Khởi tạo dữ liệu mẫu (Database Initialization)
**Mục tiêu:** Xử lý vấn đề thiếu dữ liệu mẫu (Role chưa được tạo) trong database khi hệ thống mới chạy, dẫn đến việc không thể gán Role khi tạo User và `RoleController` trả về rỗng.

**Các bước đã thực hiện:**
1. **Tạo `DatabaseInitializer.java`:** Một class component kế thừa `CommandLineRunner` để tự động chạy khi ứng dụng khởi động.
2. **Khởi tạo danh sách Role:** Quét qua toàn bộ enum `ERole` (SUPER_ADMIN, ADMIN, STAFF, DOCTOR, PATIENT), nếu trong Database chưa có role nào, hệ thống sẽ tự động thêm role đó vào bảng `roles`. Điều này giải quyết triệt để lỗi logic khi gọi `getAllRoles()` hoặc khi `createUser()` cần gán role mặc định.
3. **Khởi tạo tài khoản Admin mặc định:** Nếu chưa có user `admin`, hệ thống sẽ tự động tạo một tài khoản có username là `admin`, password là `admin`, và gán hai Role `ADMIN`, `SUPER_ADMIN`.

**Kết quả:**
- Khi chạy lại dự án, `getAllRoles()` sẽ trả về đầy đủ các Roles theo Enum.
- Người dùng đăng ký sẽ tự động được gán Role `PATIENT` (vì role này đã được khởi tạo sẵn).
- Có ngay một tài khoản Admin để đăng nhập, quản lý và thay đổi quyền của các tài khoản khác thông qua API cập nhật user.

---

## Phân tích và Sửa nghiệp vụ `PatientService`

**Mục tiêu:** Phân tích lại toàn bộ nghiệp vụ liên quan đến entity `Patient`, tìm và sửa các lỗi logic nghiêm trọng.

### Phân tích nghiệp vụ (Business Analysis)

**Câu hỏi đặt ra:** *"Nếu lấy userId để tạo Patient thì sẽ lấy ở đâu?"*

**Trả lời:** Trong hệ thống booking khám bệnh, `Patient` là **hồ sơ bệnh nhân gắn với tài khoản của người dùng** theo quan hệ 1-1 (`OneToOne`). Có hai luồng chính:
- **Luồng 1 – User tự tạo hồ sơ:** User sau khi đăng ký và đăng nhập, tự điền thông tin cá nhân (ngày sinh, giới tính, địa chỉ...) để tạo hồ sơ bệnh nhân. `userId` trong trường hợp này **phải lấy từ JWT token đang đăng nhập** qua `SecurityContextHolder`, **không được phép client tự truyền** (đây là lỗ hổng bảo mật).
- **Luồng 2 – Admin quản lý:** Admin tra cứu, cập nhật, xóa hồ sơ theo `userId` truyền qua `@PathVariable`.

### Các lỗi phát hiện và cách sửa

| # | File | Lỗi | Cách sửa |
|---|---|---|---|
| 1 | `PatientCreateRequest.java` | Có trường `userId` — client truyền `userId` tùy ý → **lỗ hổng bảo mật nghiêm trọng** (user A có thể tạo hồ sơ giả mạo cho user B) | Xóa trường `userId` khỏi Request DTO. `userId` sẽ lấy từ `SecurityContextHolder` trong Service |
| 2 | `PatientRepository.java` | `findByUserId(String)` — `User.id` có kiểu `Long`, không phải `String` → **lỗi type mismatch, lỗi runtime** | Sửa tham số thành `Long`. Bỏ `@NotNull` annotation không phù hợp trên tham số repository |
| 3 | `PatientService.createPatient()` | Kiểm tra role bằng `role.getName().equals("PATIENT")` — `getName()` trả về `ERole` (enum), không phải `String` → **luôn trả về false**, không bao giờ check được role | Sửa thành `role.getName() == ERole.PATIENT` (so sánh enum) |
| 4 | `PatientService.updatePatient()` | Ném lỗi `PATIENT_ALREADY_EXISTS` khi **không tìm thấy** patient → **sai ErrorCode hoàn toàn** | Thêm `PATIENT_NOT_FOUND(2003)` vào `ErrorCode` và dùng đúng chỗ |
| 5 | `PatientService.deletePatient()` | Tương tự — ném `PATIENT_ALREADY_EXISTS` khi không tìm thấy | Sửa thành `PATIENT_NOT_FOUND` |
| 6 | `PatientService.getAllPatient()` | Ném lỗi khi danh sách rỗng → **sai nghiệp vụ**, danh sách rỗng là hợp lệ | Bỏ việc throw exception khi empty, trả về list rỗng bình thường |
| 7 | `PatientMapper.java` | Thiếu `@Mapping` để phẳng hóa `patient.user.*` → `PatientResponse.userId`, `username`... → **response trả về null** cho các field user | Thêm `@Mapping(source="user.id", target="userId")` và các field tương tự |
| 8 | `PatientController.java` | Import thừa `UserService`, `CreateUserRequest`, `UpdateUserRequest`. Truyền `userId` kiểu `String` trong `@PathVariable` trong khi cần `Long` | Xóa import thừa, sửa kiểu `@PathVariable` thành `Long` |

### Các thay đổi thực hiện

1. **`ErrorCode.java`**: Thêm `PATIENT_NOT_FOUND(2003, "Patient not found", NOT_FOUND)`. Cập nhật message mô tả rõ hơn cho các error cũ.
2. **`PatientCreateRequest.java`**: Xóa trường `userId`.
3. **`PatientRepository.java`**: Sửa kiểu tham số `findByUserId` và `existsByUserId` từ `String` thành `Long`.
4. **`PatientMapper.java`**: Thêm các `@Mapping` để phẳng hóa nested object `user` vào `PatientResponse`.
5. **`PatientService.java`**: Viết lại hoàn toàn:
   - `createPatient()`: Lấy `userId` từ `SecurityContextHolder`, sửa logic kiểm tra role bằng enum.
   - Thêm `getMyPatient()`: User tự xem hồ sơ của mình.
   - Thêm `updateMyPatient()`: User tự cập nhật hồ sơ của mình.
   - Đổi `updatePatient()` thành `updatePatientByUserId(Long userId)`: Dùng cho Admin.
   - `deletePatient(Long userId)`: Sửa kiểu tham số và ErrorCode.
   - `getAllPatient()`: Bỏ throw exception khi empty.
   - Thêm `generatePatientCode()`: Tự động sinh mã bệnh nhân theo format `BN-YYYYMMDD-<millis>`.
6. **`PatientController.java`**: Viết lại với endpoint rõ ràng hơn:
   - `POST /patients` → User tự tạo hồ sơ.
   - `GET /patients/me` → User xem hồ sơ của mình.
   - `PUT /patients/me` → User cập nhật hồ sơ của mình.
   - `GET /patients/{userId}` → Admin xem hồ sơ theo userId.
   - `PUT /patients/{userId}` → Admin cập nhật theo userId.
   - `DELETE /patients/{userId}` → Admin xóa theo userId.
   - `GET /patients/all` → Admin lấy toàn bộ danh sách.

**Kết quả:**
- Nghiệp vụ Patient đã an toàn: không thể giả mạo userId từ phía client.
- Response `PatientResponse` đã chứa đầy đủ thông tin user (username, fullName, email, phone).
- ErrorCode rõ ràng, đúng HTTP status cho từng trường hợp lỗi.

---

## Phân tích và Hoàn thiện nghiệp vụ `DoctorService` & `DoctorController`

**Mục tiêu:** Phân tích nghiệp vụ Doctor theo tài liệu đặc tả và sửa/bổ sung toàn bộ logic còn thiếu hoặc sai.

### Phân tích nghiệp vụ tổng quan

Module Doctor là module Admin-only (chỉ Admin quản lý hồ sơ bác sĩ). Khác với Patient (user tự quản lý hồ sơ của mình), bác sĩ do Admin chỉ định user và tạo hồ sơ. Hồ sơ bác sĩ liên kết với:
- `users` (1:0..1) — quan hệ 1-1 cố định, **không được đổi user sau khi tạo**
- `specialties` (N:1) — một bác sĩ thuộc một chuyên khoa, **có thể thay đổi** nhưng phải kiểm tra
- `doctor_schedules`, `appointments`, `reviews` (1:N) — dữ liệu lịch sử, quyết định luồng xóa

### Các lỗi / thiếu sót phát hiện

| # | File | Vấn đề | Cách xử lý |
|---|---|---|---|
| 1 | `ErrorCode.java` | `DOCTOR_ALREADY_EXISTS` và `DOCTOR_NOT_EXISTED` cùng code `4001` → **duplicate enum code** | Đánh lại code theo thứ tự 4001→4006, thêm `DOCTOR_HAS_HISTORY`, `DOCTOR_ALREADY_INACTIVE`, `SPECIALTY_INACTIVE` |
| 2 | `DoctorMapper.java` | Thiếu toàn bộ `@Mapping` → `user.*` và `specialty.*` không được phẳng hóa vào `DoctorResponse` → **response null** | Thêm mapping cho `user.id/username/fullName/email/phone` và `specialty.id/name`. Thêm `ignore = true` cho các field set thủ công |
| 3 | `DoctorUpdateRequest.java` | Có field `userId` → cho phép thay đổi user liên kết sau khi tạo → **vi phạm tính toàn vẹn dữ liệu** | Xóa `userId`. Thêm `doctorCode` (optional update) và `status` (Admin toggle ACTIVE/INACTIVE) |
| 4 | `DoctorResponse.java` | Thiếu field `status` → client không biết bác sĩ đang ACTIVE hay INACTIVE | Thêm field `status` |
| 5 | `DoctorRepository.java` | Thiếu `existsByUserId`, `existsByDoctorCode`, `existsByDoctorCodeAndIdNot`, `findByStatus`, `findBySpecialtyIdAndStatus` | Bổ sung đầy đủ |
| 6 | `DoctorScheduleRepository.java` | Thiếu `existsByDoctorId` → không kiểm tra được lịch sử trước khi xóa | Thêm `existsByDoctorId` |
| 7 | `AppointmentRepository.java` | Thiếu `existsByDoctorId` → không kiểm tra được lịch hẹn trước khi xóa | Thêm `existsByDoctorId` |
| 8 | `DoctorService.createDoctor()` | Thiếu: kiểm tra `existsByUserId`, kiểm tra role DOCTOR, kiểm tra specialty ACTIVE, kiểm tra/sinh doctorCode unique | Bổ sung đầy đủ theo đặc tả |
| 9 | `DoctorService.updateDoctor()` | Luôn update cả `user` (sai), không kiểm tra specialty ACTIVE khi đổi, không kiểm tra doctorCode unique khi đổi | Bỏ update user, thêm kiểm tra specialty ACTIVE, kiểm tra doctorCode unique với `existsByDoctorCodeAndIdNot` |
| 10 | `DoctorService.deleteDoctor()` | Hard delete vô điều kiện → **vi phạm FK constraint** nếu bác sĩ đã có lịch | Thay bằng smart delete: kiểm tra lịch sử → soft delete (INACTIVE) hoặc hard delete |
| 11 | `DoctorService.getAllDoctors()` | Không hỗ trợ lọc → trả về tất cả | Thêm lọc theo `specialtyId` và/hoặc `status` |
| 12 | `DoctorController.java` | Import thừa, method name sai (`createUser`, `deleteUser`), `getAllDoctors` không có query params | Dọn dẹp import, đổi tên method, thêm `@RequestParam` cho specialtyId & status |

### Chi tiết luồng nghiệp vụ đã implement

#### CREATE — `POST /api/admin/doctors`
```
1. Tìm User theo userId → USER_NOT_EXISTED
2. existsByUserId → DOCTOR_ALREADY_EXISTS
3. Kiểm tra role DOCTOR → USER_IS_NOT_DOCTOR
4. Tìm Specialty → SPECIALTY_NOT_EXISTED
5. Specialty.status == "ACTIVE" → SPECIALTY_INACTIVE
6. Kiểm tra/sinh doctorCode (unique)
7. Build Doctor(user, specialty, doctorCode, position, exp, desc, status="ACTIVE")
8. Save → toDoctorResponse
```

#### UPDATE — `PUT /api/admin/doctors/{doctorId}`
```
1. Tìm Doctor → DOCTOR_NOT_EXISTED
2. specialtyId thay đổi → kiểm tra Specialty tồn tại & ACTIVE
3. doctorCode thay đổi → existsByDoctorCodeAndIdNot → DOCTOR_ALREADY_EXISTS
4. updateDoctorFromRequest (mapper ignore user, doctorCode, specialty)
5. Set status nếu request truyền
6. Save → toDoctorResponse
```

#### DELETE — `DELETE /api/admin/doctors/{doctorId}`
```
1. Tìm Doctor → DOCTOR_NOT_EXISTED
2. existsByDoctorId (schedules) OR existsByDoctorId (appointments)?
   ├── CÓ lịch sử → Soft Delete: status = "INACTIVE" (bảo toàn FK)
   │   └── Nếu đã INACTIVE → DOCTOR_ALREADY_INACTIVE
   └── KHÔNG có lịch sử → Hard Delete: xóa hẳn DB
3. Trả về message mô tả hành động đã thực hiện
```

#### GET ALL — `GET /api/admin/doctors?specialtyId=X&status=ACTIVE`
```
- specialtyId && status → findBySpecialtyIdAndStatus
- chỉ specialtyId → findBySpecialtyId
- chỉ status → findByStatus
- không lọc → findAll
```

**Kết quả:**
- Đủ 4 luồng nghiệp vụ theo đặc tả, không còn lỗi runtime hay vi phạm logic.
- API hoàn chỉnh tại `/api/admin/doctors` với đầy đủ CRUD + smart delete.
- Response `DoctorResponse` phẳng hóa đầy đủ thông tin user và specialty.

### 4\. Các lưu ý RẤT QUAN TRỌNG khi làm Logic Service

#### 🔹 A. Kiểm tra tính duy nhất (Uniqueness)

* **Mã chuyên khoa (** **code** **)**: Trường `code` trong DB có ràng buộc `UNIQUE`[5].
    * Nếu client không truyền `code`, Service cần có hàm tự sinh mã (VD: từ chữ cái đầu của tên hoặc tiền tố `CK-` \+ slug).
    * Nếu client truyền `code`, Service phải dùng `specialtyRepository.existsByCode(code)` để kiểm tra trước khi lưu, tránh ném lỗi `DataIntegrityViolationException` từ database.
* **Tên chuyên khoa (** **name** **)**: Nên kiểm tra trùng lặp tên (không phân biệt hoa thường) để tránh việc Admin tạo trùng các chuyên khoa như *"Nhi khoa"* và *"nhi khoa"*.

#### 🔹 B. Xử lý Ràng buộc Khóa ngoại &amp; Logic Xóa (Soft Delete vs Hard Delete)

Do `specialties` là bảng cha được tham chiếu bởi `doctors`[5], `services`[3], và `rooms`[3]:

* **❌ KHÔNG NÊN XÓA CỨNG (Hard Delete)**: Nếu chuyên khoa đã có bác sĩ hoặc dịch vụ liên kết, việc gọi `deleteById()` sẽ làm sập ứng dụng do vi phạm ràng buộc khóa ngoại (Foreign Key Constraint) trong MySQL.
* **✅ KHUYẾN NGHỊ XÓA MỀM (Soft Delete / Deactivate)**:
    * Khi gọi API `DELETE /api/admin/specialties/{id}`, Service chỉ cập nhật `status = 'INACTIVE'` và lưu `updated_at`[5].
    * Khi bị `INACTIVE`, chuyên khoa đó sẽ ẩn khỏi ứng dụng Client (Angular) để bệnh nhân không đặt lịch mới được nữa, nhưng toàn bộ lịch sử khám chữa bệnh của các bác sĩ/dịch vụ thuộc chuyên khoa đó vẫn được bảo toàn.
* **Nếu muốn hỗ trợ Xóa cứng**: Phải kiểm tra trước trong Service:

```
boolean hasDoctors = doctorRepository.existsBySpecialtyId(id);
boolean hasServices = serviceRepository.existsBySpecialtyId(id);
boolean hasRooms = roomRepository.existsBySpecialtyId(id);

if (hasDoctors || hasServices || hasRooms) {
    throw new BadRequestException("Không thể xóa chuyên khoa đã có bác sĩ, dịch vụ hoặc phòng khám liên kết!");
}

```

#### 🔹 C. Tối ưu dữ liệu trả về cho Frontend (DTO Response)

* Trong `SpecialtyResponse`, bên cạnh các trường cơ bản, bạn có thể bổ sung thêm các trường thống kê động như: `doctorCount` (số lượng bác sĩ thuộc chuyên khoa) hoặc `serviceCount`. Điều này giúp giao diện Angular hiển thị thông tin trực quan hơn mà không cần gọi thêm nhiều API phụ.

#### 🔹 D. Quản lý Trạng thái (`status`)

* Khai báo Enum `SpecialtyStatus` (`ACTIVE`, `INACTIVE`) thay vì dùng chuỗi String tự do trong code Java[7].
* Sử dụng `@Enumerated(EnumType.STRING)` trong JPA Entity[7]
### 1\. Cấu trúc Bảng &amp; Quan hệ Nghiệp vụ

Theo thiết kế database[5]:

* **Các trường dữ liệu**: `id`, `code` (mã chuyên khoa - UNIQUE), `name` (tên chuyên khoa), `description` (mô tả), `status` (ACTIVE/INACTIVE), `created_at`, `updated_at`[5].
* **Mối quan hệ 1:N**:
    * `Specialty` ➔ `Doctor` (Một chuyên khoa có nhiều bác sĩ)[4][5].
    * `Specialty` ➔ `Service` (Một chuyên khoa có nhiều dịch vụ khám)[3][4].
    * `Specialty` ➔ `Room` (Một chuyên khoa có thể quản lý nhiều phòng khám)[3][4].

---

### 2\. Thiết kế Controller &amp; Endpoints REST API

Theo định hướng RESTful API cho Admin và Client[6]:

* **Admin APIs (** **/api/admin/specialties** **)**:
    * `GET /api/admin/specialties`: Lấy danh sách chuyên khoa (hỗ trợ phân trang `Pageable`, tìm kiếm theo `keyword` và lọc theo `status`)[6].
    * `GET /api/admin/specialties/{id}`: Lấy thông tin chi tiết một chuyên khoa[6].
    * `POST /api/admin/specialties`: Tạo mới chuyên khoa (yêu cầu validate `@Valid`)[6].
    * `PUT /api/admin/specialties/{id}`: Cập nhật thông tin chuyên khoa[6].
    * `DELETE /api/admin/specialties/{id}`: Xóa hoặc chuyển trạng thái vô hiệu hóa[6].
    * `GET /api/public/specialties`: API công khai dành cho bệnh nhân xem danh sách chuyên khoa (chỉ trả về các chuyên khoa có `status = 'ACTIVE'`).

---

## Phân tích và Hoàn thiện nghiệp vụ `SpecialtyService` & `SpecialtyController`

**Mục tiêu:** Kiểm tra và hoàn thiện logic quản lý Chuyên khoa (Specialty) theo đặc tả nghiệp vụ.

### Các lỗi / thiếu sót đã phát hiện và xử lý:

| # | File | Vấn đề | Cách xử lý |
|---|---|---|---|
| 1 | `SpecialtyMapper.java` | Map sai các field `doctor.id`, `service.id`, `room.id` (entity chứa List, và không có Room), dẫn đến lỗi compile. | Xóa các `@Mapping` sai, cấu hình `ignore` cho các collections (`doctors`, `services`). |
| 2 | `SpecialtyResponse.java` | Thiếu `status`, thừa các field Id của các relation. | Thêm `status` kiểu `EStatus`, xóa các field thừa. |
| 3 | `SpecialtyRequest.java` | Bị thừa import `EStatus`, `code` không cần bắt buộc. | Dọn dẹp import. |
| 4 | `SpecialtyUpdateRequest.java`| `status` kiểu `String` không khớp entity. | Đổi thành enum `EStatus` để mapping MapStruct chính xác. |
| 5 | `ErrorCode.java` | Thiếu mã lỗi chuyên khoa bị trùng. | Bổ sung `SPECIALTY_CODE_EXISTED`, `SPECIALTY_NAME_EXISTED`. |
| 6 | `DoctorRepository.java` | Thiếu method `existsBySpecialtyId` phục vụ xóa chuyên khoa. | Bổ sung method `existsBySpecialtyId`. |
| 7 | `SpecialtyRepository.java` | Thiếu các hàm check duplicate tên/mã, lấy list theo trạng thái. | Thêm `existsByCode`, `existsByNameIgnoreCase`, `findByStatus`,... |
| 8 | `SpecialtyService.java` | Thiếu check trùng tên/mã, tự sinh mã nếu rỗng, thiếu logic hard/soft delete theo điều kiện FK. | Viết lại toàn bộ, thêm hàm `generateCodeFromName` tự tạo mã chuyên khoa (CK-XXX). Check ràng buộc Phòng, Dịch vụ, Bác sĩ trước khi xóa. |
| 9 | `SpecialtyController.java` | Đường dẫn API bị sai (`/admin/specialties` thay vì `/api/admin/specialties`), thiếu parameter `status` để lọc danh sách. | Cập nhật lại `@RequestMapping`, bổ sung `@RequestParam(required = false) EStatus status`. |
| 10 | `PublicSpecialtyController.java`| Thiếu API public cho bệnh nhân xem chuyên khoa. | Tạo mới `PublicSpecialtyController` trả về danh sách chuyên khoa `ACTIVE`. |

**Kết quả:**
- Các ràng buộc logic như mã chuyên khoa là duy nhất, tự phát sinh mã chuyên khoa theo tên đã hoạt động.
- Tránh được lỗi vi phạm khóa ngoại bằng cơ chế Smart Delete.
- MapStruct mapping chính xác giữa Entity và DTO.
- Project compile thành công.

## Kiểm tra và sửa module Service theo nghiệp vụ — 06/10/2026

### 1. Phạm vi và quá trình thực hiện

1. Đọc phần nghiệp vụ Service ở trên; đối chiếu `ServicesService`, hai controller admin/public, entity, request/response, MapStruct mapper, repository, cấu hình security và exception handler.
2. Kiểm tra thêm `Specialty`, `Appointment`, `Payment` để xác định điều kiện chuyên khoa hoạt động, ràng buộc xóa và khả năng giữ giá lịch sử.
3. Ghi nhận workspace đã có nhiều thay đổi chưa commit; chỉ sửa những phần cần thiết cho module Service, không reset hoặc ghi đè các thay đổi khác.
4. Sửa các lỗi trong bảng dưới, bổ sung kiểm thử service, HTTP/security và truy vấn JPA trên H2 độc lập với MySQL của dự án.
5. Kiểm tra mã MapStruct sinh ra, compile và chạy bộ kiểm thử riêng cho phạm vi thay đổi. Kết quả xác minh ghi ở mục 5.

### 2. Các lỗi phát hiện và cách xử lý

| Vấn đề trước khi sửa | Xử lý đã thực hiện |
|---|---|
| Controller không có `@Valid`, các annotation DTO không được thực thi | Thêm `@Valid` cho create/update; kiểm tra tên, mã, giá, thời lượng và ID chuyên khoa |
| Admin controller ở `/admin/services`, lệch đặc tả | Chuyển sang `/api/admin/services` |
| Security chỉ yêu cầu đăng nhập, bệnh nhân có thể gọi API quản lý dịch vụ | Chỉ cho `SCOPE_ADMIN` hoặc `SCOPE_SUPER_ADMIN` truy cập đường dẫn admin của module; cho phép GET public không cần token |
| Kiểm tra trùng mã hai lần, dùng mã lỗi khác nhau, mã lưu chưa trim | Chuẩn hóa mã trước khi kiểm tra/lưu, dùng `SERVICE_CODE_EXISTED`; mã bỏ trống được sinh từ tên, xử lý trùng bằng hậu tố số, giới hạn 50 ký tự |
| Tranh chấp hai request cùng mã có thể vượt qua kiểm tra `exists` | Giữ UNIQUE ở DB; dùng `saveAndFlush` và chuyển lỗi duplicate MySQL 1062/H2 SQLState 23505 thành lỗi nghiệp vụ; các lỗi constraint khác không bị gán nhầm thành trùng mã |
| `.durationMinutes(null)` làm mất mặc định của Lombok builder | Nếu bỏ trống thì gán 30 phút; mapper create cũng có mặc định 30 |
| Create luôn `findById(specialtyId)` dù đặc tả cho phép không truyền | Cho phép null; nếu có ID thì bắt buộc chuyên khoa tồn tại và ACTIVE |
| Update có `specialtyId` nhưng mapper không cập nhật quan hệ và không kiểm tra chuyên khoa | Service truy vấn chuyên khoa ACTIVE rồi gán quan hệ; mapper bỏ qua quan hệ để tránh ghi đè |
| Mapper update ghi đè các trường không truyền bằng null, có thể làm mất dữ liệu | Dùng `NullValuePropertyMappingStrategy.IGNORE`; bỏ qua id, quan hệ và timestamps |
| Mã/tên update chỉ được trim trước khi mapper ghi đè lại giá trị gốc | Chuẩn hóa giá trị trong request trước khi mapping; request có mã/tên chỉ gồm khoảng trắng bị validation từ chối |
| Update bắt buộc giá và chuyên khoa dù đang hỗ trợ cập nhật từng trường | Các trường update là tùy chọn; nếu có giá trị thì phải hợp lệ |
| Chỉ update kiểm tra trùng tên, create lại không kiểm tra; đặc tả chỉ yêu cầu mã duy nhất | Bỏ ràng buộc trùng tên ở update; cho phép cùng tên nhưng khác mã |
| Chưa có trường cập nhật trạng thái | Thêm `status: ACTIVE/INACTIVE` vào update request; khi yêu cầu ACTIVE, kiểm tra chuyên khoa hiện tại nếu có |
| Response không map `specialty.id` và thiếu thời điểm tạo/cập nhật | Mapping `specialty.id -> specialtyId`, thêm `createdAt`, `updatedAt`; flush trước response để trả timestamp mới |
| Lấy dịch vụ không tồn tại trả lỗi chuyên khoa; HTTP status là 400 | Dùng `SERVICE_NOT_EXISTED(5003)` với HTTP 404 |
| Mã lỗi service đã INACTIVE trùng mã lỗi doctor 4006 | Đổi thành `SERVICE_ALREADY_INACTIVE(5005)` |
| Public chỉ lọc trạng thái dịch vụ, có thể trả dịch vụ của chuyên khoa tạm ngưng | Truy vấn đồng thời Service ACTIVE và Specialty ACTIVE; dịch vụ không có chuyên khoa cũng bị loại |
| Admin thiếu tìm kiếm, phân trang, lọc chuyên khoa | Bổ sung `Specification`, `Pageable`, keyword tên/mã không phân biệt hoa thường, `specialtyId`, `status`; escape `%`, `_`, `\` trong keyword |
| Validation message tiếng Việt bị handler chuyển thành “Uncategorized error” | Giữ nguyên thông báo constraint khi message không phải tên enum ErrorCode; vẫn giữ cơ chế ErrorCode hiện có |
| Appointment không lưu giá lúc đặt lịch, chỉ tham chiếu Service | Thêm `Appointment.servicePrice` ánh xạ `service_price DECIMAL(15,2)`, tự lấy giá từ Service ở `@PrePersist`, `updatable=false`; update Service không sửa Appointment/Payment |

### 3. Quy ước nghiệp vụ đã chốt khi đối chiếu tài liệu

- **Giá:** dùng `price >= 0` theo Case 3 và DTO hiện tại, cho phép dịch vụ miễn phí. Tài liệu phía trên có nhắc `@Positive` nhưng mâu thuẫn với `>= 0`; triển khai dùng `@PositiveOrZero`. Thêm `@Digits(integer=13, fraction=2)` để phù hợp `DECIMAL(15,2)`.
- **Thời lượng:** nếu truyền thì phải lớn hơn 0; nếu không truyền khi tạo thì mặc định 30 phút. Khoảng 15–120 phút trong tài liệu là ví dụ thường gặp, chưa áp thành ràng buộc bắt buộc.
- **Chuyên khoa:** tùy chọn khi tạo, dựa trên câu “Nếu truyền specialty_id” và FK hiện tại cho phép null. Dịch vụ chưa gán chuyên khoa chỉ xuất hiện ở admin; public yêu cầu một chuyên khoa ACTIVE. Trong update, null/không truyền giữ quan hệ cũ, chưa cung cấp thao tác gỡ chuyên khoa.
- **Cập nhật:** giữ cách cập nhật từng trường đang có trong dự án, dù HTTP endpoint dùng PUT. Null/không truyền giữ dữ liệu cũ; có thể xóa mô tả bằng chuỗi rỗng. Không tự sinh lại mã khi đổi tên.
- **Trạng thái:** create luôn ACTIVE. PUT có thể thay đổi trạng thái. Dịch vụ có chuyên khoa INACTIVE không được kích hoạt bằng yêu cầu `status=ACTIVE` nếu không chuyển sang chuyên khoa ACTIVE.
- **Xóa:** giữ logic đúng đã có: có bất kỳ lịch hẹn nào thì chỉ INACTIVE, kể cả lịch hẹn đã hủy; chưa có lịch hẹn thì xóa cứng. Xóa lại dịch vụ có lịch sử và đã INACTIVE vẫn trả lỗi 5005 theo hành vi hiện tại.
- **Transaction:** các truy vấn chạy read-only; create/update/delete chạy transaction ghi. Mapping response thực hiện trong transaction để truy cập quan hệ lazy an toàn.

### 4. API sau khi sửa và ảnh hưởng phía client

Context path hiện tại là `/booking-schedule`, vì vậy URL thực tế có tiền tố này.

| Method | Đường dẫn controller | Quyền và chức năng |
|---|---|---|
| POST | `/api/admin/services` | ADMIN/SUPER_ADMIN tạo dịch vụ |
| PUT | `/api/admin/services/{id}` | ADMIN/SUPER_ADMIN cập nhật từng trường, gồm status |
| DELETE | `/api/admin/services/{id}` | ADMIN/SUPER_ADMIN; xóa cứng hoặc chuyển INACTIVE theo lịch sử |
| GET | `/api/admin/services/{id}` | ADMIN/SUPER_ADMIN xem chi tiết, không thấy trả 404 |
| GET | `/api/admin/services` | ADMIN/SUPER_ADMIN; `keyword`, `specialtyId`, `status`, `page`, `size`, `sort` |
| GET | `/api/public/services` | Không cần đăng nhập; tùy chọn `specialtyId`, chỉ Service ACTIVE thuộc Specialty ACTIVE |

- Admin GET list trả `ApiResponse<Page<ServiceResponse>>`: client đọc danh sách từ `result.content`, tổng số từ `result.totalElements`; mặc định page=0, size=20, sort=id. Đây là thay đổi so với `result` trước đây là một array.
- Public vẫn trả `ApiResponse<List<ServiceResponse>>`. Tham số `status` cũ vốn bị bỏ qua đã được bỏ khỏi chữ ký API; không thể dùng tham số này để yêu cầu dịch vụ INACTIVE.
- Ví dụ GET danh sách admin: `/booking-schedule/api/admin/services?keyword=Khám&specialtyId=1&status=ACTIVE&page=0&size=20`.
- Ví dụ POST tối thiểu: `{"name":"Khám nội","price":0}`; hệ thống tự sinh code, thời lượng 30, status ACTIVE. Có thể bổ sung `specialtyId` để dịch vụ đủ điều kiện xuất hiện ở public.
- Ví dụ PUT cập nhật giá: `{"price":150000}`; ví dụ đổi chuyên khoa: `{"specialtyId":2}`; ví dụ tạm ngưng: `{"status":"INACTIVE"}`.

### 5. Xác minh

- Compile baseline và sau sửa bằng `mvn -q -DskipTests compile`: thành công.
- Bộ test mới dùng H2 test-scope; `@DataJpaTest` thay datasource bằng DB in-memory, override dialect thành H2. Không chạy ứng dụng hoặc initializer trên MySQL thật.
- Lần chạy đầu phát hiện overload `delete(any())` mơ hồ sau khi thêm `JpaSpecificationExecutor`; sửa matcher thành `any(Service.class)`. Lần tiếp theo phát hiện dialect MySQL từ cấu hình ứng dụng làm H2 không tạo được bảng; sửa riêng cấu hình test, không đổi cấu hình production.
- Lệnh kiểm thử cuối: `mvn -q -Dtest=ServicesServiceTest,ServiceControllerTest,ServiceRepositoryTest,AppointmentPriceTest test`.
- Kết quả kiểm thử cuối: đang xác minh.
- Phạm vi kiểm thử: mã tự sinh/mã trùng, trim/defaults, chuyên khoa không tồn tại/INACTIVE, mapping response, partial update, thay đổi trạng thái/giá/thời lượng/chuyên khoa, 404, soft/hard delete, validation HTTP, anonymous public, PATIENT bị từ chối admin, ADMIN/SUPER_ADMIN được phép, pagination và kết hợp bộ lọc, literal keyword có `%`, timestamps, giữ giá lịch hẹn và số tiền Payment sau khi đổi giá/xóa mềm dịch vụ.
- Không chạy `ExamineApplicationTests.contextLoads` hiện có vì test đó dùng cấu hình ứng dụng thật và có thể khởi tạo/sửa dữ liệu MySQL. Kết quả trên chỉ xác nhận phạm vi test mới, không phải toàn bộ test suite hoặc E2E trên MySQL.

### 6. Dữ liệu lịch sử và phần liên quan cần tuân thủ khi viết module tiếp theo

- Schema mới có cột nullable `appointments.service_price`. Với `ddl-auto=update` hiện tại, Hibernate sẽ thêm cột khi ứng dụng được chạy trên DB; lần kiểm tra này không chủ động sửa DB thật.
- Các lịch hẹn mới persist bằng JPA tự lưu giá niêm yết ở `@PrePersist`. Không nhận giá snapshot từ client. Hibernate không cập nhật cột này khi sửa lịch hẹn về sau.
- Lịch hẹn cũ chưa có snapshot để null; không tự điền giá hiện tại vì không thể suy ra giá tại thời điểm đặt lịch. Nếu cần bổ sung lịch sử phải dùng nguồn giá/hóa đơn đáng tin cậy.
- `Payment.amount` đã là trường độc lập, không bị update Service sửa đổi. Hiện chưa có AppointmentService/PaymentService trong dự án: khi viết những module đó, response `AppointmentResponse.servicePrice` phải đọc `Appointment.servicePrice`; tính tiền từ snapshot và áp khuyến mãi, không đọc lại `Service.price` cho lịch hẹn đã tạo. Chưa tuyên bố luồng booking/thanh toán hoàn chỉnh.
- Kiểm tra `Service ACTIVE` và `Specialty ACTIVE` phải được lặp lại ở bước tạo lịch hẹn trong module booking sau này; danh sách public được lọc không thay thế validation tại thời điểm ghi lịch hẹn.
- Tranh chấp mã được trả thành lỗi nghiệp vụ và rollback; chưa tự retry tạo mã khi có hai request đồng thời sinh cùng slug. Client có thể gửi lại yêu cầu để sinh mã kế tiếp.

Trong hệ thống quản lý đặt lịch khám bệnh, **`DoctorSchedule` (Lịch làm việc của bác sĩ)** đóng vai trò là cầu nối cốt lõi để chuyển đổi lịch trực của bác sĩ thành các khung giờ khám cụ thể cho bệnh nhân.

---

## Triển khai DoctorSchedule — 07/10/2026

### Phân tích và quyết định nghiệp vụ
- Service ban đầu chưa compile, controller chưa có, mapper chưa phẳng hóa các quan hệ. Đã bổ sung luồng tạo/cập nhật/xóa/đổi trạng thái và API đọc admin/public.
- Phòng khám tùy chọn theo đặc tả và FK nullable. `maxPatients` bỏ trống khi tạo mặc định 20; nếu truyền phải >= 1. Đây là lựa chọn theo phần mô tả mặc định, thay cho yêu cầu `@NotNull` mâu thuẫn ở phần case.
- Quy ước ban đầu mỗi slot 30 phút nhận 1 bệnh nhân **đã được thay thế** bởi phân bổ sức chứa bên dưới: tổng `TimeSlot.maxPatients` phải bằng `DoctorSchedule.maxPatients`. Ca không kéo dài qua nửa đêm. DTO không nhận status/slot tùy ý khi tạo; create luôn OPEN.
- Update từng trường: null giữ giá trị cũ, chưa cung cấp thao tác gỡ phòng. doctorId cố định. Status thay đổi bằng PATCH riêng. Có lịch hẹn bất kỳ trạng thái nào hoặc bookedCount > 0 thì không đổi ngày/giờ, không giảm maxPatients; đổi phòng/tăng trần vẫn được phép. Giữ slot ID khi chỉ đổi phòng/trần; chỉ sinh lại slot khi đổi thời gian và chưa có lịch sử.
- Khoảng ca dùng [start, end): hai ca liền nhau được phép. CLOSED/FULL vẫn giữ chỗ cho bác sĩ/phòng; CANCELLED không gây xung đột. Update bỏ qua chính ca đang sửa.
- Ghi dữ liệu trong transaction; khóa pessimistic ca hiện tại, bác sĩ và phòng trước khi kiểm tra xung đột. Việc booking sau này PHẢI khóa cùng bản ghi DoctorSchedule trước khi kiểm tra trạng thái/trần ca, ghi Appointment và bookedCount; module booking chưa có trong dự án nên chưa thể bảo đảm race với các writer bên ngoài không tuân thủ khóa.
- Xóa khi chưa có Appointment và không có bookedCount: cascade xóa slot/ca. Nếu có lịch sử kể cả lịch hẹn đã hủy/hoàn tất: giữ dữ liệu, chuyển CANCELLED. Hủy các Appointment PENDING/CONFIRMED và tạo Notification tới user bệnh nhân trong cùng transaction; không thay đổi IN_PROGRESS/COMPLETED/NO_SHOW. Gọi hủy lại không tạo thông báo trùng. CANCELLED không được mở lại.
- Notification lưu trong DB, chưa gửi email/SMS/push. Quy ước giữ bookedCount trên ca hủy đã được thay thế ở mục refactor TimeSlot ngày 08/10: giảm counter khi hủy Appointment và giữ lịch sử bằng bản ghi Appointment; hoàn tiền/đối soát Payment thuộc module booking/payment sau này, chưa xử lý ở đây.
- CLOSED/FULL/CANCELLED ngừng nhận slot rảnh; OPEN phục hồi AVAILABLE cho slot chưa đầy nếu ca còn sức chứa. Public chỉ trả OPEN, bác sĩ và chuyên khoa ACTIVE, ngày >= hôm nay, slot AVAILABLE chưa đầy và chưa bắt đầu. Không trả ca đã đạt trần tổng.
- Ngày/giờ dùng LocalDate.now()/LocalTime.now() theo timezone JVM hiện tại; triển khai server cần cấu hình timezone phù hợp phòng khám.

### API và các file liên quan
Context path `/booking-schedule` đứng trước các đường dẫn sau:

| Method | Đường dẫn | Chức năng |
|---|---|---|
| POST | `/api/admin/doctor-schedules` | Tạo ca, sinh slot |
| PUT | `/api/admin/doctor-schedules/{id}` | Cập nhật từng trường |
| DELETE | `/api/admin/doctor-schedules/{id}` | Xóa cứng hoặc hủy và bảo toàn lịch sử |
| PATCH | `/api/admin/doctor-schedules/{id}/status` | Body `{"status":"OPEN"}`; OPEN/CLOSED/FULL/CANCELLED |
| GET | `/api/admin/doctor-schedules/{id}` | Chi tiết gồm tất cả slot |
| GET | `/api/admin/doctor-schedules` | Lọc doctorId, specialtyId, fromDate, toDate, status; page/size/sort; result.content |
| GET | `/api/public/doctor-schedules` | Không cần token; lọc doctorId/specialtyId/fromDate/toDate, kèm slot có thể đặt |

- Admin yêu cầu SCOPE_ADMIN hoặc SCOPE_SUPER_ADMIN. Các lỗi module dùng code 6001–6009, HTTP 400/404/409.
- Ví dụ tạo: `{"doctorId":1,"workDate":"2026-10-08","startTime":"08:00","endTime":"11:00","maxPatients":6}`; có thể thêm roomId.
- Bổ sung repository truy vấn lịch hẹn qua slot/ca, khóa bác sĩ/phòng/ca, Specification phân trang và kiểm tra xung đột; MapStruct ánh xạ doctor/user/specialty/room và scheduleId của slot.

### Xác minh đang thực hiện
- `mvn -q -DskipTests compile`: thành công.
- Tiếp theo: kiểm thử JPA/H2 cho xung đột, sinh lại slot, lịch sử/hủy/thông báo, query/public và kiểm thử MockMvc cho validation/phân quyền. Không chạy contextLoads dùng MySQL thật.

### Kết quả xác minh cuối
- Lần đầu: 16 test pass. Sau rà soát, bổ sung kiểm thử ca CLOSED/CANCELLED, lịch hẹn CONFIRMED/COMPLETED và giờ lẻ dưới một giây.
- Sửa thứ tự khóa phòng ở update: khóa trước truy vấn lịch sử/slot, tránh tạo snapshot đọc quá sớm khi MySQL dùng REPEATABLE_READ. Chưa thực hiện stress test đồng thời trên MySQL.
- Giờ chấp nhận đến độ chính xác giây; từ chối phần nano giây để tránh làm tròn mất độ dài slot và vòng lặp sinh slot không tiến lên.
- Lệnh cuối: `mvn -q -Dtest=DoctorScheduleRepositoryTest,DoctorScheduleControllerTest test` — exit code 0.
- **20 test pass**: 15 JPA/service integration trên H2 và 5 MockMvc validation/security, không failure/error/skipped.
- Đã kiểm tra mã MapStruct sinh ra: quan hệ phẳng, enum status và scheduleId của slot ánh xạ đúng. `git diff --check` không phát hiện lỗi whitespace.
- Không chạy toàn bộ test suite hoặc ứng dụng trên MySQL; các kiểm thử chỉ xác nhận module DoctorSchedule trong phạm vi nêu trên. Không thay đổi các module nghiệp vụ khác ngoài repository/security cần dùng chung.

## Sửa đồng bộ sức chứa DoctorSchedule / TimeSlot — 07/10/2026

### Nguyên nhân và giải pháp đã triển khai
- `generateSlots` cũ chỉ sinh slot 30 phút với mặc định 1 chỗ; không sử dụng maxPatients của ca. Ví dụ 08:00–12:00, maxPatients=16 chỉ có 8 chỗ thực tế. Chỉ kiểm tra trần tổng ca không giải quyết được phần thiếu sức chứa.
- Chọn mô hình **slot là nhóm thời gian tiếp nhận**, có thể nhận nhiều bệnh nhân. Giữ nguyên biên giờ 30 phút và slot dư cuối ca; phân bổ nguyên số chỗ sao cho `sum(slot.maxPatients) == schedule.maxPatients` trên toàn bộ slot lưu trong DB/response admin. Public chỉ trả tập slot còn nhận bệnh nhân nên tổng của danh sách public có thể nhỏ hơn tổng ca.
- Khi tạo/chưa có lịch sử: N slot, M bệnh nhân; mỗi slot nhận M/N (chia nguyên), M%N slot đầu nhận thêm 1. Ví dụ 16/8 => [2,2,2,2,2,2,2,2]; 17/8 => [3,2,2,2,2,2,2,2]. Phần dư ưu tiên thứ tự giờ tăng dần.
- Khi M<N: một số slot có quota 0, status CLOSED và không xuất hiện ở public. Không ép tăng số bệnh nhân hoặc tạo slot chồng thời gian để tránh số 0.
- Khi PUT thay đổi maxPatients, cập nhật quota trên các slot hiện có, giữ ID/giờ/bookedCount. Khi đổi ngày/giờ ở ca chưa có lịch sử, sinh lại slot rồi phân bổ lại theo sức chứa mới.
- Khi ca có lịch sử: vẫn cấm giảm maxPatients của ca; thuật toán lấy max(quota hiện tại, bookedCount) làm mức tối thiểu cho mỗi slot. Phân bổ phần tăng thêm vào các slot ít chỗ nhất, không giảm quota đã cấp hay phá lịch hẹn. Dữ liệu lệch có tổng mức tối thiểu vượt maxPatients bị từ chối với SCHEDULE_CAPACITY_CONFLICT(6010), HTTP 409; không tự cắt quota đang có lịch sử.
- Thuật toán tìm mức quota chung bằng binary search rồi phân bổ phần dư theo giờ: O(N * log(M)), bộ nhớ O(N), dùng long cho tổng và phép tính để không tràn int. Không lặp M lần theo từng bệnh nhân.
- Sau phân bổ, đồng bộ AVAILABLE/FULL/CLOSED; slot có bookedCount=2 và quota tăng từ 2 lên 4 được mở AVAILABLE nếu ca OPEN. Slot quota=0 luôn CLOSED, kể cả mở lại ca.

### Dữ liệu đã tạo trước bản sửa
- Không tự sửa DB thật hay sửa dữ liệu trong GET. Ca cũ được cân lại quota trong transaction khi gọi PUT (kể cả giữ nguyên maxPatients), hoặc PATCH status=OPEN; giữ các slot và lịch sử.
- Với ca id=1 trong ví dụ, có thể gọi `PUT /booking-schedule/api/admin/doctor-schedules/1` với body `{"maxPatients":16}`. Kết quả 8 slot, mỗi slot 2 chỗ nếu chưa có phân bổ/lịch sử buộc phải giữ quota khác.
- Không dùng PATCH OPEN cho ca đã CANCELLED; quy tắc cấm mở lại ca hủy vẫn áp dụng. Nếu dữ liệu cũ đã cấp quota lớn hơn trần tổng và có lịch sử, cần tăng trần hoặc đối soát riêng, không xóa và sinh lại slot có lịch hẹn.
- Booking sau này cần khóa ca, kiểm tra OPEN và quota của slot, rồi cập nhật bookedCount trong cùng transaction. Quota nhóm thời gian không có nghĩa nhiều bệnh nhân được khám đồng thời; thời lượng dịch vụ/khả năng phục vụ bác sĩ là quy tắc cần bổ sung khi phát triển booking.

### Kiểm thử
- Bổ sung các case ví dụ 16/8, phần dư 17/8, M<N, thay đổi sức chứa giữ ID, tăng sức chứa slot đã đầy, sửa ca cũ, dữ liệu lịch sử vượt trần và Integer.MAX_VALUE.
- `mvn -q -Dtest=DoctorScheduleRepositoryTest,DoctorScheduleControllerTest test`: thành công, exit code 0; **27 test pass** (22 JPA/service H2, 5 MockMvc), không failure/error/skipped. Build test đã compile mã sau sửa. Không chạy MySQL thật hoặc toàn bộ suite.

## Kiểm tra doctorId khi cập nhật DoctorSchedule — 07/10/2026

- Nguyên nhân: DoctorScheduleUpdateRequest chưa khai báo doctorId, nên trường JSON này bị Jackson bỏ qua; API vẫn cập nhật room/ngày/giờ/sức chứa theo id của ca. Trước sửa không đổi bác sĩ của ca nhưng cũng không báo client đã gửi sai doctorId.
- Thêm doctorId tùy chọn vào DTO update với @Positive. Đây là giá trị đối chiếu, không phải yêu cầu chuyển ca sang bác sĩ khác.
- Service khóa/lấy ca theo id, so sánh doctorId trong request với schedule.doctor.id trước khi khóa bác sĩ/phòng, kiểm tra lịch hẹn hoặc thay đổi bất kỳ thuộc tính/slot nào. Không khớp => SCHEDULE_DOCTOR_MISMATCH(6011), HTTP 409, kể cả ID bác sĩ truyền vào không tồn tại. Không tìm thấy ca vẫn trả SCHEDULE_NOT_FOUND(6004), HTTP 404.
- Trùng ID => cập nhật các trường được phép, không đổi quan hệ doctor. Không truyền/null => tiếp tục hỗ trợ cập nhật từng trường như trước. ID <= 0 => validation HTTP 400. API vẫn chỉ dành cho ADMIN/SUPER_ADMIN; việc đối chiếu không thay thế phân quyền.
- Ví dụ ca id=1 thuộc doctorId=2: PUT `/booking-schedule/api/admin/doctor-schedules/1` với `{"doctorId":3,"maxPatients":20}` trả 409/code 6011; `{"doctorId":2,"maxPatients":20}` được xử lý bình thường.
- Bổ sung kiểm thử bác sĩ khác có tồn tại/không tồn tại bị từ chối; flush và reload xác nhận toàn bộ response ca/slot không thay đổi; doctorId trùng được phép cập nhật; HTTP nhận đúng doctorId/trả 409 và từ chối 0/-1. Các test update bỏ doctorId đã có tiếp tục được chạy.
- `mvn -q -Dtest=DoctorScheduleRepositoryTest,DoctorScheduleControllerTest test`: exit code 0, **31 test pass** (24 JPA/service H2 và 7 MockMvc), không failure/error/skipped. `git diff --check` không có lỗi whitespace; không chạy ứng dụng/MySQL thật.

## Phân tích và refactor TimeSlot — 08/10/2026

### Đối chiếu nghiệp vụ và lỗi phát hiện
- TimeSlotService chỉ lấy toàn bộ slot, không lọc trạng thái/sức chứa/ngày; danh sách rỗng bị nhầm thành ca không tồn tại.
- TimeSlotMapper thiếu doctorSchedule.id -> scheduleId. Public controller có URL lặp time-slots và security chưa cho anonymous GET.
- Chưa có thao tác khóa/mở slot riêng lẻ, chưa có khóa hàng/cập nhật counter; repository so bookedCount với tham số chung thay vì quota của từng slot.
- Status entity đang là String thay vì enum. Logic cascade cũ vẫn để slot FULL khi ca CLOSED/CANCELLED và có thể mở lại slot admin khóa khi cập nhật ca.

### Quy tắc triển khai
- Giữ tự sinh slot từ DoctorSchedule, không thêm POST/PUT/DELETE độc lập cho TimeSlot. Giữ tổng quota bằng maxPatients ca theo quyết định ngày 07/10; mặc định 1 của entity không thay thế phân bổ quota. TimeSlotRequest cũ không có API sử dụng.
- ETimeSlotStatus lưu EnumType.STRING: AVAILABLE/FULL/CLOSED/UNAVAILABLE; admin chỉ yêu cầu AVAILABLE hoặc CLOSED/UNAVAILABLE (chuẩn hóa thành CLOSED). FULL do counter xác định, không cho ép bằng admin.
- Thêm manually_closed (boolean default false) để lưu khóa riêng, không mất khi mở lại/cập nhật ca. Hibernate ddl-auto=update sẽ thêm cột khi chạy ứng dụng; lần này không tác động MySQL. Các dòng CLOSED cũ không thể suy ra đóng riêng hay theo ca: không tự nhận là khóa riêng. Cần đối soát và khóa lại qua API nếu có dữ liệu khóa thủ công từ nguồn khác.
- TimeSlotPolicy dùng chung cho hai module: đóng theo ca ưu tiên hơn FULL; quota=0/khóa riêng thì CLOSED; OPEN và đạt quota thì FULL; slot còn chỗ nhưng ca đạt trần thì CLOSED; các trường hợp còn lại AVAILABLE. Không tự mở ca FULL/CLOSED/CANCELLED khi nhả chỗ.
- Public kiểm tra ca OPEN, doctor/specialty ACTIVE, thời điểm slot chưa bắt đầu, không khóa riêng, status AVAILABLE, bookedCount < quota và tổng bookedCount < maxPatients ca. Ca tồn tại nhưng không có slot phù hợp => []; ca không tồn tại => 404.
- Khóa riêng bị từ chối nếu bookedCount>0 hoặc có Appointment PENDING/CONFIRMED/IN_PROGRESS, kể cả counter cũ sai bằng 0. Mở riêng bị từ chối nếu ca không nhận lịch, slot hết chỗ/quota=0/đã bắt đầu hoặc tổng ca hết chỗ.
- reserveSlot/releaseSlot chỉ là thao tác NỘI BỘ, không có HTTP endpoint; Propagation.MANDATORY yêu cầu transaction gọi từ module Appointment. Khóa ca trước, sau đó toàn bộ slot theo ID bằng PESSIMISTIC_WRITE để kiểm tra cả trần ca và quota slot; reserve tăng counter và tự chuyển FULL; release không được âm và phục hồi trạng thái theo ca/khóa riêng.
- Module Appointment chưa có service nên chưa có luồng đặt/hủy hoàn chỉnh. Service Appointment sau này phải gọi reserve đúng một lần trước khi persist Appointment, release đúng một lần khi đổi trạng thái sang CANCELLED; kiểm tra trạng thái cũ để hủy idempotent và cùng transaction. Không gọi release cho Appointment đã hủy. Không cung cấp API tăng/giảm counter tùy ý.
- DoctorSchedule.cancel hiện giảm counter cho các Appointment PENDING/CONFIRMED vừa hủy, không giảm dưới 0 với dữ liệu cũ thiếu counter; lịch sử được giữ bằng Appointment thay cho counter. Quy tắc này thay thế ghi chú cũ giữ counter trên ca hủy. IN_PROGRESS/COMPLETED/NO_SHOW không bị hủy; hoàn tiền chưa thuộc phạm vi này.
- Xóa ca vẫn yêu cầu không có counter và không có bất kỳ Appointment lịch sử nào; chặt hơn điều kiện counter=0 trong tài liệu để bảo vệ FK/lịch sử, kể cả lịch hẹn đã hủy.

### API
Context path `/booking-schedule` đứng trước đường dẫn:
- GET `/api/public/time-slots?scheduleId=1` hoặc `/api/public/doctor-schedules/1/time-slots`: không cần token, chỉ slot đặt được.
- GET `/api/public/time-slots/1/time-slots`: giữ alias URL cũ để client không bị gián đoạn, cùng bộ lọc.
- PATCH `/api/admin/time-slots/{id}/status`, body `{"status":"CLOSED"}` hoặc `{"status":"AVAILABLE"}`: ADMIN/SUPER_ADMIN/STAFF theo yêu cầu lễ tân trong tài liệu.
- Lỗi module 7001–7005: slot không tồn tại (404), có booking/không khả dụng/counter không thể giảm (409), yêu cầu FULL (400).

### Xác minh
- Compile `mvn -q -DskipTests compile`: thành công.
- Lần đầu kiểm thử TimeSlot và DoctorSchedule: 49 test pass; sau đó bổ sung case tranh chỗ ở hai slot khác nhau cùng ca, ca rỗng và slot khóa thủ công.
- Lệnh cuối: `mvn -q -Dtest=TimeSlotServiceTest,TimeSlotConcurrencyTest,TimeSlotControllerTest,DoctorScheduleRepositoryTest,DoctorScheduleControllerTest,ServiceRepositoryTest,AppointmentPriceTest test` — exit code 0, **58 test pass**, không failure/error/skipped.
- Bao gồm 13 test service/JPA TimeSlot, 3 test concurrency/rollback với transaction thật trên H2, 5 test HTTP/security TimeSlot, 31 test DoctorSchedule và 6 test hồi quy service/giá lịch hẹn.
- Hai request đồng thời tranh một slot chỉ một request thành công; hai request đặt hai slot khác nhau với trần ca=1 cũng chỉ một request thành công. Counter rollback khi giao dịch giả lập Appointment thất bại. Các kết quả này xác nhận trên H2, chưa stress-test trên MySQL thật.
- Kiểm tra mã MapStruct sinh ra: scheduleId và enum -> String đúng. `git diff --check` không phát hiện lỗi whitespace. Không chạy ứng dụng hoặc test contextLoads dùng MySQL thật.
