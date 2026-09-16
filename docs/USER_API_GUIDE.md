# Tài Liệu API Quản Lý Người Dùng (User Management API)

Tài liệu chi tiết toàn bộ các Endpoint thuộc module `User` (`vn.org.thn.app.modules.user`) của dự án `app-service`.

- **Base URL**: `/public/user`
- **Định dạng dữ liệu**: `JSON` (UTF-8)
- **Chuẩn Cấu Trúc Response**: Tất cả các API đều trả về dạng `ApiResponse<T>` chuẩn (xem mục 1).
- **Xác thực**: 2 endpoint đọc (`GET`) yêu cầu JWT hợp lệ với role `USER` hoặc `ADMIN`; 3 endpoint ghi (thêm/sửa/xóa) yêu cầu JWT hợp lệ với role `ADMIN`. Không có endpoint nào của module này là công khai (`permitAll`). Rule khai báo tại `vn.org.thn.app.modules.user.api.UserSecurityRules` (xem `BASE_FRAMEWORK_GUIDE.md` mục 10). Lấy token qua `POST /public/auth/login` (chỉ hoạt động khi `base.security.jwt.mode=STANDALONE`), gửi kèm mỗi request qua header `Authorization: Bearer <accessToken>`.

---

## 1. Dạng Cấu Trúc Response Chuẩn (`ApiResponse<T>`)

`ApiResponse<T>` (`vn.org.thn.app.base.core.response.ApiResponse`) luôn có đủ 5 trường sau (kể cả khi lỗi, `data` bị lược bỏ khỏi JSON vì cấu hình `@JsonInclude(NON_NULL)`):

### Success Response (`200 OK`)
```json
{
  "success": true,
  "code": "COMMON_000",
  "message": "Success",
  "data": { "...": "..." },
  "timestamp": "2026-09-16T09:15:30.123456Z"
}
```

### Error Response (ví dụ `404 Not Found`)
```json
{
  "success": false,
  "code": "COMMON_005",
  "message": "User not found with id: 999",
  "timestamp": "2026-09-16T09:16:02.987654Z"
}
```

> Lưu ý: `timestamp` là thời điểm server build response (`Instant.now()`), định dạng ISO-8601 UTC.

---

## 2. Danh Sách Các Endpoint API

| Method | Endpoint | Mô tả | Yêu Cầu Xác Thực |
|---|---|---|---|
| `GET` | `/public/user/page` | Lấy danh sách user phân trang, hỗ trợ lọc theo `keyword` và `status` | 🔒 `USER` hoặc `ADMIN` |
| `GET` | `/public/user/{id}` | Lấy chi tiết 1 user theo `id` | 🔒 `USER` hoặc `ADMIN` |
| `POST` | `/public/user` | Tạo user mới | 🔒 `ADMIN` |
| `PUT` | `/public/user/{id}` | Cập nhật thông tin user (partial update) | 🔒 `ADMIN` |
| `DELETE` | `/public/user/{id}` | Xóa user theo `id` | 🔒 `ADMIN` |

---

## 3. Chi Tiết Từng Endpoint

### 3.1. Lấy Danh Sách User Phân Trang (Search / List)
- **HTTP Method**: `GET`
- **URL**: `/public/user/page`
- **Query Parameters**:
  - `page` *(int, optional, mặc định `1`)*: số trang, đánh số từ 1.
  - `size` *(int, optional, mặc định `20`)*: số bản ghi mỗi trang.
  - `keyword` *(String, optional)*: tìm theo `username` (LIKE, khớp một phần) **HOẶC** `email` (LIKE, khớp một phần) **HOẶC** `fullName` (so khớp không phân biệt hoa/thường, không dấu, không quan trọng thứ tự từ - qua cột `full_name_unaccent`, xem `BASE_FRAMEWORK_GUIDE.md` mục 2.8).
  - `status` *(String, optional)*: lọc chính xác theo `status` (ví dụ `ACTIVE`, `INACTIVE`) - so khớp bằng (`=`), không phải LIKE.
- **⚠️ Giới hạn đã biết**: nếu truyền `page=0` hoặc `size` âm/`0`, API hiện trả về `500 Internal Server Error` (`COMMON_999`) thay vì `400 Bad Request` thân thiện - do `QueryBuilder.page()` ném `IllegalArgumentException` và `GlobalExceptionHandler` chưa có handler riêng cho exception này (rơi vào catch-all `Exception.class`). Client nên tự đảm bảo `page >= 1` và `size >= 1` trước khi gọi.

#### Request Example:
```http
GET /public/user/page?page=1&size=20&keyword=nghia&status=ACTIVE HTTP/1.1
Host: localhost:8080
Authorization: Bearer <accessToken>
```

#### Response Example (`200 OK`):
```json
{
  "success": true,
  "code": "COMMON_000",
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "username": "john_doe",
        "email": "john.doe@example.com",
        "fullName": "Trương Hiếu Nghĩa",
        "status": "ACTIVE",
        "role": "USER",
        "createdAt": "2026-09-10T08:30:00",
        "updatedAt": "2026-09-10T08:30:00",
        "createdBy": "system",
        "updatedBy": "system"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2026-09-16T09:15:30.123456Z"
}
```

---

### 3.2. Lấy Chi Tiết User Theo ID
- **HTTP Method**: `GET`
- **URL**: `/public/user/{id}`
- **Path Variables**:
  - `id` *(Long, required)*.

#### Request Example:
```http
GET /public/user/1 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <accessToken>
```

#### Response Example (`200 OK`):
```json
{
  "success": true,
  "code": "COMMON_000",
  "message": "Success",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john.doe@example.com",
    "fullName": "Trương Hiếu Nghĩa",
    "status": "ACTIVE",
    "role": "USER",
    "createdAt": "2026-09-10T08:30:00",
    "updatedAt": "2026-09-10T08:30:00",
    "createdBy": "system",
    "updatedBy": "system"
  },
  "timestamp": "2026-09-16T09:16:02.987654Z"
}
```

#### Response Example (`404 Not Found` - id không tồn tại):
```json
{
  "success": false,
  "code": "COMMON_005",
  "message": "User not found with id: 999",
  "timestamp": "2026-09-16T09:16:02.987654Z"
}
```

---

### 3.3. Tạo User Mới
- **HTTP Method**: `POST`
- **URL**: `/public/user`
- **Request Body**: `UserCreateRequest`

#### Request Body Schema:
```json
{
  "username": "string (required, 3-50 ký tự)",
  "email": "string (required, đúng định dạng email)",
  "fullName": "string (optional, tối đa 100 ký tự)",
  "role": "string (optional, chỉ nhận ADMIN hoặc USER - mặc định USER nếu bỏ trống)",
  "password": "string (required, 8-100 ký tự - dùng cho đăng nhập khi base.security.jwt.mode=STANDALONE)"
}
```
> `status` **không** nằm trong request tạo mới - luôn tự động là `ACTIVE`. Mật khẩu được băm bằng BCrypt trước khi lưu, không bao giờ trả lại trong response.

#### Request Example:
```http
POST /public/user HTTP/1.1
Content-Type: application/json
Authorization: Bearer <accessToken của user role ADMIN>

{
  "username": "jane_doe",
  "email": "jane.doe@example.com",
  "fullName": "Trương Thị Jane",
  "role": "USER",
  "password": "S3curePass!"
}
```

#### Response Example (`200 OK`):
```json
{
  "success": true,
  "code": "COMMON_000",
  "message": "Success",
  "data": {
    "id": 2,
    "username": "jane_doe",
    "email": "jane.doe@example.com",
    "fullName": "Trương Thị Jane",
    "status": "ACTIVE",
    "role": "USER",
    "createdAt": "2026-09-16T09:20:00",
    "updatedAt": "2026-09-16T09:20:00",
    "createdBy": "admin",
    "updatedBy": "admin"
  },
  "timestamp": "2026-09-16T09:20:00.555555Z"
}
```

#### Response Example (`400 Bad Request` - trùng username):
```json
{
  "success": false,
  "code": "COMMON_001",
  "message": "Username already exists: jane_doe",
  "timestamp": "2026-09-16T09:21:00.111111Z"
}
```

#### Response Example (`400 Bad Request` - lỗi validation nhiều trường):
```json
{
  "success": false,
  "code": "COMMON_001",
  "message": "Username must not be blank; Password must not be blank",
  "timestamp": "2026-09-16T09:21:30.222222Z"
}
```

---

### 3.4. Cập Nhật User
- **HTTP Method**: `PUT`
- **URL**: `/public/user/{id}`
- **Path Variables**: `id` *(Long, required)*.
- **Request Body**: `UserUpdateRequest` - **partial update**: chỉ trường nào có giá trị (không `null`) mới được cập nhật, bỏ trống nghĩa là giữ nguyên giá trị cũ.

#### Request Body Schema:
```json
{
  "email": "string (optional, đúng định dạng email)",
  "fullName": "string (optional, tối đa 100 ký tự)",
  "status": "string (optional, chỉ nhận ACTIVE hoặc INACTIVE)",
  "role": "string (optional, chỉ nhận ADMIN hoặc USER)"
}
```

#### Request Example:
```http
PUT /public/user/2 HTTP/1.1
Content-Type: application/json
Authorization: Bearer <accessToken của user role ADMIN>

{
  "status": "INACTIVE"
}
```

#### Response Example (`200 OK`):
```json
{
  "success": true,
  "code": "COMMON_000",
  "message": "Success",
  "data": {
    "id": 2,
    "username": "jane_doe",
    "email": "jane.doe@example.com",
    "fullName": "Trương Thị Jane",
    "status": "INACTIVE",
    "role": "USER",
    "createdAt": "2026-09-16T09:20:00",
    "updatedAt": "2026-09-16T09:25:00",
    "createdBy": "admin",
    "updatedBy": "admin"
  },
  "timestamp": "2026-09-16T09:25:00.333333Z"
}
```

#### Response Example (`404 Not Found`):
```json
{
  "success": false,
  "code": "COMMON_005",
  "message": "User not found with id: 999",
  "timestamp": "2026-09-16T09:26:00.444444Z"
}
```

---

### 3.5. Xóa User
- **HTTP Method**: `DELETE`
- **URL**: `/public/user/{id}`
- **Path Variables**: `id` *(Long, required)*.

#### Request Example:
```http
DELETE /public/user/2 HTTP/1.1
Authorization: Bearer <accessToken của user role ADMIN>
```

#### Response Example (`200 OK`):
```json
{
  "success": true,
  "code": "COMMON_000",
  "message": "Success",
  "timestamp": "2026-09-16T09:30:00.666666Z"
}
```

#### Response Example (`404 Not Found`):
```json
{
  "success": false,
  "code": "COMMON_005",
  "message": "User not found with id: 999",
  "timestamp": "2026-09-16T09:31:00.777777Z"
}
```

---

## 4. Các Mã Lỗi Thường Gặp (Error Codes)

| Error Code | HTTP Status | Mô tả |
|---|---|---|
| `COMMON_001` | `400 Bad Request` | Validation thất bại trên request body (`@NotBlank`/`@Size`/`@Email`/`@Pattern`), hoặc trùng `username`/`email` phát hiện được ở tầng Service trước khi ghi CSDL. |
| `COMMON_005` | `404 Not Found` | Không tìm thấy user với `id` được yêu cầu (`getById`/`update`/`delete`). |
| `COMMON_006` | `409 Conflict` | Trùng `username`/`email` phát hiện muộn ở tầng CSDL (race condition giữa lúc kiểm tra và lúc ghi - `DataIntegrityViolationException`). |
| `COMMON_999` | `500 Internal Error` | Lỗi máy chủ chưa được xử lý riêng - bao gồm cả trường hợp `page`/`size` không hợp lệ ở mục 3.1 (giới hạn đã biết, chưa map về 400). |

> **Quan trọng**: `401 Unauthorized` (thiếu/token sai/hết hạn) và `403 Forbidden` (token hợp lệ nhưng sai role) **không** đi qua `ApiResponse` ở trên - hai lỗi này bị Spring Security chặn ngay tại filter chain, **trước khi** request tới được Controller/`GlobalExceptionHandler`, nên response body theo mặc định của Spring Security/Spring Boot (không đảm bảo đúng cấu trúc `ApiResponse`), không phải mã `COMMON_003`/`COMMON_004` trong `CommonErrorCode`. Hai mã đó trong code hiện chỉ được dùng cho `POST /public/auth/login` khi sai username/password (`COMMON_003`), không dùng cho module này.
