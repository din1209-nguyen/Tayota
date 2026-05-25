# Đặc tả workspace Manager

## Mục tiêu

Workspace `/dashboard/manager` cho phép `MANAGER` tư vấn khách hàng bằng live
chat và quản trị nội dung hiển thị trên website Tayota. Manager không xử lý
doanh thu, nghiệp vụ lịch hẹn hoặc chức năng bảo mật tài khoản.

## Chức năng

| Module | Chức năng Manager |
| --- | --- |
| Live Chat | Xem phiên chờ/đang chat, nhận phiên, gửi tin nhắn, hoàn tất và đóng phiên. |
| Xe | Quản lý catalog phiên bản xe, thông số, giá theo màu, gallery, video và trạng thái hiển thị. Xe chỉ được ẩn, không xóa vật lý. |
| Bài viết | Thêm/sửa/ẩn bài viết chung hoặc bài viết gắn phiên bản xe; bài viết xuất bản hiển thị tại `/news` hoặc chi tiết xe. |
| Đại lý | Xem/thêm/sửa/ngừng hoạt động/kích hoạt lại; đại lý ngừng hoạt động không xuất hiện công khai. |
| Phụ kiện | Xem/thêm/sửa/ẩn phụ kiện và liên kết với phiên bản xe; không quản lý tồn kho theo đại lý. |
| Người dùng | Xem, thống kê và sửa hồ sơ của `SERVICE_ADVISOR`, `ASSISTANT`, `MECHANIC`, `USER`. |

## Quy tắc nghiệp vụ

- Manager quản trị catalog website, không quản lý xe vật lý theo VIN.
- Thao tác xóa đối với xe, bài viết, đại lý và phụ kiện là ẩn/ngừng hoạt động
  để bảo toàn lịch sử tham chiếu.
- `AccessoryInventory` và `Dealership.accessoryQuantity` không thuộc domain
  hiện hành; phụ kiện chỉ là dữ liệu catalog.
- Manager không tạo tài khoản, đổi role, reset mật khẩu, khóa/mở khóa hoặc gán
  đại lý cho nhân sự trong workspace này.
- Tất cả request frontend đi qua API Gateway; giữ cơ chế refresh token cookie
  `HttpOnly` và single-flight refresh hiện có.

## API chính

- Public: `GET /car/catalog/**`, `GET /car/dealerships`, `GET /car/news`,
  `GET /car/news/{id}`.
- Manager content: `/car/manager/car-versions`, `/car/manager/articles`,
  `/car/manager/dealerships`, `/car/manager/accessories`.
- Catalog writes dùng chung cho Admin/Manager: `/car/car-versions/**`,
  `/car/car-styles/**`, `/car/car-series/**`, `/car/accessories/**`.
- Manager users: `GET /user/manager/users`, `GET /user/manager/users/stats`,
  `GET /user/manager/users/{id}`; cập nhật hồ sơ qua `PUT /user/profile`.
- Chat tiếp tục dùng `/user/assistant/chat/**` và `/user/chat/ws/**`.

## Kiểm tra bàn giao

- Public không đọc được xe, bài viết, đại lý hoặc phụ kiện đã ẩn.
- Manager không xem/sửa được tài khoản `ADMIN` hoặc `MANAGER`.
- `/news` và `/news/[id]` hiển thị bài viết chung đã xuất bản.
- Chạy frontend `npm run lint`, `npm run build`; chạy xác minh backend qua
  Docker Compose theo `AGENTS.md`.
