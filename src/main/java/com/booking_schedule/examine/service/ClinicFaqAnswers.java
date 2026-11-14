package com.booking_schedule.examine.service;

import java.util.*;

/** Deterministic help only: no personal data lookup or business mutations. */
final class ClinicFaqAnswers {
    private ClinicFaqAnswers() {}
    record Entry(String action, String answer, List<String> questions) {}

    static final List<Entry> ENTRIES = List.of(
        entry("BOOKING_HELP", "Bạn hãy đăng nhập, kiểm tra hồ sơ bệnh nhân rồi mở chức năng đặt lịch. Chọn dịch vụ/chuyên khoa, bác sĩ và khung giờ còn chỗ, kiểm tra thông tin và gửi yêu cầu. Sau đó xem trạng thái trong lịch hẹn của mình. Mình chưa tạo lịch trực tiếp trong chat.",
            "Tôi muốn đặt lịch", "Đặt lịch giúp tôi", "Đặt lịch như thế nào", "Làm sao đặt lịch khám", "Hướng dẫn đặt lịch khám", "Cần đăng nhập để đặt lịch không"),
        entry("CHAT_HELP", "Mình có thể giúp bạn xem chuyên khoa, giá và thời lượng dịch vụ, hướng dẫn đặt/hủy lịch và thanh toán. Bạn muốn tìm thông tin nào?",
            "Bạn giúp được gì", "Bạn có thể làm gì", "Bạn hỗ trợ gì", "Tôi cần hỗ trợ", "Hướng dẫn sử dụng chatbot"),
        entry("APPOINTMENT_HELP", "Bạn hãy đăng nhập và mở danh sách hoặc lịch sử lịch hẹn, chọn lịch cần xem để kiểm tra ngày, giờ và trạng thái hiện tại. Mình chưa tra cứu lịch hẹn cá nhân trực tiếp trong chat.",
            "Xem lịch hẹn ở đâu", "Tôi muốn xem lịch hẹn", "Kiểm tra lịch hẹn của tôi", "Lịch hẹn của tôi ở đâu", "Xem lịch sử khám ở đâu", "Làm sao biết đã đặt lịch thành công"),
        entry("CANCEL_HELP", "Bạn mở lịch hẹn của mình, chọn lịch cần hủy và sử dụng chức năng hủy nếu hệ thống cho phép. Việc hủy phụ thuộc trạng thái lịch và thời hạn đang cấu hình. Nếu đã thanh toán hoặc không thể hủy, hãy liên hệ phòng khám để được hỗ trợ. Mình chưa thực hiện hủy lịch qua chat.",
            "Cách hủy lịch", "Hủy lịch như thế nào", "Tôi muốn hủy lịch", "Hủy lịch giúp tôi", "Có hủy lịch được không", "Tại sao không hủy được lịch", "Đặt lịch rồi có hủy được không"),
        entry("RESCHEDULE_HELP", "Hiện chat chưa hỗ trợ đổi lịch. Bạn hãy liên hệ phòng khám để được hướng dẫn; nếu được phép hủy và đặt lại trên hệ thống, hãy kiểm tra lịch trống và tình trạng thanh toán trước khi thao tác. Không nên tự đặt thêm lịch trùng.",
            "Tôi muốn đổi lịch", "Cách đổi lịch", "Đổi giờ khám được không", "Tôi muốn dời lịch khám", "Có đổi ngày khám được không"),
        entry("PAYMENT_HELP", "Bạn mở chi tiết lịch hẹn và chọn phương thức thanh toán đang được hiển thị. Hệ thống có luồng VNPay, VietQR và payOS nhưng việc sử dụng phụ thuộc cấu hình triển khai. Kiểm tra số tiền, mã lịch và trạng thái trước khi trả tiền; không gửi mật khẩu ngân hàng hoặc OTP vào chat.",
            "Thanh toán như thế nào", "Cách thanh toán", "Thanh toán ở đâu", "Có những phương thức thanh toán nào", "Có chuyển khoản được không", "Có thanh toán online không"),
        entry("VNPAY_HELP", "Nếu màn hình lịch hẹn có lựa chọn VNPay, hãy tạo liên kết thanh toán và làm theo hướng dẫn trên trang VNPay. Sau khi quay lại ứng dụng, kiểm tra trạng thái giao dịch. Việc quay lại trang kết quả chưa đồng nghĩa đã thanh toán thành công; cần chờ backend nhận xác nhận.",
            "Cách thanh toán VNPay", "Thanh toán bằng VNPay như thế nào", "Hướng dẫn VNPay", "Thanh toán VNPay", "VNPay là gì"),
        entry("PAYOS_HELP", "Nếu payOS đang được bật, bạn mở lịch hẹn hoặc khoản cần thu, chọn thanh toán qua payOS rồi quét QR hoặc mở trang thanh toán. Kiểm tra người nhận và số tiền trước khi xác nhận. Sau đó kiểm tra trạng thái trong ứng dụng; trang chuyển hướng không tự xác nhận đã nhận tiền.",
            "Cách thanh toán payOS", "Thanh toán bằng payOS như thế nào", "Hướng dẫn payOS", "Thanh toán payOS"),
        entry("VIETQR_HELP", "Nếu VietQR đang được bật, hãy tạo mã QR tại lịch hẹn, dùng ứng dụng ngân hàng quét mã và kiểm tra người nhận, số tiền, nội dung trước khi xác nhận. Giữ nguyên nội dung chuyển khoản và kiểm tra trạng thái sau khi trả tiền. Chat không tự tạo QR hay xác nhận đã nhận tiền.",
            "Cách thanh toán VietQR", "Thanh toán bằng VietQR như thế nào", "Hướng dẫn VietQR", "Thanh toán VietQR", "Quét QR thanh toán như thế nào", "Tạo mã QR ở đâu"),
        entry("PAYMENT_PENDING_HELP", "Bạn hãy kiểm tra lịch sử giao dịch trong ứng dụng ngân hàng và trạng thái thanh toán của lịch hẹn. Nếu đã bị trừ tiền, đừng thanh toán lại; lưu mã giao dịch, thời gian và số tiền để liên hệ phòng khám đối soát. Mình chưa kiểm tra giao dịch cá nhân trong chat.",
            "Đã chuyển tiền nhưng chưa xác nhận", "Tôi đã chuyển tiền nhưng chưa được xác nhận", "Thanh toán rồi vẫn chờ xử lý", "Đã trừ tiền nhưng chưa thành công", "Tại sao thanh toán vẫn pending", "Tôi đã thanh toán rồi", "Kiểm tra thanh toán của tôi", "Tiền đã trừ nhưng lịch chưa cập nhật"),
        entry("PAYMENT_FAILED_HELP", "Bạn hãy kiểm tra xem ngân hàng đã trừ tiền chưa. Nếu chưa trừ, kiểm tra kết nối và tạo lại liên kết thanh toán từ lịch hẹn nếu hệ thống cho phép. Nếu đã trừ tiền hoặc kết quả không rõ, đừng trả lần nữa; liên hệ phòng khám để đối soát.",
            "Thanh toán thất bại", "VNPay báo lỗi", "Liên kết thanh toán hết hạn", "Link thanh toán hết hạn", "Không thanh toán được", "Thanh toán bị hủy"),
        entry("PAYMENT_MISMATCH_HELP", "Bạn đừng chuyển bổ sung hoặc thanh toán lại khi chưa được hướng dẫn. Hãy lưu biên lai, mã giao dịch, số tiền và nội dung đã chuyển, rồi liên hệ phòng khám để đối soát. Hệ thống không tự xác nhận khi số tiền hoặc thông tin chuyển khoản không khớp.",
            "Tôi chuyển sai số tiền", "Chuyển khoản sai nội dung", "Tôi chuyển thiếu tiền", "Tôi chuyển thừa tiền", "Tôi thanh toán hai lần", "Tôi chuyển tiền hai lần", "Chuyển nhầm tài khoản"),
        entry("REFUND_HELP", "Bạn hãy liên hệ phòng khám, cung cấp mã lịch hẹn và mã giao dịch để được kiểm tra điều kiện hoàn tiền. Hệ thống hiện chưa có hoàn tiền tự động; việc hủy lịch không đồng nghĩa tiền đã được hoàn. Mình không thể xác nhận thời hạn hoặc số tiền hoàn qua chat.",
            "Tôi muốn hoàn tiền", "Hoàn tiền như thế nào", "Hủy lịch có được hoàn tiền không", "Bao lâu được hoàn tiền", "Tôi chưa nhận được tiền hoàn", "Cách hoàn tiền"),
        entry("PROFILE_HELP", "Bạn hãy đăng nhập và mở hồ sơ bệnh nhân của mình để kiểm tra hoặc cập nhật thông tin trước khi đặt lịch. Nếu hệ thống yêu cầu hồ sơ mà bạn chưa có, hãy tạo hồ sơ bằng chức năng tương ứng. Không gửi mật khẩu, OTP hoặc giấy tờ nhạy cảm vào chat.",
            "Tạo hồ sơ bệnh nhân ở đâu", "Cách cập nhật hồ sơ", "Tôi chưa có hồ sơ bệnh nhân", "Đổi thông tin bệnh nhân", "Không đặt được lịch vì thiếu hồ sơ"),
        entry("VISIT_PREPARATION_HELP", "Bạn nên chuẩn bị mã lịch hẹn, giấy tờ cần thiết và các kết quả khám trước nếu có. Với yêu cầu riêng như nhịn ăn hoặc chuẩn bị trước xét nghiệm, hãy hỏi nhân viên y tế của phòng khám; không tự áp dụng chỉ dẫn chưa được xác nhận.",
            "Đi khám cần mang gì", "Tôi cần chuẩn bị gì trước khi đi khám", "Lần đầu đi khám cần chuẩn bị gì", "Đi khám cần giấy tờ gì"),
        entry("CONTACT_HELP", "Mình chưa có thông tin liên hệ, địa chỉ hoặc giờ mở cửa đã xác thực trong dữ liệu chat. Bạn hãy xem thông tin liên hệ trên ứng dụng hoặc liên hệ trực tiếp phòng khám; mình không tự đưa số điện thoại hay giờ làm việc.",
            "Số điện thoại phòng khám", "Liên hệ phòng khám như thế nào", "Địa chỉ phòng khám", "Phòng khám ở đâu", "Phòng khám mở cửa mấy giờ", "Chủ nhật có khám không")
    );
    private static Entry entry(String action, String answer, String... questions) {
        return new Entry(action, answer, List.of(questions));
    }
    private static final Map<String,String> BY_QUESTION;
    private static final Map<String,String> BY_ACTION;
    static {
        Map<String,String> questions = new HashMap<>(), actions = new HashMap<>();
        for (Entry entry : ENTRIES) {
            actions.put(entry.action(), entry.answer());
            for (String question : entry.questions()) {
                if (questions.put(key(question), entry.answer()) != null)
                    throw new IllegalStateException("Duplicate FAQ question");
            }
        }
        BY_QUESTION = Map.copyOf(questions);
        BY_ACTION = Map.copyOf(actions);
    }
    static Optional<String> answer(String text) { return Optional.ofNullable(BY_QUESTION.get(key(text))); }
    static Optional<String> action(String action) { return Optional.ofNullable(BY_ACTION.get(action)); }
    private static String key(String text) {
        return ClinicDirectAnswers.normalize(text.replaceAll("[,;:!?.]+", " ")).replaceFirst("^(cho toi hoi|cho minh hoi|ban oi|lam on) +", "")
                .replaceFirst(" +(a|nhe|voi|duoc khong)$", "");
    }
}
