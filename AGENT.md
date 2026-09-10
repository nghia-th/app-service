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
   *(Đọc file này để biết cách dùng `QueryBuilder`, `Save/Upsert`, `UpdateBuilder`, `DeleteBuilder`, Custom MyBatis Mapper, `BaseCtl`, `ApiResponse`, `@Transactional` và Flyway Migration).*

3. 📙 **Tài liệu API Đa Ngôn Ngữ (i18n)**:
   👉 [LANGUAGE_API_GUIDE.md](file:///Volumes/Data/04.MyProject/java-project/app-service/docs/LANGUAGE_API_GUIDE.md)
   *(Đọc file này khi cần xử lý hoặc gọi API liên quan đến đa ngôn ngữ và dịch thuật).*

4. 💬 **Bộ Mẫu Câu Lệnh Cho Người Dùng (Prompt Templates)**:
   👉 [PROMPT_TEMPLATES.md](file:///Volumes/Data/04.MyProject/java-project/app-service/docs/PROMPT_TEMPLATES.md)
   *(Tham khảo file này để hiểu các dạng câu lệnh/yêu cầu mẫu từ người dùng).*

---

## 🚨 2. Các Điều Luật Vàng Cho AI Agent (Golden Execution Rules)

Tất cả các AI Agent (Antigravity, Claude, ChatGPT, Cursor, Copilot...) **bắt buộc tuân thủ 100%** 8 điều luật sau mà không có ngoại lệ:

### ⚡ Điều 1: Định Vị Package Chuẩn Microservice
- Mọi class nghiệp vụ mới **phải nằm trong `vn.org.thn.app.modules.<module_name>.*`** (Ví dụ: `vn.org.thn.app.modules.user`, `vn.org.thn.app.modules.order`).
- **TUYỆT ĐỐI KHÔNG** tạo package lẻ ở ngoài root package hoặc trong package legacy `demo`.

### ⚡ Điều 2: Tái Sử Dụng Base Framework Core
- Không tự tạo lại `ApiResponse`, `PageResponse`, `JsonUtils`, `BaseException`, `CommonErrorCode`, hay `GlobalExceptionHandler`.
- Bắt buộc `import` và tái sử dụng từ `vn.org.thn.app.base.*`.
- **TUYỆT ĐỐI KHÔNG** tự ý sửa đổi code trong package `vn.org.thn.app.base.*` trừ khi có yêu cầu nâng cấp framework từ người dùng.

### ⚡ Điều 3: Cú Pháp QueryBuilder Type-Safe
- Khi truy vấn dữ liệu CSDL qua Repository, **bắt buộc dùng Method Reference** `eq(Entity::getFieldName, value)` thay vì truyền tên cột dạng String cứng.
- Ví dụ: `userRepository.query().eq(UserEntity::getUsername, "admin").one();`

### ⚡ Điều 4: Chuẩn Hóa Controller & Swagger Docs
- Mọi REST Controller **phải kế thừa `BaseCtl`** và trả về kết quả qua `ok(...)` hoặc `fail(...)`.
- Bắt buộc gắn đầy đủ chú thích OpenAPI Swagger: `@Tag(name = "...")` và `@Operation(summary = "...")`.

### ⚡ Điều 5: Quản Lý CSDL Bằng Flyway Migration
- Mọi thay đổi bảng hoặc cột CSDL phải có file migration SQL tương ứng trong `database/<db_type>/V<N>__<description>.sql` (Ví dụ: `database/sqlite/V2__init_user.sql`).
- Chú ý tên file phải chứa 2 dấu gạch dưới `__`.

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
  export JAVA_HOME=/usr/local/Cellar/openjdk@21/21.0.6/libexec/openjdk.jdk/Contents/Home && ./gradlew test --no-daemon
  ```

---

## 🚫 3. Các Anti-Pattern Bắt Buộc Tránh (What NOT To Do)

| ❌ Không Được Làm (Don't) | ✅ Cách Làm Đúng (Do) | Lý Do |
|---|---|---|
| Dùng JPA annotations như `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@JoinColumn` | Sử dụng Custom ORM nhẹ: `@Entity`, `@Table`, `@Column`, `@Id` và JOIN bằng `QueryBuilder` hoặc `CustomMapper`. | Dự án sử dụng Custom ORM siêu nhẹ chạy trên MyBatis template, KHÔNG sử dụng Hibernate/JPA. |
| Tự tạo lại class Response như `ResponseData`, `ResultDTO` | Dùng `ApiResponse<T>` từ `vn.org.thn.app.base.core.response.ApiResponse`. | Chuẩn hóa định dạng JSON đầu ra toàn hệ thống (`code`, `message`, `data`). |
| Trả trực tiếp Entity CSDL ra REST Controller | Chuyển đổi Entity thành DTO Response tại tầng Application Service. | Đảm bảo tính bảo mật, tránh lộ cấu trúc DB và ngăn ngừa circular JSON reference. |
| Nuốt ngoại lệ (silent try-catch) hoặc trả về null/empty khi lỗi | Ném `BusinessException(CommonErrorCode, "Thông báo lỗi")`. | `GlobalExceptionHandler` sẽ tự động bắt và trả về HTTP status + JSON error code rõ ràng. |
| Viết trực tiếp câu lệnh SQL dạng String trong Controller hoặc Service | Sử dụng `QueryBuilder` hoặc viết Custom MyBatis XML Mapper. | Đảm bảo tính đóng gói, dễ bảo trì và ngăn ngừa SQL Injection. |
| Sửa đổi file `mapper/DynamicSQL.xml` | Tạo file mapper mới `mapper/<Module>CustomMapper.xml`. | `DynamicSQL.xml` là core engine của ORM framework. |

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
│   └── LANGUAGE_API_GUIDE.md              # Tài liệu API quản lý đa ngôn ngữ
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

> 🎯 **Ghi nhớ**: "Luôn đọc tài liệu trước - Tuân thủ 8 điều luật vàng - Tránh anti-patterns - Chạy kiểm thử thành công trước khi báo cáo kết quả."
