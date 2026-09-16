# 🤖 Chỉ Thị Bắt Buộc Cho AI Coding Agent (`AGENT.md`)

> **CHÚ Ý CHO AI CODING AGENT:**  
> Bạn vừa được khởi tạo trong dự án **`app-service`**. Đây là file quy chuẩn đầu tiên bạn **BẮT BUỘC PHẢI ĐỌC VÀ TUÂN THỦ** trước khi thực hiện bất kỳ hành động xem, sửa đổi hoặc viết code nào trong dự án này.

---

## 📌 1. Danh Sách Tài Liệu Bắt Buộc Đọc (Mandatory Reading)

Trước khi thực hiện yêu cầu của người dùng, hãy mở và đọc các tài liệu hướng dẫn sau tùy theo loại nhiệm vụ:

1. 📘 **Quy chuẩn Kiến trúc & Cấu trúc Thư mục**:
   👉 [MICROSERVICE_ARCHITECTURE_GUIDE.md](file:///Volumes/Data/04.MyProject/java-project/app-service/docs/MICROSERVICE_ARCHITECTURE_GUIDE.md)
   *(Đọc file này để biết vị trí đặt class, cấu trúc 4 tầng Clean Architecture, và quy định phân chia package).*

2. 📗 **Hướng dẫn Cú pháp & ORM Framework**:
   👉 [BASE_FRAMEWORK_GUIDE.md](file:///Volumes/Data/04.MyProject/java-project/app-service/docs/BASE_FRAMEWORK_GUIDE.md)
   *(Đọc file này để biết cách dùng `QueryBuilder`, `Save/Upsert`, `UpdateBuilder`, `DeleteBuilder`, Custom MyBatis Mapper, `BaseCtl`, `ApiResponse`, `@Transactional` và Flyway Migration. **Mục 10** hướng dẫn riêng về JWT Auth/Authorization - đọc trước khi tạo endpoint mới hoặc đổi rule phân quyền).*

3. 📙 **Tài liệu API Đa Ngôn Ngữ (i18n)**:
   👉 [LANGUAGE_API_GUIDE.md](file:///Volumes/Data/04.MyProject/java-project/app-service/docs/LANGUAGE_API_GUIDE.md)
   *(Đọc file này khi cần xử lý hoặc gọi API liên quan đến đa ngôn ngữ và dịch thuật).*

4. 💬 **Bộ Mẫu Câu Lệnh Cho Người Dùng (Prompt Templates)**:
   👉 [PROMPT_TEMPLATES.md](file:///Volumes/Data/04.MyProject/java-project/app-service/docs/PROMPT_TEMPLATES.md)
   *(Tham khảo file này để hiểu các dạng câu lệnh/yêu cầu mẫu từ người dùng).*

---

## 🚨 2. Các Điều Luật Vàng Cho AI Agent (Golden Execution Rules)

Tất cả các AI Agent (Antigravity, Claude, ChatGPT, Cursor, Copilot...) **bắt buộc tuân thủ 100%** 11 điều luật sau mà không có ngoại lệ:

### ⚡ Điều 1: Định Vị Package Chuẩn Microservice
- Mọi class nghiệp vụ mới **phải nằm trong `vn.org.thn.app.modules.<module_name>.*`** (Ví dụ: `vn.org.thn.app.modules.user`, `vn.org.thn.app.modules.order`).
- **TUYỆT ĐỐI KHÔNG** tạo package lẻ ở ngoài root package hoặc trong package legacy `demo`.

### ⚡ Điều 2: Tái Sử Dụng Base Framework Core
- Không tự tạo lại `ApiResponse`, `PageResponse`, `JsonUtils`, `BaseException`, `CommonErrorCode`, hay `GlobalExceptionHandler`.
- Bắt buộc `import` và tái sử dụng từ `vn.org.thn.app.base.*`.
- **TUYỆT ĐỐI KHÔNG** tự ý sửa đổi code trong package `vn.org.thn.app.base.*` trừ khi có yêu cầu nâng cấp framework từ người dùng.

### ⚡ Điều 3: Cú Pháp QueryBuilder Type-Safe & An Toàn
- Khi truy vấn dữ liệu CSDL qua Repository, **bắt buộc dùng Method Reference** `eq(Entity::getFieldName, value)` thay vì truyền tên cột dạng String cứng.
- Ví dụ: `userRepository.query().eq(UserEntity::getUsername, "admin").one();`
- **Tìm kiếm đa từ & không dấu**: Khi tìm kiếm theo từ khóa người dùng, sử dụng `likeAnyOrder(Entity::getField, keyword)` cho cột gốc, hoặc `likeAnyOrderUnaccent(Entity::getUnaccentField, keyword)` kết hợp annotation `@Unaccent(from = "...")` trên entity.
- **An toàn dữ liệu**: Cả `UpdateBuilder` và `DeleteBuilder` **bắt buộc phải có điều kiện `WHERE`**. Không bao giờ được gọi `.execute()` mà không có điều kiện lọc.

### ⚡ Điều 4: Chuẩn Hóa Controller, Swagger Docs & Bean Validation
- Mọi REST Controller **phải kế thừa `BaseCtl`** và trả về kết quả qua `ok(...)` hoặc `fail(...)`.
- Bắt buộc gắn đầy đủ chú thích OpenAPI Swagger: `@Tag(name = "...")` và `@Operation(summary = "...")`.
- Mọi Request DTO nhận dữ liệu từ client **bắt buộc khai báo các Jakarta Validation annotations** (`@NotBlank`, `@NotNull`, `@Size`, `@Email`...) và Controller phải gắn `@Valid` trước `@RequestBody`.

### ⚡ Điều 5: Quản Lý CSDL Bằng Flyway Migration Đa Nền Tảng
- Mọi thay đổi bảng hoặc cột CSDL phải có file migration SQL tương ứng trong `database/<db_type>/V<N>__<description>.sql` (Ví dụ: `database/sqlite/V2__init_user.sql`).
- **Hỗ trợ 5 loại DB**: Phải tạo đồng bộ script cho cả 5 database: `sqlite`, `postgresql`, `mysql`, `oracle`, `sqlserver`.
- **Chuẩn Hóa BaseEntity & 5 Cột Audit**: Tất cả các bảng nghiệp vụ (Business Tables) **bắt buộc kế thừa `BaseEntity`** và có đủ 5 cột audit chuẩn (`created_at`, `updated_at`, `created_by`, `updated_by`, `deleted`). (Ngoại lệ duy nhất là các bảng ghi log append-only hoặc bảng liên kết M:N thuần túy).
- **Cơ Chế Auto-Audit Tự Động**: Khi gọi `save()` hoặc `saveAll()`, Base Framework tự động điền `createdAt`, `updatedAt`, `createdBy` (lấy từ `UserContext` hoặc fallback `"system"`), `updatedBy`, và `deleted = false`. Khi UPDATE, framework tự động cập nhật `updatedAt`/`updatedBy` và bảo vệ không ghi đè `createdAt`/`createdBy`. Lập trình viên và AI Agent **không cần gán tay các trường audit trong Service**.
- **Tra cứu DDL Type Mapping**: Bắt buộc tra cứu bảng kiểu dữ liệu tại `BASE_FRAMEWORK_GUIDE.md` mục 5.3:
  - Oracle: Dùng `NUMBER(1) DEFAULT 0` cho cột `deleted` và `NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY` cho ID. Tuyệt đối không dùng `BOOLEAN`.
  - SQL Server: Dùng `BIT DEFAULT 0` cho cột `deleted` và `IDENTITY(1,1)` cho ID. Tuyệt đối không dùng `BOOLEAN`.
  - PostgreSQL: Dùng `TIMESTAMP` và `BOOLEAN DEFAULT FALSE`.
  - MySQL: Dùng `DATETIME` và `TINYINT(1) DEFAULT 0`.

### ⚡ Điều 6: Custom MyBatis XML Mappers
- Khi cần viết câu SQL phức tạp, báo cáo, hoặc JOIN nhiều bảng:
  - File XML đặt tại thư mục root: `mapper/<Module>CustomMapper.xml`.
  - Interface Java gắn `@Mapper` đặt tại: `modules/<module_name>/infrastructure/mapper/<Module>CustomMapper.java`.
  - Trong Repository, gọi mapper qua helper method: `mapper(CustomMapper.class).findData(...)`.

### ⚡ Điều 7: Kiểm Thử Độc Lập (Unit Tests)
- Mỗi module tạo mới **bắt buộc phải có bài Unit Test tương ứng** đặt tại `src/test/java/vn/org/thn/app/modules/<module_name>/`.

### ⚡ Điều 8: Thực Thi Lệnh Xác Minh Sau Khi Viết Code
- Sau khi chỉnh sửa hoặc viết xong code, AI Agent **bắt buộc thực thi lệnh kiểm thử** và xác minh kết quả build thành công trước khi báo lại người dùng:
  ```bash
  export JAVA_HOME=/Users/truonghieunghia/Library/Java/JavaVirtualMachines/azul-17.0.20.1/Contents/Home && ./gradlew test --no-daemon
  ```

### ⚡ Điều 9: Phân Quyền Endpoint Bắt Buộc (Security/JWT)
- Từ 2026-09-13, `base` mang theo sẵn hệ thống JWT Auth/Authorization 2 chế độ (`base.security.*`, xem `BASE_FRAMEWORK_GUIDE.md` mục 10) - **mặc định mọi endpoint chưa khai báo rule đều yêu cầu token hợp lệ** (`anyRequest().authenticated()` trong `SecurityAutoConfiguration#securityFilterChain`).
- Khi tạo endpoint/module mới, **bắt buộc** thêm rule tường minh: `.permitAll()` nếu endpoint cố ý công khai, `.hasRole(...)`/`.hasAnyRole(...)` nếu giới hạn theo vai trò. Không dựa vào hành vi mặc định để suy ra ý định phân quyền.
- **TUYỆT ĐỐI KHÔNG** sửa trực tiếp `SecurityAutoConfiguration#securityFilterChain` trong `base` để thêm rule cho module nghiệp vụ (vi phạm Điều 2, và từ 2026-09-16 không còn cần thiết nữa). Thay vào đó, tạo một `@Component implements SecurityRuleCustomizer` (interface trong `vn.org.thn.app.base.security`) ngay trong package `api` của module đó - xem `vn.org.thn.app.modules.user.api.UserSecurityRules` làm ví dụ mẫu. `securityFilterChain` tự động gom và áp dụng mọi bean `SecurityRuleCustomizer` trước dòng `anyRequest().authenticated()` cuối cùng.
- **TUYỆT ĐỐI KHÔNG** đọc thông tin user hiện tại (cho audit log hay bất kỳ mục đích gì) từ header client tự gửi (`X-User-Id`, `X-Username`...) - đây chính là lỗ hổng Critical #2 đã bị vá; luôn dùng `SecurityContextHolder.getContext().getAuthentication()` hoặc helper `RequestContextFilter.resolveUser(request)` đã có sẵn trong `base`.

### ⚡ Điều 10: Đồng Bộ Tài Liệu Sau Khi Thay Đổi Quy Ước/Kiến Trúc
- Khi thay đổi bất kỳ quy ước, pattern, hay quyết định kiến trúc nào đã được ghi trong `AGENT.md` hoặc `docs/*.md` (ví dụ: đổi cách khai báo phân quyền endpoint, đổi vị trí đặt một loại class, đổi tên interface/class core...), AI Agent **bắt buộc** phải `grep` từ khóa liên quan trên toàn bộ `AGENT.md` + `docs/*.md` để tìm mọi chỗ còn mô tả cách làm cũ, và cập nhật hết trong cùng lượt - không chỉ sửa file "nghĩ tới đầu tiên".
- Việc rà soát này phải làm **ngay sau khi sửa code xong**, không phải "nếu nhớ" hay chờ người dùng nhắc lại.
- Nếu phát hiện tài liệu mô tả một quy ước đã lỗi thời nhưng **không liên quan trực tiếp** tới thay đổi đang làm, phải báo lại cho người dùng biết thay vì tự ý sửa lan man ngoài phạm vi yêu cầu - trừ khi được cho phép mở rộng.
- **Ví dụ thực tế (2026-09-16)**: khi tách rule bảo mật `/public/user/**` ra khỏi `SecurityAutoConfiguration` (xem Điều 9), lượt sửa đầu chỉ cập nhật `AGENT.md` và `BASE_FRAMEWORK_GUIDE.md`, bỏ sót `MICROSERVICE_ARCHITECTURE_GUIDE.md` và `PROMPT_TEMPLATES.md` - khiến 2 tài liệu này tiếp tục dạy sai cách làm cho tới khi rà soát lại riêng.

### ⚡ Điều 11: Bắt Buộc Tạo Tài Liệu Riêng Cho Mỗi Module/Tính Năng Mới
- Khi tạo mới một module nghiệp vụ (hoặc một tính năng lớn trong module có sẵn), AI Agent **bắt buộc** tạo file `docs/<TÊN_MODULE>_API_GUIDE.md`, theo đúng mẫu cấu trúc của `docs/LANGUAGE_API_GUIDE.md`:
  1. Tổng quan: Base URL, định dạng response, yêu cầu xác thực.
  2. Bảng danh sách toàn bộ endpoint (method, path, mô tả, yêu cầu role).
  3. Chi tiết từng endpoint: request/response example thật (JSON cụ thể, không phải placeholder mơ hồ).
  4. Bảng mã lỗi thường gặp của module đó.
- File này viết **sau khi code đã chạy được và test đã pass** - request/response example phải khớp đúng DTO/`ApiResponse` thực tế, không suy đoán.
- Đây là tài liệu bổ sung, không thay thế Swagger (`@Tag`/`@Operation` ở Điều 4 vẫn bắt buộc) - Swagger phục vụ tra cứu nhanh lúc dev, còn file này phục vụ người đọc muốn hiểu module mà không cần mở code hay chạy app.

---

## 🚫 3. Các Anti-Pattern Bắt Buộc Tránh (What NOT To Do)

| ❌ Không Được Làm (Don't) | ✅ Cách Làm Đúng (Do) | Lý Do |
|---|---|---|
| Dùng JPA annotations như `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@JoinColumn` | Sử dụng Custom ORM nhẹ: `@Entity`, `@Table`, `@Column`, `@Id` và JOIN bằng `QueryBuilder` hoặc `CustomMapper`. | Dự án sử dụng Custom ORM siêu nhẹ chạy trên MyBatis template, KHÔNG sử dụng Hibernate/JPA. |
| Tự tạo lại class Response như `ResponseData`, `ResultDTO` | Dùng `ApiResponse<T>` từ `vn.org.thn.app.base.core.response.ApiResponse`. | Chuẩn hóa định dạng JSON đầu ra toàn hệ thống (`code`, `message`, `data`). |
| Trả trực tiếp Entity CSDL ra REST Controller | Chuyển đổi Entity thành DTO Response tại tầng Application Service. | Đảm bảo tính bảo mật, tránh lộ cấu trúc DB và ngăn ngừa circular JSON reference. |
| Nuốt ngoại lệ (silent try-catch) hoặc trả về null/empty khi lỗi | Ném `BusinessException(CommonErrorCode, "Thông báo lỗi")`. | `GlobalExceptionHandler` sẽ tự động bắt và trả về HTTP status + JSON error code rõ ràng. |
| Gọi `update().execute()` hoặc `delete().execute()` không có `where` | Luôn xác định rõ điều kiện `.eq()`, `.in()`, v.v. trước khi gọi `.execute()`. | Ngăn chặn việc vô tình sửa/xóa nhầm toàn bộ dữ liệu trong bảng. |
| Dùng vòng lặp `for` gọi `save()` từng bản ghi khi nạp dữ liệu lớn | Sử dụng `saveAll(list)` để thực hiện Batch Insert/Update. | Tránh overhead kết nối DB và tối ưu hóa thời gian thực thi gấp nhiều lần. |
| Bỏ qua Jakarta Validation trên Request DTO | Khai báo `@NotBlank`, `@Size`, `@Email`... trên DTO và `@Valid` trên Controller. | Ngăn chặn dữ liệu rác, lỗi SQL constraint từ tầng Web và trả về mã `VAL_001` chuẩn. |
| Viết `deleted BOOLEAN` cho CSDL Oracle hoặc SQL Server | Dùng `NUMBER(1) DEFAULT 0` (Oracle) hoặc `BIT DEFAULT 0` (SQL Server). | Oracle và SQL Server không hỗ trợ kiểu dữ liệu BOOLEAN, migration sẽ bị lỗi ngay lập tức. |
| Bỏ quên 5 cột của `BaseEntity` trong câu `CREATE TABLE` | Luôn thêm `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted`. | `EntityParser` tự động quét thuộc tính lớp cha, nếu DB thiếu cột sẽ gây crash câu lệnh INSERT/UPDATE. |
| Tự gán tay các trường audit (`setCreatedAt`, `setUpdatedAt`...) trong Service | Để `save()` và `saveAll()` của framework tự động điền qua cơ chế Auto-Audit của `BaseEntity`. | Tránh mã nguồn thừa thãi (boilerplate), đảm bảo tính nhất quán và bảo vệ dữ liệu audit gốc không bị ghi đè. |
| Viết trực tiếp câu lệnh SQL dạng String trong Controller hoặc Service | Sử dụng `QueryBuilder` hoặc viết Custom MyBatis XML Mapper. | Đảm bảo tính đóng gói, dễ bảo trì và ngăn ngừa SQL Injection. |
| Sửa đổi file `mapper/DynamicSQL.xml` | Tạo file mapper mới `mapper/<Module>CustomMapper.xml`. | `DynamicSQL.xml` là core engine của ORM framework. |
| Tạo endpoint mới mà không khai báo rule phân quyền | Tạo bean `@Component implements SecurityRuleCustomizer` trong package `api` của module, khai báo `.permitAll()` hoặc `.hasRole(...)`/`.hasAnyRole(...)` tường minh cho path mới - xem `UserSecurityRules` làm ví dụ. | Mặc định `anyRequest().authenticated()` sẽ áp dụng - endpoint vẫn chạy nhưng có thể yêu cầu/không yêu cầu đúng quyền như ý định ban đầu. |
| Sửa trực tiếp `base.security.SecurityAutoConfiguration#securityFilterChain` để thêm rule cho module nghiệp vụ | Tạo `SecurityRuleCustomizer` riêng trong module đó (xem Điều 9). | Vi phạm Điều 2 (không sửa `base`); mỗi module mới không còn phải đụng vào core framework. |
| Đọc thông tin user từ header client tự gửi (`X-User-Id`, `X-Username`...) cho audit log/phân quyền | Dùng `SecurityContextHolder.getContext().getAuthentication()` hoặc `RequestContextFilter.resolveUser(request)`. | Header client tự gửi không qua xác thực, dễ bị giả mạo (đây chính là Critical #2 đã bị vá 2026-09-13). |
| Sửa 1 tài liệu rồi coi như xong, không rà các file docs khác | `grep` từ khóa liên quan trên toàn bộ `AGENT.md` + `docs/*.md` trước khi báo cáo hoàn tất (xem Điều 10). | Tài liệu tham chiếu chéo nhiều file - sót 1 chỗ là đủ để dạy sai quy ước ở lần sau. |
| Tạo module mới xong mà không viết `docs/<Module>_API_GUIDE.md` (xem Điều 11) | Viết tài liệu API riêng cho module theo mẫu `LANGUAGE_API_GUIDE.md` ngay sau khi code chạy được. | Người đọc sau này (kể cả AI Agent khác) phải hiểu module mà không cần mở code hay chạy thử app. |

---

## 🗺️ 4. Bản Đồ Thư Mục Dự Án (Directory Sitemap)

```text
/Volumes/Data/04.MyProject/java-project/app-service/
│
├── AGENT.md                               # <-- BẠN ĐANG Ở ĐÂY (File quy chuẩn cho AI Agent)
│
├── docs/                                  # TẤT CẢ TÀI LIỆU QUY CHUẨN CỦA DỰ ÁN
│   ├── BASE_FRAMEWORK_GUIDE.md            # Hướng dẫn chi tiết Base ORM, QueryBuilder, Controller, i18n
│   ├── MICROSERVICE_ARCHITECTURE_GUIDE.md # Quy chuẩn cấu trúc Microservice 4 tầng Clean Architecture
│   ├── LANGUAGE_API_GUIDE.md              # Tài liệu API quản lý đa ngôn ngữ
│   └── USER_API_GUIDE.md                  # Tài liệu API module User (mẫu cho Điều 11 - mỗi module mới có 1 file riêng)
│
├── config/                                # Cấu hình môi trường bên ngoài
│   ├── secrets.yaml                       # Chứa username/password CSDL thật (gitignored)
│   └── secrets.yaml.example               # File mẫu cấu trúc secrets
│
├── database/                              # SCRIPT MIGRATION DATABASE (FLYWAY)
│   ├── sqlite/                            # Scripts cho SQLite (V1__init.sql, V2__init_user.sql...)
│   ├── postgresql/                        # Scripts cho PostgreSQL
│   ├── mysql/                             # Scripts cho MySQL
│   ├── oracle/                            # Scripts cho Oracle
│   └── sqlserver/                         # Scripts cho SQL Server
│
├── mapper/                                # MYBATIS XML MAPPERS
│   ├── DynamicSQL.xml                     # Core Dynamic SQL Engine của Custom ORM (KHÔNG SỬA)
│   └── <Module>CustomMapper.xml           # File XML Mapper custom cho các câu SQL phức tạp
│
├── src/main/java/vn/org/thn/app/
│   ├── AppApplication.java                # Spring Boot Entrypoint
│   │
│   ├── base/                              # FRAMEWORK CORE (KHÔNG SỬA TRỪ KHI ĐƯỢC YÊU CẦU)
│   │   ├── config/                        # Auto-configurations, Hikari, Dialects
│   │   ├── core/                          # ApiResponse, BaseEntity, BaseDTO, BusinessException
│   │   ├── i18n/                          # Module dịch thuật đa ngôn ngữ
│   │   ├── persistence/                   # ORM Engine (QueryBuilder, Executors, Dialects)
│   │   ├── security/                      # JWT Auth/Authorization 2 chế độ (xem BASE_FRAMEWORK_GUIDE.md mục 10)
│   │   ├── util/                          # JsonUtils, DateUtils, ValidationUtils
│   │   └── web/                           # BaseCtl, GlobalExceptionHandler, ContextFilter
│   │
│   └── modules/                           # TẤT CẢ MODULE NGHIỆP VỤ NẰM Ở ĐÂY
│       └── <module_name>/                 # Ví dụ: user, order, product, payment...
│           ├── api/                       # Presentation Layer (Controllers, DTOs)
│           ├── application/               # Application Layer (Services, @Transactional)
│           ├── domain/                    # Domain Layer (Entities kế thừa BaseEntity)
│           └── infrastructure/            # Infrastructure Layer (Repositories, FeignClients, Custom Mappers)
│
└── src/test/java/vn/org/thn/app/
    ├── base/                              # Unit test cho Framework Core
    └── modules/                           # Unit test cho các Module nghiệp vụ
```

---

## 🔄 5. Quy Trình 8 Bước Thực Thi Nhiệm Vụ (AI Task Execution Protocol)

Khi nhận yêu cầu tạo mới hoặc sửa đổi một module nghiệp vụ (Ví dụ: Module `Product`), AI Agent phải thực hiện tuần tự theo 8 bước sau:

1. **Bước 1 (Đọc tài liệu & kiểm tra)**: Kiểm tra thông tin bảng CSDL hiện có tại `database/<db_type>/`.
2. **Bước 2 (Migration SQL)**: Thêm file `database/<db_type>/V<N>__init_<module>.sql`.
3. **Bước 3 (Domain Entity)**: Tạo class Entity tại `modules/<module>/domain/entity/<Module>Entity.java` kế thừa `BaseEntity` với annotation `@Entity` và `@Table`.
4. **Bước 4 (Infrastructure Repository)**: Tạo class Repository tại `modules/<module>/infrastructure/<Module>Repository.java` kế thừa `BaseRepositoryImpl<Entity, Long>`.
5. **Bước 5 (API DTOs)**: Tạo các DTO Request và Response tại `modules/<module>/api/dto/`.
6. **Bước 6 (Application Service)**: Tạo Service tại `modules/<module>/application/<Module>Service.java`, gắn `@Service`, sử dụng `QueryBuilder` và `@Transactional`.
7. **Bước 7 (REST Controller)**: Tạo Controller tại `modules/<module>/api/<Module>Ctl.java` kế thừa `BaseCtl`, gắn `@RestController`, `@RequestMapping`, `@Tag`, `@Operation`.
8. **Bước 8 (Unit Test & Verification)**: Tạo Unit Test tại `src/test/java/vn/org/thn/app/modules/<module>/<Module>ServiceTest.java` và chạy lệnh Gradle test để xác minh.

---

## 🛠️ 6. Bảng Lệnh Thao Tác Hệ Thống (Useful Commands Reference)

| Mục Đích | Lệnh Bắt Buộc | Ghi Chú |
|---|---|---|
| **Chạy Toàn Bộ Unit Test** | `export JAVA_HOME=/usr/local/Cellar/openjdk@21/21.0.6/libexec/openjdk.jdk/Contents/Home && ./gradlew test --no-daemon` | Chạy trước khi kết thúc lượt làm việc của Agent. |
| **Build File Executable JAR** | `export JAVA_HOME=/usr/local/Cellar/openjdk@21/21.0.6/libexec/openjdk.jdk/Contents/Home && ./gradlew bootJar --no-daemon` | File xuất ra tại `build/libs/app-service-1.0.0.jar`. |
| **Xem Swagger UI** | `http://localhost:8080/api.html` | Đảm bảo app đang chạy trên port 8080. |
| **Xem OpenAPI Docs JSON** | `http://localhost:8080/doc` | File định dạng OpenAPI 3.0 JSON. |

---

> 🎯 **Ghi nhớ**: "Luôn đọc tài liệu trước - Tuân thủ 11 điều luật vàng - Tránh anti-patterns - Đồng bộ tài liệu & viết doc module mới - Chạy kiểm thử thành công trước khi báo cáo kết quả."
