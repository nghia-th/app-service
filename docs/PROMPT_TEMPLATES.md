# 💬 Mẫu Câu Ra Lệnh Cho AI Agent (`PROMPT_TEMPLATES.md`)

> **Bộ Mẫu Câu Lệnh (Prompt Templates)** dành cho người dùng để ra lệnh cho **AI Coding Agent** phát triển module mới, thêm tính năng, chỉnh sửa CSDL, fix bug, và viết unit test trên dự án `app-service`.

---

## 📋 Mục Lục
1. [Tạo Module Nghiệp Vụ Mới (New Module)](#1-tạo-module-nghiệp-vụ-mới-new-module)
2. [Phát Triển Tính Năng / API Mới (Feature Development)](#2-phát-triển-tính-năng--api-mới-feature-development)
3. [Cập Nhật CSDL & Migration (Database Migration)](#3-cập-nhật-csdl--migration-database-migration)
4. [Viết SQL Phức Tạp & Custom MyBatis Mapper](#4-viết-sql-phức-tạp--custom-mybatis-mapper)
5. [Sửa Lỗi & Tối Ưu Hóa (Bug Fixing & Refactoring)](#5-sửa-lỗi--tối-ưu-hóa-bug-fixing--refactoring)
6. [Viết Unit Test & Kiểm Thử (Testing)](#6-viết-unit-test--kiểm-thử-testing)

---

## 1. Tạo Module Nghiệp Vụ Mới (New Module)

### Mẫu 1.1: Tạo Module CRUD Cơ Bản
```text
Hãy tạo cho anh module <Tên_Module> (ví dụ: Product) theo đúng quy chuẩn AGENT.md và MICROSERVICE_ARCHITECTURE_GUIDE.md.
- Tên bảng: tbl_<tên_module> (ví dụ: tbl_product)
- Các trường:
  + id (Long, Auto-increment)
  + productName (String, Bắt buộc)
  + price (Double)
  + status (String: ACTIVE, INACTIVE)
- Cần đầy đủ Flyway migration đồng bộ cho 5 loại DB (SQLite, PostgreSQL, MySQL, SQL Server, Oracle có đủ 5 cột audit), Entity kế thừa BaseEntity (tận dụng Auto-Audit tự động của framework, không set ngày giờ thủ công), Repository, DTOs (gắn Jakarta Validation annotations), Service (@Transactional), Controller kế thừa BaseCtl (có @Valid, Swagger) và Unit Test.
```

### Mẫu 1.2: Tạo Module Nghiệp Vụ Phức Tạp (Nhiều trạng thái & Quan hệ)
```text
Hãy tạo cho anh module Order quản lý đơn hàng theo đúng chuẩn AGENT.md.
- Tên bảng: tbl_order
- Các trường:
  + id (Long, Auto-increment)
  + userId (Long, Khóa ngoại tham chiếu tbl_user)
  + orderCode (String, Tự sinh dạng ORD-YYYYMMDD-XXXX)
  + totalAmount (Double)
  + status (PENDING, PROCESSING, COMPLETED, CANCELLED)
  + note (String)
- Luồng xử lý trong OrderService:
  + API tạo đơn: Bắt buộc dùng @Transactional, tự động tính tổng tiền và sinh orderCode. Tận dụng Auto-Audit của BaseEntity khi gọi save().
  + API chuyển trạng thái đơn hàng: Kiểm tra logic hợp lệ (ví dụ: Đơn đã COMPLETED thì không thể CANCELLED).
- Đi kèm đầy đủ Flyway migration SQL (cho cả 5 loại DB: SQLite, PostgreSQL, MySQL, SQL Server, Oracle có đầy đủ 5 cột audit BaseEntity), Controller, Swagger docs và Unit Test suite.
```

---

## 2. Phát Triển Tính Năng / API Mới (Feature Development)

### Mẫu 2.1: Thêm API Phân Trang & Tìm Kiếm Theo Điều Kiện
```text
Trong module Product, hãy viết thêm API tìm kiếm sản phẩm nâng cao:
- Endpoint: GET /public/product/search
- Request params: productName (like), minPrice, maxPrice, status, page, size
- Sử dụng QueryBuilder của Base Framework để lọc dữ liệu type-safe.
- Trả về ApiResponse<PageResponse<ProductResponse>>.
```

### Mẫu 2.2: Thêm API Xử Lý Logic Nghiệp Vụ Nhiều Bảng (Transaction)
```text
Trong module Order, hãy viết API hủy đơn hàng (POST /public/order/{id}/cancel):
- Đầu vào: id đơn hàng và lý do hủy (reason).
- Logic trong OrderService (@Transactional):
  1. Kiểm tra đơn hàng có tồn tại không (nếu không quăng BusinessException NOT_FOUND).
  2. Kiểm tra trạng thái đơn: Nếu đã COMPLETED thì quăng BusinessException INVALID_STATUS.
  3. Cập nhật status = CANCELLED và ghi chú lý do hủy.
```

### Mẫu 2.3: Thêm API Tìm Kiếm Đa Từ Bất Kể Thứ Tự & Không Dấu (Smart Search)
```text
Trong module <Tên_Module> (ví dụ: User), hãy xây dựng API tìm kiếm thông minh không phân biệt thứ tự từ và không dấu tiếng Việt:
- Endpoint: GET /public/<module>/smart-search (keyword, page, size)
- Quy trình triển khai:
  1. Thêm cột <field>_unaccent và đánh index trong CSDL qua Flyway migration cho 5 DB (sqlite, postgresql, mysql, sqlserver, oracle).
  2. Trong Entity, khai báo trường unaccent tương ứng với annotation @Unaccent:
     @Column(name = "full_name_unaccent")
     @Unaccent(from = "fullName")
     private String fullNameUnaccent;
     (Base Framework sẽ tự động đồng bộ giá trị không dấu khi insert/update).
  3. Trong Service, sử dụng QueryBuilder với likeAnyOrderUnaccent:
     query.likeAnyOrderUnaccent(UserEntity::getFullNameUnaccent, keyword);
     (hoặc dùng likeAnyOrder nếu tìm kiếm có dấu nhưng không phụ thuộc thứ tự từ).
  4. Viết Unit Test kiểm thử các trường hợp hoán vị từ ("Nghĩa Trương Hiếu" -> "Trương Hiếu Nghĩa"), không dấu ("truong hieu nghia"), hoa/thường.
```

---

## 3. Cập Nhật CSDL & Migration (Database Migration)

### Mẫu 3.1: Thêm Trường Mới Vào Bảng Đã Tồn Tại
```text
Anh cần thêm trường phone_number (String, độ dài 20) vào bảng tbl_user:
1. Tạo file Flyway migration mới đồng bộ cho 5 loại database: database/<db_type>/V3__add_phone_number_to_user.sql (sqlite, postgresql, mysql, sqlserver, oracle).
2. Cập nhật UserEntity (gắn @Column(name = "phone_number")).
3. Cập nhật UserCreateRequest, UserUpdateRequest, UserResponse (kèm validation).
4. Cập nhật UserService và các bài Unit Test liên quan.
```

### Mẫu 3.2: Tạo Bảng Mới Hoặc Bảng Liên Kết
```text
Hãy tạo bảng liên kết tbl_user_role (user_id, role_id, created_at):
1. Viết script migration Flyway V4__create_tbl_user_role.sql.
2. Tạo UserRoleEntity tương ứng.
3. Viết UserRoleRepository kế thừa BaseRepositoryImpl.
```

---

## 4. Viết SQL Phức Tạp & Custom MyBatis Mapper

### Mẫu 4.1: Báo Cáo / Thống Kê Đa Bảng (Custom Mapper XML)
```text
Anh cần một API báo cáo thống kê doanh thu theo từng User trong module User:
- Hãy làm theo quy trình 3 bước tại BASE_FRAMEWORK_GUIDE.md:
  1. Tạo XML Mapper: mapper/UserCustomMapper.xml với câu SQL JOIN giữa tbl_user và tbl_order, nhóm GROUP BY user_id, lọc theo khoảng thời gian (fromDate, toDate).
  2. Tạo Interface: modules/user/infrastructure/mapper/UserCustomMapper.java (@Mapper).
  3. Gọi qua mapper(UserCustomMapper.class) trong UserRepository.
- Thêm endpoint GET /public/user/revenue-report tại UserCtl.
```

---

## 5. Sửa Lỗi & Tối Ưu Hóa (Bug Fixing & Refactoring)

### Mẫu 5.1: Sửa Lỗi Logic Hoặc Exception
```text
Hiện tại khi gọi API tạo User với username đã tồn tại trong CSDL, ứng dụng đang quăng lỗi SQL constraint thay vì báo lỗi thân thiện.
- Hãy kiểm tra UserService#createUser.
- Trước khi save, dùng userRepository.query().eq(UserEntity::getUsername, username).exists() để kiểm tra.
- Nếu đã tồn tại, ném BusinessException(CommonErrorCode.DUPLICATE, "Tên đăng nhập đã tồn tại trong hệ thống").
- Thêm Unit Test cho trường hợp này và chạy ./gradlew test để xác minh.
```

### Mẫu 5.2: Refactor Code Cho Đúng Quy Chuẩn
```text
Hãy rà soát lại Controller UserCtl:
- Đảm bảo tất cả các hàm đều kế thừa BaseCtl và dùng ok(...) hoặc fail(...).
- Đảm bảo đã gắn chú thích Swagger @Tag và @Operation cho từng endpoint.
- Loại bỏ các câu lệnh try-catch thừa để GlobalExceptionHandler tự xử lý.
```

---

## 6. Viết Unit Test & Kiểm Thử (Testing)

### Mẫu 6.1: Viết Unit Test Cho Module Hiện Có
```text
Hãy viết bài Unit Test đầy đủ cho UserService tại src/test/java/vn/org/thn/app/modules/user/UserServiceTest.java:
- Test case 1: Tạo user thành công.
- Test case 2: Tạo user thất bại do trùng username.
- Test case 3: Phân trang danh sách user.
- Chạy lệnh test và đảm bảo 100% test cases PASSED:
  export JAVA_HOME=/usr/local/Cellar/openjdk@21/21.0.6/libexec/openjdk.jdk/Contents/Home && ./gradlew test --no-daemon
```

---

> 💡 **Mẹo:** Anh có thể copy trực tiếp các câu lệnh mẫu trên, thay thế tên module/trường dữ liệu và gửi thẳng cho AI Agent!
