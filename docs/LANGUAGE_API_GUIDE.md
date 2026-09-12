# Tài Liệu API Quản Lý Ngôn Ngữ & Dịch Thuật (Language & i18n API)

Tài liệu chi tiết toàn bộ các Endpoint thuộc phân hệ quản lý ngôn ngữ đa ngữ (i18n) của dự án `app-service`.

- **Base URL**: `/public/language`
- **Định dạng dữ liệu**: `JSON` (UTF-8)
- **Chuẩn Cấu Trúc Response**: Tất cả các API đều trả về dạng `ApiResponse<T>` chuẩn.

---

## 1. Dạng Cấu Trúc Response Chuẩn (`ApiResponse<T>`)

### Success Response (`200 OK`)
```json
{
  "code": "00",
  "message": "Success",
  "data": { ... }
}
```

### Error Response (`400 Bad Request` / `500 Internal Error`)
```json
{
  "code": "VAL_001",
  "message": "langKey must not be blank",
  "data": null
}
```

---

## 1.1. Cơ Chế Khởi Tạo & Đồng Bộ Ngôn Ngữ (Startup Batch Ingestion)
Khi ứng dụng khởi chạy, `LanguageService.loadLanguage()` thực hiện nạp dữ liệu theo quy trình tối ưu:
1. **Đọc File**: Quét toàn bộ file JSON trong thư mục `lang/*.json` và nạp vào bộ nhớ Cache.
2. **Đối Soát (Smart Diffing)**: Truy vấn danh sách khóa ngôn ngữ hiện có trong CSDL để so sánh đối soát với file JSON.
3. **Lưu Hàng Loạt (Batch Insert)**: Chỉ trích xuất các cặp `(langKey, lang)` chưa tồn tại trong CSDL và thực hiện lưu hàng loạt qua `saveAll()`.
- **Ưu điểm**:
  - Loại bỏ hoàn toàn nguy cơ xung đột khóa chính (Primary Key Collision).
  - Tối ưu hiệu năng, giảm thời gian khởi động ứng dụng xuống nhiều lần so với cơ chế insert từng dòng.
  - Bảo toàn các giá trị dịch người dùng đã cập nhật hoặc tùy biến trên CSDL.

---

## 2. Danh Sách Các Endpoint API

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/public/language/list` | Lấy danh sách tất cả các từ khóa dịch (có hỗ trợ tìm kiếm `keyword`) |
| `GET` | `/public/language` | Lấy toàn bộ bản dịch của tất cả ngôn ngữ |
| `GET` | `/public/language/{lang}` | Lấy toàn bộ từ khóa dịch của một ngôn ngữ cụ thể (ví dụ `/vi`, `/en`) |
| `POST` / `PUT` | `/public/language` | Thêm mới hoặc cập nhật giá trị dịch cho từ khóa |
| `DELETE` | `/public/language` | Xóa 1 từ khóa dịch |
| `DELETE` | `/public/language/deletes` | Xóa nhiều từ khóa dịch cùng lúc |
| `POST` | `/public/language/export` | Tải xuống file zip chứa toàn bộ file JSON ngôn ngữ (`lang.zip`) |

---

## 3. Chi Tiết Từng Endpoint

### 3.1. Lấy Danh Sách Từ Khóa Dịch (Search / List)
- **HTTP Method**: `GET`
- **URL**: `/public/language/list`
- **Query Parameters**:
  - `keyword` *(String, optional)*: Từ khóa tìm kiếm theo `langKey` hoặc `value`.

#### Request Example:
```http
GET /public/language/list?keyword=button.save HTTP/1.1
Host: localhost:8080
```

#### Response Example (`200 OK`):
```json
{
  "code": "00",
  "message": "Success",
  "data": [
    {
      "langKey": "button.save",
      "vi": "Lưu lại",
      "en": "Save"
    },
    {
      "langKey": "button.cancel",
      "vi": "Hủy bỏ",
      "en": "Cancel"
    }
  ]
}
```

---

### 3.2. Lấy Tất Cả Dịch Thuật Theo Mã Ngôn Ngữ
- **HTTP Method**: `GET`
- **URL**: `/public/language/{lang}`
- **Path Variables**:
  - `lang` *(String, required)*: Mã ngôn ngữ (ví dụ: `vi`, `en`).

#### Request Example:
```http
GET /public/language/vi HTTP/1.1
Host: localhost:8080
```

#### Response Example (`200 OK`):
```json
{
  "code": "00",
  "message": "Success",
  "data": {
    "button.save": "Lưu lại",
    "button.cancel": "Hủy bỏ",
    "message.success": "Thành công"
  }
}
```

---

### 3.3. Lấy Toàn Bộ Ngôn Ngữ Hệ Thống (Map All)
- **HTTP Method**: `GET`
- **URL**: `/public/language`

#### Request Example:
```http
GET /public/language HTTP/1.1
Host: localhost:8080
```

#### Response Example (`200 OK`):
```json
{
  "code": "00",
  "message": "Success",
  "data": {
    "vi": {
      "button.save": "Lưu lại",
      "button.cancel": "Hủy bỏ"
    },
    "en": {
      "button.save": "Save",
      "button.cancel": "Cancel"
    }
  }
}
```

---

### 3.4. Thêm Mới Hoặc Cập Nhật Từ Khóa Dịch (Upsert)
- **HTTP Method**: `POST` hoặc `PUT`
- **URL**: `/public/language`
- **Request Body**: `LanguageRequest`

#### Request Body Schema:
```json
{
  "langKey": "string (required)",
  "mapValues": {
    "lang_code_1": "string_value_1",
    "lang_code_2": "string_value_2"
  }
}
```

#### Request Example:
```http
POST /public/language HTTP/1.1
Content-Type: application/json

{
  "langKey": "common.welcome",
  "mapValues": {
    "vi": "Xin chào người dùng",
    "en": "Welcome User"
  }
}
```

#### Response Example (`200 OK`):
```json
{
  "code": "00",
  "message": "Success",
  "data": null
}
```

---

### 3.5. Xóa 一 Từ Khóa Dịch
- **HTTP Method**: `DELETE`
- **URL**: `/public/language`
- **Request Body**: `String` (Chuỗi JSON chứa `langKey`)

#### Request Example:
```http
DELETE /public/language HTTP/1.1
Content-Type: application/json

"common.welcome"
```

#### Response Example (`200 OK`):
```json
{
  "code": "00",
  "message": "Success",
  "data": null
}
```

---

### 3.6. Xóa Hàng Loạt Từ Khóa Dịch (Batch Delete)
- **HTTP Method**: `DELETE`
- **URL**: `/public/language/deletes`
- **Request Body**: `List<String>` (Mảng các `langKey`)

#### Request Example:
```http
DELETE /public/language/deletes HTTP/1.1
Content-Type: application/json

[
  "button.save",
  "button.cancel"
]
```

#### Response Example (`200 OK`):
```json
{
  "code": "00",
  "message": "Success",
  "data": null
}
```

---

### 3.7. Export Bản Dịch Dạng File Zip
- **HTTP Method**: `POST`
- **URL**: `/public/language/export`
- **Response Content-Type**: `application/zip`
- **File trả về**: `lang.zip` chứa các file `lang/vi.json`, `lang/en.json`...

#### Request Example:
```http
POST /public/language/export HTTP/1.1
Host: localhost:8080
```

---

## 4. Các Mã Lỗi Thường Gặp (Error Codes)

| Error Code | HTTP Status | Mô tả |
|---|---|---|
| `VAL_001` | `400 Bad Request` | Dữ liệu đầu vào không hợp lệ (ví dụ `langKey` trống hoặc `lang` chứa ký tự nguy hiểm/path traversal) |
| `INT_001` | `500 Internal Error` | Lỗi máy chủ chưa được xử lý |
