# Tiến độ tích hợp VNPay

## Phạm vi

Tích hợp luồng thanh toán phí khám toàn phần qua VNPay Sandbox cho backend Spring Boot. Callback IPN là nguồn cập nhật trạng thái thanh toán chính; Return URL chỉ xác minh phản hồi và đưa người dùng về trang kết quả.

## Đã thực hiện

- Thêm cấu hình `payment.vnpay` trong `application.yaml`, đọc thông tin nhạy cảm từ biến môi trường.
- Đưa cấu hình Sandbox vào `src/main/resources/application-local.yaml` bằng placeholder biến môi trường. File local này đã tồn tại và đang được Git theo dõi; nó chỉ chứa placeholder, không chứa khóa thật.
- Bổ sung entity `PaymentTransaction` và repository để lưu từng lần thử thanh toán, mã giao dịch VNPay, số tiền, trạng thái, thời hạn và kết quả trả về.
- Bổ sung `VnpayService`: tạo URL thanh toán, ký HMAC-SHA512, xác minh chữ ký IPN/Return, đối chiếu số tiền và xử lý IPN lặp.
- Bổ sung endpoint tạo URL thanh toán bệnh nhân: `POST /api/patient/appointments/{id}/payments/vnpay`.
- Bổ sung callback công khai: `GET /api/payments/vnpay/ipn` và `GET /api/payments/vnpay/return`.
- Bổ sung API bệnh nhân đọc trạng thái: `GET /api/patient/payments/vnpay/{txnRef}`, có kiểm tra quyền sở hữu lịch.
- Mở quyền truy cập không cần JWT cho IPN và Return; API tạo thanh toán vẫn yêu cầu bệnh nhân đăng nhập và là chủ lịch.
- Mỗi `vnp_TxnRef` được sinh bằng số để tương thích định dạng mã tham chiếu VNPay, mỗi lần thử có bản ghi riêng.

## Cấu hình môi trường

Các biến tương ứng trong `application.yaml` là `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_URL`, `VNPAY_RETURN_URL` và `VNPAY_FRONTEND_RESULT_URL`. Đặt `VNPAY_TMN_CODE` và `VNPAY_HASH_SECRET` trong môi trường chạy local/IDE theo thông tin merchant đã nhận; URL Sandbox đã có mặc định. Không dùng thông tin đăng nhập Merchant Admin trong ứng dụng.

IPN Sandbox phải được cấu hình thành URL HTTPS công khai có thể truy cập từ VNPAY. Địa chỉ localhost trong cấu hình local chỉ dùng cho phát triển; cần dùng tunnel HTTPS hoặc backend đã triển khai và khai báo URL đó trong Merchant Admin/SIT.

## Còn cần hoàn tất khi tích hợp vào môi trường chạy

- Cập nhật `VNPAY_RETURN_URL` và `VNPAY_FRONTEND_RESULT_URL` theo URL frontend/backend thực tế.
- Khai báo IPN URL HTTPS công khai trong Merchant Admin và chạy kịch bản SIT.
- Kiểm tra thành công, thất bại, người dùng hủy, sai chữ ký/số tiền, IPN lặp và IPN tới muộn.
- Frontend có thể dùng API trạng thái giao dịch để truy vấn sau Return nếu cần polling.
- Xác nhận chính sách hoàn tiền khi lịch bị hủy sau khi tiền đã được ghi nhận.

## Ghi chú an toàn

Thông tin đăng nhập Merchant Admin/SIT không được đưa vào ứng dụng. Khóa bí mật chỉ được nạp từ biến môi trường; không ghi giá trị khóa vào file đang được theo dõi hoặc log.
