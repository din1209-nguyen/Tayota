# Luồng nghiệp vụ đặt lịch Tayota

Tài liệu này mô tả nghiệp vụ đặt lịch lái thử, đặt lịch bảo dưỡng/sửa chữa và các chức năng liên quan cho Customer, Kỹ thuật viên và Cố vấn dịch vụ.

## 1. Phạm vi nghiệp vụ

Hệ thống hỗ trợ 2 loại appointment chính:

- `TEST_DRIVE`: lịch lái thử xe.
- `SERVICE`: lịch bảo dưỡng/sửa chữa xe.

Appointment là yêu cầu đặt lịch ban đầu. Với lịch bảo dưỡng/sửa chữa, khi khách tới đại lý và được check-in, hệ thống tạo thêm service ticket để theo dõi quá trình tiếp nhận, sửa chữa, hạng mục dịch vụ, chi phí và hoàn tất.

## 2. Vai trò trong luồng

### Customer

- Đặt lịch lái thử xe.
- Đặt lịch bảo dưỡng/sửa chữa.
- Nếu đã đăng nhập, hệ thống dùng thông tin tài khoản, không bắt nhập lại họ tên/email/số điện thoại.
- Nếu là khách vãng lai, phải nhập thông tin guest gồm họ tên, email, số điện thoại.
- Xem lịch sử đặt lịch, lịch sử dịch vụ và trạng thái hiện tại.
- Xem hóa đơn dịch vụ nếu đã phát sinh.
- Đánh giá dịch vụ sau khi service hoàn tất.

### Cố vấn dịch vụ / Manager đại lý

- Xem danh sách lịch đang chờ duyệt.
- Kiểm tra thông tin đặt lịch, gọi khách xác nhận.
- Duyệt, từ chối hoặc điều chỉnh ngày giờ nếu cần.
- Check-in khách khi khách tới đại lý.
- Với lịch bảo dưỡng/sửa chữa, tạo service ticket và gán kỹ thuật viên.
- Quản lý khung giờ làm việc và ngày nghỉ của đại lý.
- Xem lịch sử lịch đã duyệt.
- Tạo hợp đồng mua xe và gán xe cho tài khoản khách hàng.
- Xem báo cáo liên quan đến đặt lịch, dịch vụ, hợp đồng mua xe và kỹ thuật viên thuộc đại lý của mình.

### Kỹ thuật viên

- Xem danh sách service ticket chờ tiếp nhận.
- Tiếp nhận xe/dịch vụ được giao.
- Cập nhật trạng thái service.
- Ghi nhận tình trạng xe.
- Thêm hạng mục sửa chữa/bảo dưỡng.
- Xem lịch sử dịch vụ đã phục vụ, kèm đánh giá của khách hàng nếu có.

## 3. Quy tắc đặt lịch chung

Áp dụng cho cả lịch lái thử và lịch bảo dưỡng/sửa chữa:

- Chỉ được đặt lịch sớm nhất từ 12 tiếng tiếp theo tính từ thời điểm hiện tại.
- Ngày xa nhất có thể đặt là 4 tháng tiếp theo.
- Không cho đặt vào ngày nghỉ của đại lý.
- Không cho đặt vào khung giờ không thuộc lịch làm việc của đại lý.
- Trước giờ hẹn 2 tiếng, hệ thống gửi email và thông báo nhắc lịch cho khách.
- Khi khách đặt lịch thành công, appointment ban đầu có trạng thái `PENDING`.
- Cố vấn dịch vụ/manager phải xác nhận trước khi chuyển sang `CONFIRMED`.

## 4. Trạng thái Appointment

| Trạng thái   | Ý nghĩa                                               |
| ------------ | ----------------------------------------------------- |
| `PENDING`    | Khách vừa tạo lịch, đang chờ đại lý kiểm tra/xác nhận |
| `CONFIRMED`  | Đại lý đã xác nhận lịch với khách                     |
| `CHECKED_IN` | Khách đã tới đại lý và được check-in                  |
| `COMPLETED`  | Lịch đã hoàn tất                                      |
| `CANCELED`   | Khách hoặc đại lý đã hủy lịch trước giờ hẹn           |
| `EXPIRED`    | Quá giờ/ngày hẹn nhưng khách không đến check-in       |
| `REJECTED`   | Đại lý từ chối yêu cầu đặt lịch vì không phù hợp      |

## 5. Trạng thái Service Ticket

Áp dụng cho lịch bảo dưỡng/sửa chữa sau khi khách đã check-in.

| Trạng thái   | Ý nghĩa                                                                |
| ------------ | ---------------------------------------------------------------------- |
| `CONFIRMED`  | Service ticket vừa được tạo sau khi khách check-in                     |
| `RECEIVING`  | Kỹ thuật viên đã tiếp nhận xe                                          |
| `PROCESSING` | Kỹ thuật viên đang sửa chữa/bảo dưỡng                                  |
| `COMPLETED`  | Service đã hoàn tất                                                    |
| `EXPIRED`    | Lịch/service hết hạn do khách không đến hoặc không được xử lý đúng hạn |
| `CANCELED`   | Service bị hủy nếu nghiệp vụ cho phép                                  |

Ghi chú: Trong code có thể enum đang đặt tên là `SERVICE_PROCESSING` hoặc `PROCESSING`. Khi triển khai cần dùng đúng enum hiện có hoặc chuẩn hóa lại toàn hệ thống.

## 6. Luồng đặt lịch bảo dưỡng / sửa chữa

### Bước 1. Customer tạo appointment

Customer nhập thông tin đặt lịch bảo dưỡng/sửa chữa:

- VIN xe.
- Thông tin khách/guest: họ tên, email, số điện thoại nếu chưa đăng nhập.
- Đại lý muốn tới.
- Ngày hẹn.
- Khung giờ hẹn.
- Mô tả chi tiết tình trạng xe hoặc nhu cầu bảo dưỡng/sửa chữa.

Hệ thống xử lý:

1. Kiểm tra VIN có tồn tại trong hệ thống không.
2. Nếu user đã đăng nhập, lấy thông tin khách từ tài khoản, không bắt nhập lại.
3. Nếu guest, validate đầy đủ họ tên, email, số điện thoại.
4. Kiểm tra đại lý tồn tại và đang hoạt động.
5. Kiểm tra ngày/giờ thỏa quy tắc đặt lịch: tối thiểu 12 tiếng tiếp theo, tối đa 4 tháng, không trùng ngày nghỉ, khung giờ còn trống.
6. Tạo appointment loại `SERVICE` với trạng thái `PENDING`.
7. Gửi email/thông báo xác nhận đã nhận yêu cầu đặt lịch nếu cần.

Kết quả:

- `APPOINTMENT.status = PENDING`
- Chưa tạo service ticket ở bước này.

### Bước 2. Manager/Cố vấn duyệt appointment

Cố vấn dịch vụ hoặc manager đại lý:

1. Xem danh sách appointment `PENDING`.
2. Kiểm tra VIN, thông tin khách, đại lý, ngày giờ và mô tả tình trạng xe.
3. Gọi cho khách để xác nhận nhu cầu.
4. Có thể điều chỉnh ngày/giờ nếu cần và nếu khung giờ mới còn hợp lệ.
5. Chuyển appointment sang `CONFIRMED` nếu hợp lệ.
6. Gửi email/thông báo xác nhận lịch cho khách.

Trường hợp không hợp lệ:

- Nếu khách không xác nhận hoặc thông tin không phù hợp, chuyển appointment sang `REJECTED` hoặc `CANCELED` tùy nghiệp vụ.
- Gửi thông báo lý do từ chối/hủy nếu cần.

Kết quả khi duyệt:

- `APPOINTMENT.status = CONFIRMED`

### Bước 3. Khách tới đại lý và check-in

Khi khách tới đại lý:

1. Cố vấn dịch vụ xác nhận khách đã đến.
2. Cập nhật appointment sang `CHECKED_IN`.
3. Tạo service ticket cho lịch bảo dưỡng/sửa chữa.
4. Ghi nhận mô tả/tình trạng xe ban đầu.
5. Gán kỹ thuật viên phụ trách.
6. Service ticket ban đầu có trạng thái `CONFIRMED`.

Kết quả:

- `APPOINTMENT.status = CHECKED_IN`
- `SERVICE_TICKET.status = CONFIRMED`
- Service ticket liên kết với appointment, khách, xe/VIN, đại lý và kỹ thuật viên.

### Bước 4. Kỹ thuật viên tiếp nhận và bắt đầu sửa

Kỹ thuật viên thực hiện:

1. Xem danh sách service ticket được giao hoặc đang chờ tiếp nhận.
2. Bấm tiếp nhận xe.
3. Hệ thống chuyển service ticket sang `RECEIVING`.
4. Kỹ thuật viên kiểm tra xe và ghi nhận tình trạng thực tế.
5. Khi bắt đầu sửa/bảo dưỡng, chuyển service ticket sang `PROCESSING`.

Kết quả:

- `SERVICE_TICKET.status = RECEIVING` khi tiếp nhận.
- `SERVICE_TICKET.status = PROCESSING` khi bắt đầu sửa.

### Bước 5. Trong lúc sửa chữa/bảo dưỡng

Kỹ thuật viên thêm các hạng mục dịch vụ:

- Công kiểm tra.
- Công sửa chữa.
- Phụ tùng thay thế.
- Vật tư/phụ kiện.
- Ghi chú kỹ thuật.

Hệ thống xử lý:

1. Kiểm tra service ticket đang ở trạng thái cho phép thêm hạng mục.
2. Lấy giá phụ tùng hoặc công sửa từ dữ liệu hệ thống nếu có.
3. Cho phép nhập giá thủ công nếu nghiệp vụ cần và role được phép.
4. Tính `total_amount` từ danh sách service item.
5. Cập nhật tổng tiền trên service ticket/hóa đơn tạm.

Kết quả:

- Service ticket có danh sách `SERVICE_ITEM`.
- Tổng tiền dịch vụ được cập nhật.

### Bước 6. Sửa xong, xác nhận và thanh toán

Khi sửa xong:

1. Kỹ thuật viên/cố vấn thông báo cho khách.
2. Khách kiểm tra và xác nhận.
3. Hệ thống xuất hóa đơn hoặc ghi nhận thanh toán theo nghiệp vụ thanh toán.
4. Kỹ thuật viên bấm hoàn thành.
5. Service ticket chuyển sang `COMPLETED`.
6. Appointment liên quan tự chuyển sang `COMPLETED`.
7. Gửi email cảm ơn và khảo sát đánh giá.

Kết quả:

- `SERVICE_TICKET.status = COMPLETED`
- `APPOINTMENT.status = COMPLETED`
- Có thể phát sinh hóa đơn dịch vụ.
- Khách được phép đánh giá dịch vụ.

### Bước 7. Đánh giá dịch vụ

Điều kiện:

- Chỉ cho đánh giá nếu `SERVICE_TICKET.status = COMPLETED`.
- Một service ticket chỉ nên có một đánh giá hợp lệ từ khách hàng, trừ khi nghiệp vụ cho phép cập nhật.

Hệ thống:

1. Gửi email cảm ơn.
2. Gửi link khảo sát/đánh giá.
3. Khách nhập điểm đánh giá và nội dung nhận xét.
4. Lưu review gắn với service ticket/appointment/customer.

### Bước 8. Khách không đến

Trường hợp khách hủy trước giờ hẹn:

- Manager/cố vấn chuyển appointment sang `CANCELED`.
- Gửi thông báo hủy lịch cho khách nếu cần.

Trường hợp quá giờ/ngày hẹn mà khách không check-in:

- Scheduler tự kiểm tra appointment quá hạn.
- Nếu appointment vẫn chưa `CHECKED_IN`, hệ thống chuyển appointment sang `EXPIRED`.
- Nếu đã có service ticket liên quan nhưng chưa xử lý, chuyển service ticket sang `EXPIRED`.

## 7. Luồng đặt lịch lái thử

### Bước 1. Customer tạo lịch lái thử

Customer nhập:

- Họ tên, email, số điện thoại nếu chưa đăng nhập.
- Dòng xe hoặc phiên bản xe muốn lái thử.
- Đại lý.
- Ngày giờ mong muốn.
- Ghi chú yêu cầu thêm nếu có.

Hệ thống xử lý:

1. Nếu user đã đăng nhập, dùng thông tin tài khoản.
2. Nếu guest, validate họ tên, email, số điện thoại.
3. Kiểm tra xe/dòng xe/phiên bản xe tồn tại và có thể lái thử.
4. Kiểm tra đại lý tồn tại và đang hoạt động.
5. Kiểm tra ngày/giờ hợp lệ theo quy tắc 12 tiếng, 4 tháng, ngày nghỉ và khung giờ trống.
6. Tạo appointment loại `TEST_DRIVE` với trạng thái `PENDING`.

Kết quả:

- `APPOINTMENT.status = PENDING`

### Bước 2. Manager/Cố vấn duyệt appointment

Cố vấn dịch vụ hoặc manager:

1. Kiểm tra yêu cầu lái thử có hợp lệ không.
2. Gọi cho khách để xác nhận.
3. Có thể điều chỉnh ngày giờ nếu cần.
4. Chuyển appointment từ `PENDING` sang `CONFIRMED`.
5. Gửi email/thông báo xác nhận cho khách.

Kết quả:

- `APPOINTMENT.status = CONFIRMED`

### Bước 3. Khách hàng đến đại lý

Khi khách tới:

1. Cố vấn dịch vụ xác nhận khách đã đến.
2. Cập nhật appointment sang `CHECKED_IN`.
3. Chuẩn bị xe lái thử và hỗ trợ khách.

Kết quả:

- `APPOINTMENT.status = CHECKED_IN`

### Bước 4. Sau khi lái thử xong

Sau khi khách hoàn tất lái thử:

1. Cố vấn dịch vụ cập nhật appointment sang `COMPLETED`.
2. Gửi email cảm ơn.
3. Gửi khảo sát đánh giá.

Kết quả:

- `APPOINTMENT.status = COMPLETED`
- Khách có thể đánh giá trải nghiệm lái thử nếu nghiệp vụ cho phép.

### Bước 5. Khách không đến

Trường hợp khách hủy trước giờ hẹn:

- Cố vấn/manager set appointment thành `CANCELED`.

Trường hợp quá giờ/ngày hẹn mà khách không check-in:

- Scheduler tự chuyển appointment sang `EXPIRED`.

## 8. Chức năng theo từng actor

### 8.1 User thường / Customer

- Đặt lịch lái thử xe.
- Đặt lịch bảo dưỡng/sửa chữa.
- Với user đã đăng nhập: xem lịch sử đặt lịch.
- Xem lịch sử dịch vụ.
- Xem hóa đơn dịch vụ.
- Xem trạng thái hiện tại của appointment/service.
- Đánh giá dịch vụ hoặc trải nghiệm sau khi hoàn tất.

### 8.2 Kỹ thuật viên

- Xem danh sách service ticket chờ tiếp nhận hoặc được gán.
- Tiếp nhận dịch vụ bảo dưỡng/sửa chữa.
- Cập nhật trạng thái service: `CONFIRMED` -> `RECEIVING` -> `PROCESSING` -> `COMPLETED`.
- Ghi nhận tình trạng xe.
- Thêm hạng mục sửa chữa/bảo dưỡng.
- Xem lịch sử dịch vụ đã phục vụ.
- Xem đánh giá của khách hàng đối với các service đã phục vụ.

Ghi chú: Kỹ thuật viên không nên là người duyệt appointment ban đầu. Việc duyệt/xác nhận lịch thuộc cố vấn dịch vụ hoặc manager.

### 8.3 Cố vấn dịch vụ / Manager đại lý

- Set ngày nghỉ của đại lý để khách không đặt được vào ngày đó.
- Quản lý khung giờ làm việc của đại lý.
- Xem danh sách appointment đang chờ.
- Duyệt yêu cầu đặt lịch.
- Từ chối/hủy yêu cầu đặt lịch.
- Điều chỉnh ngày giờ appointment nếu cần.
- Check-in khách khi khách tới.
- Với lịch bảo dưỡng/sửa chữa: tạo service ticket và gán kỹ thuật viên.
- Xem lịch sử lịch mình đã duyệt.
- Gán xe vào tài khoản khách hàng.
- Xem báo cáo đặt lịch, dịch vụ, hợp đồng mua xe và hiệu suất kỹ thuật viên thuộc đại lý mình.

## 9. Quy tắc chuyển trạng thái đề xuất

### 9.1 Appointment TEST_DRIVE

```text
PENDING
  -> CONFIRMED
  -> CHECKED_IN
  -> COMPLETED
```

Nhánh phụ:

```text
PENDING/CONFIRMED -> CANCELED
PENDING/CONFIRMED -> EXPIRED
PENDING -> REJECTED
```

### 9.2 Appointment SERVICE

```text
PENDING
  -> CONFIRMED
  -> CHECKED_IN
  -> COMPLETED
```

Nhánh phụ:

```text
PENDING/CONFIRMED -> CANCELED
PENDING/CONFIRMED -> EXPIRED
PENDING -> REJECTED
```

### 9.3 Service Ticket

```text
CONFIRMED
  -> RECEIVING
  -> PROCESSING
  -> COMPLETED
```

Nhánh phụ:

```text
CONFIRMED/RECEIVING -> CANCELED
CONFIRMED -> EXPIRED
```

## 10. Quy tắc validation quan trọng

### Khi tạo lịch bảo dưỡng/sửa chữa

- `vin` bắt buộc.
- VIN phải tồn tại trong hệ thống.
- Nếu khách đã đăng nhập, có thể kiểm tra xe/VIN có thuộc tài khoản đó hay không. Nếu là xe chưa gán, cần có chính sách rõ: cho đặt bằng guest info hoặc yêu cầu xác minh.
- `dealershipId` bắt buộc.
- `appointmentDate` và `timeSlotId` bắt buộc.
- `description` tình trạng xe nên bắt buộc hoặc tối thiểu phải có nội dung rõ.
- Không cho đặt ngày/giờ ngoài quy tắc 12 tiếng và 4 tháng.

### Khi tạo lịch lái thử

- Phải có `carVersionId` hoặc thông tin dòng xe/phiên bản xe muốn lái thử.
- `dealershipId` bắt buộc.
- `appointmentDate` và `timeSlotId` bắt buộc.
- Guest phải có họ tên, email, số điện thoại.
- Không cho đặt ngày/giờ ngoài quy tắc 12 tiếng và 4 tháng.

### Khi duyệt lịch

- Chỉ duyệt appointment đang `PENDING`.
- Nếu đổi ngày/giờ, phải kiểm tra lại ngày nghỉ, khung giờ và giới hạn đặt lịch.
- Chỉ cố vấn/manager thuộc đúng đại lý hoặc admin mới được duyệt.

### Khi check-in

- Chỉ check-in appointment đang `CONFIRMED`.
- Không check-in appointment đã `CANCELED`, `EXPIRED`, `REJECTED`, `COMPLETED`.
- Với lịch service, check-in phải tạo service ticket nếu chưa có.

### Khi hoàn tất service

- Chỉ hoàn tất service đang `PROCESSING`.
- Nếu có yêu cầu thanh toán/hóa đơn, phải hoàn tất hoặc ghi nhận thanh toán trước khi set `COMPLETED`.
- Khi service completed, appointment liên quan cũng chuyển `COMPLETED`.

### Khi đánh giá

- Chỉ đánh giá appointment/service đã `COMPLETED`.
- Với bảo dưỡng/sửa chữa, ưu tiên kiểm tra `SERVICE_TICKET.status = COMPLETED`.
- Không cho đánh giá trùng nếu đã có review hợp lệ.

## 11. Email và thông báo

Các thời điểm nên gửi email/thông báo:

- Sau khi customer tạo appointment thành công.
- Khi appointment được xác nhận.
- Khi appointment bị từ chối/hủy.
- Trước giờ hẹn 2 tiếng.
- Khi service hoàn tất.
- Email cảm ơn và khảo sát đánh giá sau lái thử hoặc sau service.

Scheduler cần xử lý:

- Gửi reminder trước 2 tiếng.
- Tự chuyển appointment quá hạn sang `EXPIRED` nếu chưa check-in.
- Tự chuyển service ticket sang `EXPIRED` nếu có service ticket quá hạn theo rule nghiệp vụ.

## 12. Gợi ý màn hình Frontend

### Customer

- Form đặt lịch lái thử.
- Form đặt lịch bảo dưỡng/sửa chữa.
- Màn hình chọn đại lý, ngày và khung giờ trống.
- Trang lịch sử appointment.
- Trang chi tiết appointment/service.
- Trang hóa đơn dịch vụ.
- Form đánh giá sau khi hoàn tất.

### Cố vấn dịch vụ / Manager

- Danh sách appointment `PENDING`.
- Màn hình chi tiết appointment để duyệt/từ chối/đổi lịch.
- Màn hình check-in khách.
- Form tạo service ticket và gán kỹ thuật viên.
- Quản lý ngày nghỉ và khung giờ làm việc.
- Báo cáo lịch hẹn, service, hợp đồng và kỹ thuật viên.

### Kỹ thuật viên

- Danh sách service ticket được gán.
- Chi tiết service ticket.
- Form ghi nhận tình trạng xe.
- Form thêm service item.
- Action tiếp nhận, bắt đầu xử lý, hoàn tất.
- Lịch sử dịch vụ đã phục vụ và đánh giá từ khách.

## 13. Ghi chú triển khai

- Appointment và service ticket là 2 khái niệm khác nhau. Không nên nhét toàn bộ quá trình sửa chữa vào appointment.
- Lịch lái thử chỉ cần appointment lifecycle, không cần service ticket.
- Lịch bảo dưỡng/sửa chữa cần appointment trước, service ticket sau khi check-in.
- Các trạng thái phải được kiểm soát ở service layer, không để FE truyền trạng thái tùy ý.
- Mọi API ghi dữ liệu cần kiểm tra quyền theo role và đại lý phụ trách.
- Các mốc thời gian nên lưu rõ timezone hoặc dùng `Instant` để tránh sai lệch khi scheduler chạy.
- 