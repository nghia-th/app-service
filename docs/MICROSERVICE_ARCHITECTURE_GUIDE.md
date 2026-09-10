# Tài Liệu Quy Chuẩn Kiến Trúc Microservice (`app-service`)
> **Dành cho Lập Trình Viên & AI Coding Agent**

Tài liệu này định nghĩa **Quy chuẩn cấu trúc thư mục, nội dung lưu trữ của từng thư mục, phân tầng Clean Architecture và Quy trình phát triển module Microservice** cho dự án `app-service`. Tất cả lập trình viên và AI Agent khi phát triển tính năng mới **bắt buộc tuân thủ 100% cấu trúc này**.

---

## 📋 Mục Lục
1. [Tổng Quan Kiến Trúc Thư Mục (Folder Layout)](#1-tổng-quan-kiến-trúc-thư-mục-folder-layout)
2. [Chi Tiết Nội Dung & Chức Năng Của Từng Thư Mục (Directory Reference)](#2-chi-tiết-nội-dung--chức-năng-của-từng-thư-mục-directory-reference)
   - [2.1 Phân Hệ Gốc Framework (`vn.org.thn.app.base`)](#21-phân-hệ-gốc-framework-vnorgthnappbase)
   - [2.2 Phân Hệ Module Nghiệp Vụ (`vn.org.thn.app.modules`)](#22-phân-hệ-module-nghiệp-vụ-vnorgthnappmodules)
   - [2.3 Thư Mục Dùng Chung & Tài Nguyên Dự Án (Resources & Configs)](#23-thư-mục-dùng-chung--tài-nguyên-dự-án-resources--configs)
3. [Chi Tiết 4 Tầng Trong 1 Module (`modules/<module-name>`)](#3-chi-tiết-4-tầng-trong-1-module-modulesmodule-name)
   - [3.1 Tầng API (Presentation Layer)](#31-tầng-api-presentation-layer)
   - [3.2 Tầng Application (Application Layer)](#32-tầng-application-application-layer)
   - [3.3 Tầng Domain (Domain Layer)](#33-tầng-domain-domain-layer)
   - [3.4 Tầng Infrastructure (Infrastructure Layer)](#34-tầng-infrastructure-infrastructure-layer)
4. [Quy Trình 7 Bước Phát Triển Module Mới (Developer Workflow)](#4-quy-trình-7-bước-phát-triển-module-mới-developer-workflow)
5. [Chỉ Thị Dành Cho AI Coding Agent (AI Agent Execution Rules)](#5-chỉ-thị-dành-cho-ai-coding-agent-ai-agent-execution-rules)

---

## 1. Tổng Quan Kiến Trúc Thư Mục (Folder Layout)

Mã nguồn được tổ chức theo mô hình **Modular Monolith / Clean Microservice Architecture**:
- Tách biệt tuyệt đối bộ khung **Framework Core (`base`)** với các **Module Nghiệp Vụ (`modules`)**.

```text
src/main/java/vn/org/thn/app/
│
├── AppApplication.java                   # Main Spring Boot Application Entrypoint
│
├── base/                                 # -------------------------------------------------------------
│   ├── config/                           # Base Auto-Configurations, Hikari, Dialects
│   ├── core/                             # ApiResponse, BaseEntity, BaseDTO, CommonErrorCode
│   ├── i18n/                             # Module quản lý ngôn ngữ (LanguageApi, LanguageService, Translate)
│   ├── persistence/                      # Custom ORM Engine (QueryBuilder, Executors, Dialects, Annotations)
│   ├── util/                             # JsonUtils, DateUtils, ValidationUtils, StringUtils
│   └── web/                              # BaseCtl, GlobalExceptionHandler, RequestContextFilter
│
├── modules/                              # -------------------------------------------------------------
│   │                                     # TẤT CẢ MODULE NGHIỆP VỤ NẰM Ở ĐÂY
│   └── <module_name>/                    # Ví dụ: user, order, product, payment...
│       │
│       ├── api/                          # 1. PRESENTATION LAYER
│       │   ├── dto/                      #    Request DTOs (Create/Update) & Response DTOs
│       │   └── <Entity>Ctl.java          #    REST Controllers (kế thừa BaseCtl)
│       │
│       ├── application/                  # 2. APPLICATION LAYER
│       │   └── <Entity>Service.java      #    Use Case & Orchestration Services
│       │
│       ├── domain/                       # 3. DOMAIN LAYER (CORE BUSINESS)
│       │   ├── entity/                   #    Entities (kế thừa BaseEntity, khai báo @Entity, @Table)
│       │   ├── model/                    #    Domain Value Objects, Enums
│       │   └── repository/               #    (Optional) Domain Repository Interfaces
│       │
│       └── infrastructure/               # 4. INFRASTRUCTURE LAYER
│           ├── client/                   #    Feign / HTTP RestClients gọi Microservices khác
│           ├── security/                 #    Cấu hình Security riêng cho Module
│           └── <Entity>Repository.java   #    Repository Implementation (kế thừa BaseRepositoryImpl)
│
└── shared/                               # -------------------------------------------------------------
    ├── constants/                        # Global Business Constants & Shared Enums
    └── exception/                        # Module Business Error Codes
```

---

## 2. Chi Tiết Nội Dung & Chức Năng Của Từng Thư Mục (Directory Reference)

### 2.1 Phân Hệ Gốc Framework (`vn.org.thn.app.base`)

| Thư mục / Package | Loại thông tin lưu trữ | Các class tiêu biểu |
|---|---|---|
| `base/config/` | Cấu hình Spring Boot cho DataSource, HikariCP, và đăng ký Bean Dialect theo loại CSDL active. | `DataSourceConfig`, `DatabaseProperties`, `DialectConfig` |
| `base/core/constant/` | Chứa các hằng số hệ thống khung (ví dụ Header tên `X-Request-Id`, `token`). | `CommonConstants` |
| `base/core/dto/` | Chứa DTO gốc và phân trang envelope dùng chung toàn bộ ứng dụng. | `BaseDTO`, `PageRequest`, `PageResponse` |
| `base/core/entity/` | Class Entity gốc chứa các trường thông tin cơ bản (ví dụ ngày tạo, người tạo). | `BaseEntity` |
| `base/core/exception/` | Định nghĩa hệ thống ngoại lệ framework, ErrorCode mẫu và BusinessException. | `BaseException`, `BusinessException`, `ErrorCode`, `CommonErrorCode` |
| `base/core/response/` | Chuẩn hóa cấu trúc Response JSON đầu ra cho tất cả các API (`code`, `message`, `data`). | `ApiResponse<T>` |
| `base/i18n/api/` | REST API quản lý xem, thêm, sửa, xóa, và xuất zip dữ liệu dịch thuật đa ngôn ngữ. | `LanguageApi`, `LanguageRequest` |
| `base/i18n/config/` | Cấu hình tự động nạp các file JSON ngôn ngữ khi ứng dụng khởi chạy. | `LanguageAutoConfiguration` |
| `base/i18n/domain/` | Model lưu trữ bộ nhớ và Entity CSDL đại diện cho bảng `translate`. | `Language`, `Translate`, `TranslateId` |
| `base/i18n/repository/` | Repository thao tác dữ liệu dịch thuật trên CSDL. | `TranslateRepository` |
| `base/i18n/service/` | Business Logic đồng bộ dịch thuật 2 chiều giữa CSDL, JSON file và Bộ nhớ đệm. | `LanguageService` |
| `base/persistence/annotation/` | Các Custom Annotation ORM định nghĩa thông tin bảng và cột CSDL. | `@Entity`, `@Table`, `@Column`, `@Id`, `@GeneratedValue`, `@Transient` |
| `base/persistence/datasource/` | Các Lớp tương tác Provider kết nối dành riêng cho từng loại CSDL. | `DatabaseProvider`, `PostgreSqlProvider`, `MySqlProvider`, `SqliteProvider`, `OracleProvider`, `SqlServerProvider` |
| `base/persistence/dialect/` | Các lớp sinh cú pháp phân trang (LIMIT/OFFSET) và sinh ID cho từng loại CSDL. | `SqlDialect`, `PostgreSqlDialect`, `MySqlDialect`, `SqliteDialect`, `OracleDialect`, `SqlServerDialect`, `DialectFactory` |
| `base/persistence/executor/` | Bộ thực thi các câu lệnh SQL trực tiếp qua MyBatis session (Insert, Update, Delete, Batch, Native SQL). | `BaseExecutor`, `QueryExecutor`, `InsertExecutor`, `UpdateExecutor`, `DeleteExecutor`, `BatchInsertExecutor`, `NativeQueryExecutor`, `RowMapper` |
| `base/persistence/lambda/` | Trích xuất tên thuộc tính Entity từ Lambda Method Reference (`Entity::getGetter`). | `SFunction`, `LambdaFieldResolver` |
| `base/persistence/metadata/` | Bóc tách annotation phản xạ (reflection) và lưu cache thông tin Entity. | `EntityInfo`, `EntityParser`, `EntityCache` |
| `base/persistence/migration/` | Tự động quét và thực thi các script Flyway SQL migration khi ứng dụng boot. | `DatabaseInitializer`, `FlywayConfig`, `DirectoryInitializer` |
| `base/persistence/logging/` | Ghi log câu lệnh SQL phát sinh và đo thời gian thực thi. | `SqlLogger`, `SqlLogLevel`, `SqlLogResult` |
| `base/persistence/query/` | Bộ xây dựng câu lệnh SQL Fluent Type-Safe DSL cho `SELECT`, `UPDATE`, `DELETE`. | `QueryBuilder`, `UpdateBuilder`, `DeleteBuilder`, `BaseConditionBuilder`, `QueryCondition`, `QueryOrder` |
| `base/persistence/repository/` | Class Repository trừu tượng gốc chứa toàn bộ các hàm thao tác CSDL. | `BaseRepository`, `BaseRepositoryImpl` |
| `base/util/` | Thư viện xử lý tiện ích hệ thống (JSON, String, Date, Validation). | `JsonUtils`, `StringUtils`, `DateUtils`, `ValidationUtils` |
| `base/web/config/` | Đăng ký tự động các Filter và Interceptor cho Web MVC. | `BaseWebAutoConfiguration` |
| `base/web/controller/` | Controller gốc cung cấp các hàm trả về kết quả chuẩn (`ok()`, `fail()`). | `BaseCtl` |
| `base/web/exception/` | Bộ bắt lỗi ngoại lệ tập trung toàn hệ thống (Global Exception Handler). | `GlobalExceptionHandler` |
| `base/web/filter/` | Filter gắn Request ID vào MDC và log body request an toàn. | `RequestContextFilter`, `CachedBodyHttpServletRequest` |

---

### 2.2 Phân Hệ Module Nghiệp Vụ (`vn.org.thn.app.modules`)

Mỗi thư mục con trong `modules/` đại diện cho một **Microservice Domain hoàn chỉnh** (Ví dụ: `user`, `order`, `product`):

| Thư mục Module | Loại thông tin lưu trữ | Ví dụ File |
|---|---|---|
| `modules/<name>/api/` | REST Controllers của module và các DTO Request / Response. | `UserCtl.java` |
| `modules/<name>/api/dto/` | Chứa DTO nhận dữ liệu đầu vào (Create/Update Request) và DTO trả về. | `UserCreateRequest.java`, `UserResponse.java` |
| `modules/<name>/application/` | Chứa Use Cases và Application Services điều phối nghiệp vụ. | `UserService.java` |
| `modules/<name>/domain/entity/` | Chứa các Entity đại diện cho bảng CSDL của module. | `UserEntity.java` |
| `modules/<name>/domain/model/` | Chứa các Value Objects, Enums nghiệp vụ của module. | `UserRoleEnum.java` |
| `modules/<name>/infrastructure/` | Chứa Repository triển khai thực tế CSDL và các HTTP Clients kết nối service ngoài. | `UserRepository.java` |
| `modules/<name>/infrastructure/client/` | Chứa Feign Client hoặc RestClient gọi API sang các Microservice khác. | `OrderClient.java` |
| `modules/<name>/infrastructure/security/` | Cấu hình bảo mật hoặc phân quyền riêng cho module. | `UserSecurityConfig.java` |

---

### 2.3 Thư Mục Dùng Chung & Tài Nguyên Dự Án (Resources & Configs)

| Thư mục ngoài `src/main/java` | Loại thông tin lưu trữ | Nội dung chi tiết |
|---|---|---|
| `config/` | Cấu hình bí mật bên ngoài ứng dụng. | `secrets.yaml` (chứa username/password CSDL thật, được gitignore), `secrets.yaml.example` |
| `database/` | Chứa các Script SQL Migration cho Flyway phân theo loại CSDL. | `sqlite/V1__init.sql`, `postgresql/V1__init.sql`, `mysql/V1__init.sql`, `oracle/`, `sqlserver/` |
| `docs/` | Chứa toàn bộ tài liệu quy chuẩn kiến trúc và API của dự án. | `MICROSERVICE_ARCHITECTURE_GUIDE.md`, `BASE_FRAMEWORK_GUIDE.md`, `LANGUAGE_API_GUIDE.md` |
| `html/` | Chứa build tĩnh của giao diện React served bởi Spring Boot. | `html/build/languages/*.json` |
| `lang/` | Chứa các file dữ liệu dịch thuật đa ngôn ngữ JSON. | `vi.json`, `en.json` |
| `mapper/` | Chứa file MyBatis XML cho truy vấn SQL động. | `DynamicSQL.xml` |
| `src/main/resources/` | Cấu hình mặc định của Spring Boot. | `application.yaml`, `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |

---

## 3. Chi Tiết 4 Tầng Trong 1 Module (`modules/<module-name>`)

### 3.1 Tầng API (Presentation Layer)
- **Vị trí**: `modules/<module_name>/api/`
- **Nhiệm vụ**: Tiếp nhận Request từ bên ngoài, kiểm tra Validation sơ bộ, gọi Application Service và trả về Response.
- **Quy tắc**:
  - Controller **phải kế thừa `BaseCtl`** và gắn các Swagger Annotation (`@Tag`, `@Operation`, `@ApiResponses`).
  - Dữ liệu trả về qua `ok(...)` hoặc `fail(...)` của `BaseCtl` (đóng gói trong `ApiResponse<T>`).
  - Không viết business logic trực tiếp trong Controller.

### 3.2 Tầng Application (Application Layer)
- **Vị trí**: `modules/<module_name>/application/`
- **Nhiệm vụ**: Điều phối luồng nghiệp vụ (Orchestration), quản lý Transaction (`@Transactional`), gọi Repository.
- **Quy tắc**:
  - Tách biệt Request DTO với Entity CSDL (không trả trực tiếp Entity ra API, phải convert sang Response DTO).
  - Sử dụng `QueryBuilder` từ Repository để phân trang và tìm kiếm dữ liệu.

### 3.3 Tầng Domain (Domain Layer)
- **Vị trí**: `modules/<module_name>/domain/entity/`
- **Nhiệm vụ**: Định nghĩa cấu trúc bảng CSDL và quy tắc nghiệp vụ cốt lõi.
- **Quy tắc**:
  - Class Entity kế thừa `BaseEntity`.
  - Gắn `@Entity`, `@Table(name = "tbl_<module>")`.
  - Khóa chính `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`.

### 3.4 Tầng Infrastructure (Infrastructure Layer)
- **Vị trí**: `modules/<module_name>/infrastructure/`
- **Nhiệm vụ**: Thực thi giao tiếp dữ liệu (Database access, Gọi HTTP REST Client sang service khác).
- **Quy tắc**:
  - Repository **bắt buộc kế thừa `BaseRepositoryImpl<Entity, Long>`**.

---

## 4. Quy Trình 7 Bước Phát Triển Module Mới (Developer Workflow)

Khi cần phát triển một Microservice Module mới (Ví dụ: `Product`):

1. **Bước 1 (Domain)**: Tạo `ProductEntity.java` trong `modules/product/domain/entity/`.
2. **Bước 2 (Infrastructure)**: Tạo `ProductRepository.java` trong `modules/product/infrastructure/` kế thừa `BaseRepositoryImpl<ProductEntity, Long>`.
3. **Bước 3 (API DTOs)**: Tạo `ProductCreateRequest`, `ProductUpdateRequest`, `ProductResponse` trong `modules/product/api/dto/`.
4. **Bước 4 (Application)**: Tạo `ProductService.java` trong `modules/product/application/` xử lý CRUD & `QueryBuilder`.
5. **Bước 5 (API Controller)**: Tạo `ProductCtl.java` trong `modules/product/api/` kế thừa `BaseCtl`.
6. **Bước 6 (Flyway SQL)**: Thêm file tạo bảng `database/<db_type>/V<N>__init_<module>.sql`.
7. **Bước 7 (Unit Test)**: Thêm bài test tại `src/test/java/vn/org/thn/app/modules/product/ProductServiceTest.java`.

---

## 5. Chỉ Thị Dành Cho AI Coding Agent (AI Agent Execution Rules)

> 🤖 **Khi được yêu cầu tạo mới hoặc sửa đổi một module nghiệp vụ, AI Agent BẮT BUỘC thực hiện đúng các điều luật sau:**
>
> 1. **Định Vị Package**: Mọi class nghiệp vụ mới **phải nằm trong `vn.org.thn.app.modules.<module_name>.*`**. Tuyệt đối không tạo package lẻ ở ngoài root hoặc trong package `demo`.
> 2. **Tái Sử Dụng Base Core**: Không tự tạo lại `ApiResponse`, `PageResponse`, `JsonUtils`, `BaseException`, hay `GlobalExceptionHandler`. Bắt buộc `import` từ `vn.org.thn.app.base.*`.
> 3. **Cú Pháp QueryBuilder**: Khi thực hiện truy vấn DB, sử dụng Method Reference dạng `eq(Entity::getFieldName, value)` thay vì truyền String cứng.
> 4. **Tài Liệu Swagger**: Mọi REST Controller mới khởi tạo phải có đầy đủ chú thích `@Tag`, `@Operation`, và `@ApiResponses`.
> 5. **Kiểm Thử Độc Lập**: Mỗi module tạo mới phải đi kèm file Unit Test tương ứng trong `src/test/java/vn/org/thn/app/modules/<module_name>/`.
