# Hướng Dẫn Sử Dụng Base Framework (`app-service`)
> **Dành cho Lập Trình Viên & AI Coding Agent**

Tài liệu này cung cấp đầy đủ hướng dẫn kiến trúc, quy tắc lập trình, và cú pháp mẫu để phát triển các tính năng mới trên bộ khung **Base Framework** (`vn.org.thn.app.base`).

---

## 📋 Mục Lục
1. [Tổng Quan Kiến Trúc (Overview)](#1-tổng-quan-kiến-trúc-overview)
2. [Phân Hệ Persistence / Custom ORM](#2-phân-hệ-persistence--custom-orm)
   - [2.1 Khai Báo Entity](#21-khai-báo-entity)
   - [2.2 Khai Báo Repository](#22-khai-báo-repository)
   - [2.3 Truy Vấn Dữ Liệu Type-Safe (QueryBuilder)](#23-truy-vấn-dữ-liệu-type-safe-querybuilder)
   - [2.4 Thêm / Cập Nhật Dữ Liệu (Save & Upsert)](#24-thêm--cập-nhật-dữ-liệu-save--upsert)
   - [2.5 Cập Nhật Theo Điều Kiện (UpdateBuilder)](#25-cập-nhật-theo-điều-kiện-updatebuilder)
   - [2.6 Xóa Dữ Liệu (DeleteBuilder)](#26-xóa-dữ-liệu-deletebuilder)
   - [2.7 Native Query & MyBatis Mapper](#27-native-query--mybatis-mapper)
3. [Phân Hệ Web Core & Controller](#3-phân-hệ-web-core--controller)
   - [3.1 Controller Kế Thừa `BaseCtl`](#31-controller-kế-thừa-basectl)
   - [3.2 Chuẩn Hóa Response `ApiResponse<T>`](#32-chuẩn-hóa-response-apiresponset)
   - [3.3 Xử Lý Lỗi Ngoại Lệ (BusinessException & GlobalExceptionHandler)](#33-xử-lý-lỗi-ngoại-lệ-businessexception--globalexceptionhandler)
4. [Phân Hệ Đa Ngôn Ngữ i18n](#4-phân-hệ-đa-ngôn-ngữ-i18n)
5. [Cấu Hình Đa CSDL & Flyway Migration](#5-cấu-hình-đa-csdl--flyway-migration)
6. [Quản Lý Giao Dịch Transaction Management](#6-quản-lý-giao-dịch-transaction-management)
7. [Hướng Dẫn Viết Unit Test](#7-hướng-dẫn-viết-unit-test)
8. [Swagger UI & Cấu Hình Bí Mật (Secrets)](#8-swagger-ui--cấu-hình-bí-mật-secrets)
9. [Mẫu Hướng Dẫn Phát Triển Tính Năng Mới (Recipe for AI Agent)](#9-mẫu-hướng-dẫn-phát-triển-tính-năng-mới-recipe-for-ai-agent)

---

## 1. Tổng Quan Kiến Trúc (Overview)

Framework được thiết kế theo tư duy **nhẹ (lightweight), tốc độ khởi động nhanh**, thay thế JPA/Hibernate cồng kềnh bằng custom ORM chạy trên MyBatis dynamic SQL template.

### Cấu trúc package chính:
```text
vn.org.thn.app.base
├── config               # Cấu hình DataSource, Database Dialects
├── core                 # DTO, Entity gốc, Exception, ApiResponse
├── i18n                 # Module đa ngôn ngữ (API, Service, Domain)
├── persistence          # Custom ORM Engine (Annotations, DSL Query, Executors, Dialects)
├── util                 # Utilities (JsonUtils, StringUtils, DateUtils, ValidationUtils)
└── web                  # Controller gốc (BaseCtl), Filters, GlobalExceptionHandler
```

---

## 2. Phân Hệ Persistence / Custom ORM

### 2.1 Khai Báo Entity
Entity đại diện cho 1 bảng trong CSDL. Sử dụng các annotation thuộc package `vn.org.thn.app.base.persistence.annotation.*`:

```java
package vn.org.thn.app.modules.product.domain.entity;

import lombok.Getter;
import lombok.Setter;
import vn.org.thn.app.base.core.entity.BaseEntity;
import vn.org.thn.app.base.persistence.annotation.*;

@Getter
@Setter
@Entity
@Table(name = "tbl_product")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "price")
    private Double price;

    @Column(name = "status")
    private String status;

    @Transient // Trường này không lưu vào DB
    private String extraInfo;
}
```

> 💡 **Quy tắc cho AI Agent / Dev**:
> - Bắt buộc có `@Entity`.
> - `@Table(name = "...")`: Tên bảng CSDL. Nếu bỏ trống tên, mặc định dùng tên class.
> - `@Id`: Đánh dấu khóa chính.
> - `@GeneratedValue(strategy = GenerationType.IDENTITY)`: Tự động tăng (Auto-increment).
> - Nếu không khai báo `@Column(name = "...")`, tên cột sẽ tự động chuyển từ CamelCase sang snake_case (ví dụ: `productName` $\rightarrow$ `product_name`).

---

### 2.2 Khai Báo Repository
Mọi Repository quản lý Entity phải kế thừa `BaseRepositoryImpl<T, ID>` và gắn `@Repository`:

```java
package vn.org.thn.app.modules.product.infrastructure;

import org.springframework.stereotype.Repository;
import vn.org.thn.app.base.persistence.repository.BaseRepositoryImpl;
import vn.org.thn.app.modules.product.domain.entity.Product;

@Repository
public class ProductRepository extends BaseRepositoryImpl<Product, Long> {
    // Tự động có sẵn tất cả các hàm: save, saveAll, findById, findAll, count, existsById, deleteById, query(), update(), delete()
}
```

---

### 2.3 Truy Vấn Dữ Liệu Type-Safe (`QueryBuilder`)
Khởi tạo builder truy vấn bằng `repository.query()`. Sử dụng **Method Reference** (`Entity::getGetter`) để type-safe tuyệt đối:

#### a) Các Phép Lọc Điều Kiện (Filtering)
```java
// Lấy danh sách sản phẩm có status = 'ACTIVE' và price >= 100
List<Product> products = productRepository.query()
    .eq(Product::getStatus, "ACTIVE")
    .ge(Product::getPrice, 100.0)
    .orderByDesc(Product::getId)
    .list();

// Lấy 1 bản ghi đầu tiên (hoặc null nếu không tìm thấy)
Product product = productRepository.query()
    .eq(Product::getId, 10L)
    .one();

// Lấy 1 bản ghi hoặc ném exception nếu không có
Product product = productRepository.query()
    .eq(Product::getId, 10L)
    .oneOrThrow();
```

#### b) Danh Sách Các Toán Tử Hỗ Trợ:
| Phương thức | Ví dụ | Ý nghĩa SQL |
|---|---|---|
| `eq(Field, val)` | `.eq(Product::getStatus, "ACTIVE")` | `status = 'ACTIVE'` |
| `ne(Field, val)` | `.ne(Product::getStatus, "DELETED")` | `status <> 'DELETED'` |
| `gt(Field, val)` | `.gt(Product::getPrice, 50.0)` | `price > 50.0` |
| `ge(Field, val)` | `.ge(Product::getPrice, 50.0)` | `price >= 50.0` |
| `lt(Field, val)` | `.lt(Product::getPrice, 200.0)` | `price < 200.0` |
| `le(Field, val)` | `.le(Product::getPrice, 200.0)` | `price <= 200.0` |
| `like(Field, val)` | `.like(Product::getProductName, "iPhone")` | `product_name LIKE '%iPhone%'` |
| `startsWith(Field, val)` | `.startsWith(Product::getProductName, "Mac")` | `product_name LIKE 'Mac%'` |
| `endsWith(Field, val)` | `.endsWith(Product::getProductName, "Pro")` | `product_name LIKE '%Pro'` |
| `in(Field, Collection)` | `.in(Product::getId, List.of(1L, 2L, 3L))` | `id IN (1, 2, 3)` |
| `notIn(Field, Collection)`| `.notIn(Product::getStatus, List.of("A", "B"))` | `status NOT IN ('A', 'B')` |
| `between(Field, min, max)`| `.between(Product::getPrice, 10.0, 50.0)` | `price BETWEEN 10.0 AND 50.0` |
| `isNull(Field)` | `.isNull(Product::getStatus)` | `status IS NULL` |
| `isNotNull(Field)` | `.isNotNull(Product::getStatus)` | `status IS NOT NULL` |

#### c) Nhóm Điều Kiện Phức Tạp (`and`, `or`)
```java
// WHERE status = 'ACTIVE' AND (price > 100 OR product_name LIKE '%Special%')
List<Product> list = productRepository.query()
    .eq(Product::getStatus, "ACTIVE")
    .and(sub -> sub
        .gt(Product::getPrice, 100.0)
        .orEq(Product::getProductName, "Special")
    )
    .list();
```

#### d) Phân Trang (`page`)
```java
// Phân trang (Trang 1, Kích thước 20 bản ghi)
PageResponse<Product> pageResult = productRepository.query()
    .eq(Product::getStatus, "ACTIVE")
    .orderByDesc(Product::getId)
    .page(1, 20)
    .pageResult();

// Dữ liệu trả về gồm: pageResult.getData(), pageResult.getTotal(), pageResult.getPage(), pageResult.getSize()
```

#### e) Hàm Gom Nhóm (Aggregations)
```java
long total = productRepository.query().eq(Product::getStatus, "ACTIVE").count();
Double maxPrice = productRepository.query().max(Product::getPrice);
Double avgPrice = productRepository.query().avg(Product::getPrice);
```

#### f) Tìm Kiếm Đa Từ Bất Kể Thứ Tự (Order-Independent Search)
Framework hỗ trợ 2 phương thức tìm kiếm chuỗi linh hoạt khi người dùng nhập từ khóa theo bất kỳ thứ tự nào:

1. **`likeAnyOrder(Entity::getField, keyword)`**:
   - Tự động tách từ khóa theo khoảng trắng và sinh điều kiện `AND` kết hợp `LOWER()` trên cột gốc (không phân biệt hoa/thường).
   - Ví dụ: Dữ liệu trong CSDL là `"Trương Hiếu Nghĩa"`. Người dùng gõ `"HIếu Nghĩa Trương"`, `"Nghĩa Trương Hiếu"`, hay `"Trương Nghĩa"` đều tìm thấy chính xác!
   ```java
   List<UserEntity> list = userRepository.query()
       .likeAnyOrder(UserEntity::getFullName, "HIếu Nghĩa Trương")
       .list();
   ```

2. **`likeAnyOrderUnaccent(Entity::getUnaccentField, keyword)`**:
   - Tự động chuyển đổi từ khóa người dùng sang không dấu, tách từ và sinh điều kiện `AND` trên cột unaccent.
   - Hỗ trợ người dùng gõ tiếng Việt **CÓ DẤU** lẫn **KHÔNG DẤU** mà vẫn tìm ra kết quả:
   ```java
   // Người dùng gõ "hieu nghia truong" hoặc "truong nghia"
   List<UserEntity> list = userRepository.query()
       .likeAnyOrderUnaccent(UserEntity::getFullNameUnaccent, "hieu nghia truong")
       .list();
   ```

---

### 2.4 Thêm / Cập Nhật Dữ Liệu (`Save` & `Upsert`)
Hàm `save()` là entry-point duy nhất tự động quyết định `INSERT` hoặc `UPDATE`:

```java
// 1. Tạo mới (ID null -> Tự động INSERT & ghi ngược ID tự tăng vào object)
Product newProduct = new Product();
newProduct.setProductName("Laptop Dell");
newProduct.setPrice(1500.0);
newProduct.setStatus("ACTIVE");
productRepository.save(newProduct); 
System.out.println("ID vừa sinh: " + newProduct.getId());

// 2. Cập nhật (ID có giá trị và đã tồn tại trong DB -> Tự động UPDATE)
newProduct.setPrice(1400.0);
productRepository.save(newProduct);

// 3. Thêm/Sửa hàng loạt trong 1 Transaction
List<Product> savedList = productRepository.saveAll(productList);
```

---

### 2.5 Cập Nhật Theo Điều Kiện (`UpdateBuilder`)
Sử dụng `repository.update()` để cập nhật danh sách các trường theo điều kiện `WHERE` (bắt buộc phải có điều kiện `WHERE`):

```java
int affectedRows = productRepository.update()
    .set(Product::getStatus, "INACTIVE")
    .set(Product::getPrice, 0.0)
    .eq(Product::getStatus, "OUT_OF_STOCK")
    .execute();
```

---

### 2.6 Xóa Dữ Liệu (`DeleteBuilder`)
```java
// Xóa theo ID
productRepository.deleteById(10L);

// Xóa theo điều kiện
int deletedCount = productRepository.delete()
    .eq(Product::getStatus, "DELETED")
    .execute();
```

> [!CAUTION]
> **Quy tắc an toàn dữ liệu (Safety First):**
> Cả `UpdateBuilder` và `DeleteBuilder` **bắt buộc phải có ít nhất một điều kiện WHERE**. 
> Nếu gọi `.execute()` mà không có điều kiện nào, hệ thống sẽ ngay lập tức ném ra ngoại lệ `IllegalStateException("Cannot execute UPDATE/DELETE without WHERE condition")` nhằm ngăn ngừa việc sửa/xóa nhầm toàn bộ dữ liệu của bảng. Tuyệt đối không bọc nuốt lỗi này bằng silent try-catch.

---

### 2.7 Native Query & MyBatis Mapper

Khi cần thực thi các câu lệnh SQL phức tạp (JOIN nhiều bảng, báo cáo, thống kê, hoặc SQL động nâng cao), Framework hỗ trợ 2 cách tiếp cận:

#### Cách 1: Sử dụng `nativeQuery(...)` trực tiếp trong Repository
Phù hợp cho các câu truy vấn SQL ngắn hoặc trung bình:

```java
@Repository
public class ProductRepository extends BaseRepositoryImpl<Product, Long> {

    // Native query trả về List DTO
    public List<ProductCustomDTO> findCustomProducts(String category) {
        String sql = "SELECT p.id, p.product_name AS productName, c.category_name AS categoryName " +
                     "FROM tbl_product p JOIN tbl_category c ON p.category_id = c.id " +
                     "WHERE c.name = #{category}";
        return nativeQuery(sql, ProductCustomDTO.class, Map.of("category", category));
    }
}
```

#### Cách 2: Sử dụng Custom MyBatis XML Mapper (Quy trình 3 Bước)
Phù hợp cho các câu SQL rất dài, báo cáo phức tạp, hoặc sử dụng tính năng MyBatis Dynamic SQL XML (như `<if>`, `<choose>`, `<foreach>`).

*Lưu ý: Thư mục `mapper/` ở root dự án đã được cấu hình tự động nạp qua `mybatis.mapper-locations: file:./mapper/*.xml`.*

- **Bước 1: Tạo file XML Mapper trong thư mục `mapper/`** (ví dụ: `mapper/UserCustomMapper.xml`):
  ```xml
  <?xml version="1.0" encoding="UTF-8" ?>
  <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
          "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
  <mapper namespace="vn.org.thn.app.modules.user.infrastructure.mapper.UserCustomMapper">

      <select id="findUserReport" resultType="vn.org.thn.app.modules.user.api.dto.UserReportDTO">
          SELECT u.id, u.username, u.email, COUNT(o.id) AS totalOrders
          FROM tbl_user u
          LEFT JOIN tbl_order o ON u.id = o.user_id
          <where>
              <if test="status != null and status != ''">
                  AND u.status = #{status}
              </if>
          </where>
          GROUP BY u.id, u.username, u.email
          ORDER BY totalOrders DESC
      </select>

  </mapper>
  ```

- **Bước 2: Tạo Java Mapper Interface với annotation `@Mapper`** (đặt trong package `infrastructure` của module):
  ```java
  package vn.org.thn.app.modules.user.infrastructure.mapper;

  import org.apache.ibatis.annotations.Mapper;
  import org.apache.ibatis.annotations.Param;
  import vn.org.thn.app.modules.user.api.dto.UserReportDTO;
  import java.util.List;

  @Mapper
  public interface UserCustomMapper {
      List<UserReportDTO> findUserReport(@Param("status") String status);
  }
  ```

- **Bước 3: Gọi Custom Mapper từ Repository** qua helper method `mapper(...)` kế thừa từ `BaseRepositoryImpl`:
  ```java
  package vn.org.thn.app.modules.user.infrastructure;

  import org.springframework.stereotype.Repository;
  import vn.org.thn.app.base.persistence.repository.BaseRepositoryImpl;
  import vn.org.thn.app.modules.user.domain.entity.UserEntity;
  import vn.org.thn.app.modules.user.infrastructure.mapper.UserCustomMapper;
  import vn.org.thn.app.modules.user.api.dto.UserReportDTO;
  import java.util.List;

  @Repository
  public class UserRepository extends BaseRepositoryImpl<UserEntity, Long> {

      public List<UserReportDTO> getUserReport(String status) {
          // Gọi custom MyBatis mapper thông qua helper method mapper()
          return mapper(UserCustomMapper.class).findUserReport(status);
      }
  }
  ```

---

### 2.8 Tự Động Hóa Chuẩn Hóa Cột Không Dấu Với `@Unaccent`
Khi một bảng cần cột lưu chuỗi không dấu phục vụ tìm kiếm tiếng Việt toàn diện, khai báo annotation `@Unaccent(from = "sourceFieldName")` trên trường đích:

```java
package vn.org.thn.app.modules.user.domain.entity;

import lombok.Data;
import vn.org.thn.app.base.core.entity.BaseEntity;
import vn.org.thn.app.base.persistence.annotation.*;

@Data
@Entity
@Table(name = "tbl_user")
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Unaccent(from = "fullName")
    @Column(name = "full_name_unaccent")
    private String fullNameUnaccent;
}
```

- **Nguyên lý hoạt động**:
  - Khi gọi `repository.save(user)` (tạo mới hoặc cập nhật) hoặc `repository.saveAll(userList)`:
  - Framework sẽ tự động đọc giá trị từ `fullName` $\rightarrow$ gọi `StringUtils.toUnaccent()` $\rightarrow$ tự động gán vào `fullNameUnaccent` trước khi lưu vào CSDL.
  - Lập trình viên **không cần viết code thủ công** để set giá trị cho trường unaccent.

---

### 2.9 Chuẩn Hóa `BaseEntity` & Cơ Chế Tự Động Hóa Audit (Auto-Audit)

Mọi Entity bảng nghiệp vụ trong dự án bắt buộc kế thừa lớp trừu tượng `BaseEntity`:

```java
@Data
public abstract class BaseEntity {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private boolean deleted = false;
}
```

#### a) Cơ Chế Hoạt Động Của Auto-Audit
Khi gọi `repository.save(entity)` hoặc `repository.saveAll(entities)`, `InsertExecutor` và `BatchInsertExecutor` tự động quản lý các trường audit:
1. **Khi INSERT**:
   - `createdAt`: Tự động điền `LocalDateTime.now()` (nếu đang null).
   - `updatedAt`: Tự động gán bằng `createdAt` (nếu đang null).
   - `createdBy`: Lấy từ `UserContext.getCurrentUser()`. Nếu chưa có user trong context, tự động fallback về `"system"`.
   - `updatedBy`: Gán bằng `createdBy`.
   - `deleted`: Mặc định là `false`.
2. **Khi UPDATE**:
   - `updatedAt`: Tự động cập nhật thành thời điểm hiện tại `LocalDateTime.now()`.
   - `updatedBy`: Tự động cập nhật thành user hiện tại từ `UserContext`.
   - **Bảo toàn vết tạo lập ban đầu**: Các cột `created_at` và `created_by` được tự động loại bỏ khỏi câu lệnh `UPDATE ... SET ...` để không bao giờ bị ghi đè hay mất dữ liệu khởi tạo.

#### b) Quản Lý Ngữ Cảnh Người Dùng (`UserContext`)
- `UserContext` quản lý danh tính người dùng qua `ThreadLocal<String>`.
- `RequestContextFilter` tự động trích xuất các header `X-User-Id`, `X-Username`, hoặc `username` từ HTTP request và nạp vào `UserContext.setCurrentUser(...)`.
- Khi kết thúc request, `RequestContextFilter` dọn dẹp an toàn tại `finally { UserContext.clear(); }`.
- Trong Service hoặc background job, lập trình viên hoàn toàn không cần gọi `setCreatedAt(...)` hay `setUpdatedAt(...)` thủ công.

---

## 3. Phân Hệ Web Core & Controller

### 3.1 Controller Kế Thừa `BaseCtl`
Mọi REST Controller nên kế thừa `BaseCtl` để sử dụng các helper response:

```java
package vn.org.thn.app.modules.product.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.org.thn.app.base.core.dto.page.PageResponse;
import vn.org.thn.app.base.core.response.ApiResponse;
import vn.org.thn.app.base.web.controller.BaseCtl;
import vn.org.thn.app.modules.product.domain.entity.Product;
import vn.org.thn.app.modules.product.infrastructure.ProductRepository;

@Tag(name = "Product Management API")
@RestController
@RequestMapping("/public/product")
public class ProductCtl extends BaseCtl {

    @Autowired
    private ProductRepository productRepository;

    @Operation(summary = "Lấy danh sách sản phẩm phân trang")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<Product>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageResponse<Product> result = productRepository.query()
                .orderByDesc(Product::getId)
                .page(page, size)
                .pageResult();
                
        return ok(result); // Trả về ApiResponse.success(result)
    }

    @Operation(summary = "Tạo mới sản phẩm")
    @PostMapping
    public ResponseEntity<ApiResponse<Product>> create(@RequestBody Product product) {
        Product saved = productRepository.save(product);
        return ok(saved);
    }
}
```

### 3.2 Chuẩn Hóa Response `ApiResponse<T>`
Tất cả API tự động trả về định dạng JSON chuẩn:
```json
{
  "code": "00",
  "message": "Success",
  "data": { ... }
}
```

### 3.3 Xử Lý Lỗi Ngoại Lệ (`BusinessException`)
Khi ném ra `BusinessException` hoặc `BaseException`, `GlobalExceptionHandler` sẽ tự động bắt và trả về HTTP Error tương ứng kèm Error Code:

```java
if (product == null) {
    throw new BusinessException(CommonErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm");
}
```

### 3.4 Xác Thực Dữ Liệu Đầu Vào (Bean Validation - JSR-380)
Hệ thống sử dụng chuẩn **Jakarta Bean Validation (JSR-380)** kết hợp với `GlobalExceptionHandler`:
- **Tại Request DTO**: Khai báo các annotation kiểm tra dữ liệu như `@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@Pattern`, `@Min`, `@Max` kèm `message` rõ ràng.
- **Tại Controller**: Bắt buộc gắn `@Valid` trước `@RequestBody`.
- **Xử lý ngoại lệ**: Khi dữ liệu không thỏa mãn, Spring sẽ ném `MethodArgumentNotValidException`. `GlobalExceptionHandler` sẽ tự động bắt và trả về `ApiResponse` có `code: "VAL_001"` kèm chi tiết lỗi validation.

Ví dụ:
```java
public class UserCreateRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
}

@PostMapping
public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserCreateRequest request) {
    return ok(userService.createUser(request));
}
```

### 3.5 Ghi Log Request An Toàn (`RequestContextFilter`)
`RequestContextFilter` tự động quản lý vòng đời HTTP request:
- **MDC Tracking**: Tự động sinh hoặc kế thừa `X-Request-Id` (UUID) và đưa vào Logback MDC để theo dõi vết log xuyên suốt.
- **Tự động Masking**: Tự động nhận diện và che giấu các trường nhạy cảm trong JSON body (như `password`, `accessToken`, `token`, `secret`, `authorization`) trước khi ghi log.
- **Bỏ qua ghi body file nhị phân**: Tự động phát hiện và bỏ qua việc cache/ghi body đối với các request tải file hoặc dữ liệu nhị phân (như `/export`, `multipart/form-data`) để tránh tràn bộ nhớ log.

---

## 4. Phân Hệ Đa Ngôn Ngữ i18n

Dự án có sẵn service quản lý dịch đa ngôn ngữ đồng bộ giữa DB và JSON file (`LanguageService`).
- **Khởi tạo & Đồng bộ Ngôn ngữ thông minh (Startup Batch Ingestion)**: Khi ứng dụng khởi chạy, `LanguageService.loadLanguage()` sẽ:
  1. Đọc toàn bộ các file `lang/*.json` vào bộ nhớ đệm (Cache).
  2. Tải danh sách các cặp khóa ngôn ngữ hiện có trong CSDL lên bộ nhớ đệm để đối soát (Smart Diffing).
  3. Chỉ trích xuất các bản ghi dịch thực sự còn thiếu trong CSDL và thực hiện lưu hàng loạt (**Batch Insert bằng `saveAll()`**).
  - *Lợi ích*: Tránh lỗi trùng lặp khóa chính (PK Collision), tăng tốc độ khởi động ứng dụng vượt bậc và không ghi đè dữ liệu dịch tùy biến trên CSDL.
- **Tải danh sách ngôn ngữ**: `languageService.loadLanguage()`
- **Cập nhật key dịch**: `languageService.updateLanguage(request)`

---

## 5. Cấu Hình Đa CSDL & Flyway Migration

### 5.1 Cấu Hình Môi Trường & Database Theo Spring Profiles
Hệ thống phân tách cấu hình theo mô hình đa môi trường (Spring Boot Profiles):
- **`application.yaml`**: Cấu hình chung toàn hệ thống (Multipart, Swagger Docs, format log, i18n) và kích hoạt profile mặc định:
  ```yaml
  spring:
    profiles:
      active: dev
  ```
- **`application-dev.yaml`** (Môi trường Phát triển - Dev):
  - Database: `SQLITE` (`file-name: dev_app.db`), `dbPrefix: dev`
  - Log SQL: `FULL` (hiển thị chi tiết toàn bộ SQL và tham số bind)
  - Log level: `vn.org.thn.app: DEBUG`
- **`application-test.yaml`** (Môi trường Kiểm thử tự động / CI):
  - Database: `SQLITE` (`file-name: test_app.db`), `dbPrefix: test`
  - Log SQL: `BASIC`, port: `0` (random port)
- **`application-staging.yaml`** (Môi trường Thử nghiệm / UAT / Pre-prod):
  - Database: `POSTGRESQL` / `MYSQL`, `dbPrefix: staging`
  - Log SQL: `BASIC`, Pool size: `10`
- **`application-prod.yaml`** (Môi trường Vận hành thực tế - Production):
  - Database: `POSTGRESQL` / `MYSQL` / `ORACLE`, `dbPrefix: prod`
  - Log SQL: `OFF` (tắt SQL log để tối đa hóa hiệu năng và bảo mật)
  - Pool size: `20` (tối ưu tải cao)
  - Swagger UI: Tắt công khai trên Production (`enabled: false`)

### 5.2 Quy Trình Quản Lý Script Migration (Flyway)
Mọi thay đổi cấu trúc CSDL (tạo bảng, thêm cột, index) đều được tự động thực thi bởi Flyway khi ứng dụng khởi chạy (`DatabaseInitializer`).

- **Thư mục lưu trữ**: Đặt trong `database/<db_type>/` (Hỗ trợ 5 loại: `sqlite/`, `postgresql/`, `mysql/`, `oracle/`, `sqlserver/`).
- **Quy tắc đặt tên file**: `V<Version>__<Mo_Ta_Cau_Truc>.sql` (chú ý **2 dấu gạch dưới `__`**).
- **Ví dụ**:
  - `database/sqlite/V1__init.sql`
  - `database/sqlite/V2__init_user.sql`
  - `database/postgresql/V2__init_user.sql`
  - `database/mysql/V2__init_user.sql`
  - `database/oracle/V2__init_user.sql`
  - `database/sqlserver/V2__init_user.sql`

### 5.3 Chuẩn Bảng CSDL Kế Thừa `BaseEntity` & Quy Tắc DDL 5 Loại Database

#### a) Cơ chế ORM Reflection đối với `BaseEntity`
- Lớp `BaseEntity` chứa 5 trường audit: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, và `deleted` (default `false`).
- Trong `EntityParser.java`, framework sử dụng `FieldUtils.getAllFieldsList(clazz)` để quét **toàn bộ các trường của cả Entity con lẫn lớp cha `BaseEntity`**.
- Do đó, ORM sẽ tự động coi 5 cột này luôn tồn tại trong các câu lệnh `INSERT`, `UPDATE`, `SELECT`. **Nếu câu lệnh `CREATE TABLE` thiếu bất kỳ cột nào, hệ thống sẽ ném lỗi `SQLException: column does not exist`**.
- Ngoài ra, `BaseEntity` **cố tình không chứa trường `id`** (để Entity con linh hoạt chọn kiểu ID). Do đó, câu lệnh DDL của Entity con phải tự định nghĩa cột khóa chính phù hợp với từng hệ CSDL.

#### b) Bảng Tra Cứu Kiểu Dữ Liệu Chuẩn (DDL Type Mapping Cheat Sheet)

| Cột trong Entity | Java Type | SQLite | PostgreSQL | MySQL | SQL Server | Oracle |
|---|---|---|---|---|---|---|
| `id` (Khóa chính) | `Long` | `INTEGER PRIMARY KEY AUTOINCREMENT` | `BIGSERIAL PRIMARY KEY` | `BIGINT AUTO_INCREMENT PRIMARY KEY` | `BIGINT IDENTITY(1,1) PRIMARY KEY` | `NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY` |
| `created_at` | `LocalDateTime` | `TEXT` hoặc `TIMESTAMP` | `TIMESTAMP` | `DATETIME` | `DATETIME2` | `TIMESTAMP` |
| `updated_at` | `LocalDateTime` | `TEXT` hoặc `TIMESTAMP` | `TIMESTAMP` | `DATETIME` | `DATETIME2` | `TIMESTAMP` |
| `created_by` | `String` | `TEXT` | `VARCHAR(50)` | `VARCHAR(50)` | `NVARCHAR(50)` | `VARCHAR2(50)` |
| `updated_by` | `String` | `TEXT` | `VARCHAR(50)` | `VARCHAR(50)` | `NVARCHAR(50)` | `VARCHAR2(50)` |
| `deleted` | `boolean` (false) | `BOOLEAN DEFAULT 0` | `BOOLEAN DEFAULT FALSE` | `TINYINT(1) DEFAULT 0` | `BIT DEFAULT 0` | `NUMBER(1) DEFAULT 0` |

> [!WARNING]
> **Lưu ý đặc thù hệ CSDL:**
> 1. **Oracle** KHÔNG có kiểu `BOOLEAN` $\rightarrow$ Bắt buộc dùng `NUMBER(1) DEFAULT 0`.
> 2. **SQL Server** KHÔNG có kiểu `BOOLEAN` $\rightarrow$ Bắt buộc dùng `BIT DEFAULT 0`.
> 3. **PostgreSQL** dùng `TIMESTAMP` thay vì `DATETIME`. Khóa chính tự tăng dùng `BIGSERIAL` hoặc `GENERATED ALWAYS AS IDENTITY`.
> 4. **Oracle** dùng `GENERATED BY DEFAULT AS IDENTITY` (không phải `GENERATED ALWAYS`) cho `id`: `InsertExecutor` hỗ trợ nhánh "manual identity insert" (caller tự cấp giá trị id, ví dụ khi import dữ liệu có sẵn khóa chính) — nhánh này chỉ insert được giá trị id tường minh nếu cột là `BY DEFAULT`; `GENERATED ALWAYS` sẽ từ chối insert đó trên Oracle.

#### c) Bộ Mẫu DDL Tham Khảo Chuẩn Cho 5 Loại Database (Ví dụ bảng `tbl_product`)

**1. SQLite** (`database/sqlite/V<N>__init_product.sql`):
```sql
CREATE TABLE IF NOT EXISTS tbl_product
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    product_name TEXT NOT NULL,
    price        REAL,
    status       TEXT DEFAULT 'ACTIVE',
    created_at   TEXT,
    updated_at   TEXT,
    created_by   TEXT,
    updated_by   TEXT,
    deleted      BOOLEAN DEFAULT 0
);
```

**2. PostgreSQL** (`database/postgresql/V<N>__init_product.sql`):
```sql
CREATE TABLE IF NOT EXISTS tbl_product
(
    id           BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    price        NUMERIC(15, 2),
    status       VARCHAR(50) DEFAULT 'ACTIVE',
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(50),
    updated_by   VARCHAR(50),
    deleted      BOOLEAN DEFAULT FALSE
);
```

**3. MySQL** (`database/mysql/V<N>__init_product.sql`):
```sql
CREATE TABLE IF NOT EXISTS tbl_product
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    price        DECIMAL(15, 2),
    status       VARCHAR(50) DEFAULT 'ACTIVE',
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   VARCHAR(50),
    updated_by   VARCHAR(50),
    deleted      TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**4. SQL Server** (`database/sqlserver/V<N>__init_product.sql`):
```sql
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='tbl_product' AND xtype='U')
CREATE TABLE tbl_product
(
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_name NVARCHAR(255) NOT NULL,
    price        DECIMAL(15, 2),
    status       NVARCHAR(50) DEFAULT 'ACTIVE',
    created_at   DATETIME2,
    updated_at   DATETIME2,
    created_by   NVARCHAR(50),
    updated_by   NVARCHAR(50),
    deleted      BIT DEFAULT 0
);
```

**5. Oracle** (`database/oracle/V<N>__init_product.sql`):
```sql
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'TBL_PRODUCT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE tbl_product
        (
            id           NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
            product_name VARCHAR2(255) NOT NULL,
            price        NUMBER(15, 2),
            status       VARCHAR2(50) DEFAULT ''ACTIVE'',
            created_at   TIMESTAMP,
            updated_at   TIMESTAMP,
            created_by   VARCHAR2(50),
            updated_by   VARCHAR2(50),
            deleted      NUMBER(1) DEFAULT 0
        )';
    END IF;
END;
/
```

---

## 6. Quản Lý Giao Dịch (Transaction Management)

Đối với các Use Case nghiệp vụ tại tầng Application Service có nhiều thao tác ghi CSDL (`save`, `update`, `delete`), bắt buộc sử dụng annotation `@Transactional(rollbackFor = Exception.class)` để đảm bảo tính toàn vẹn dữ liệu:

```java
package vn.org.thn.app.modules.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.app.modules.user.infrastructure.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createUserWithOrder(...) {
        // Nếu bất kỳ thao tác nào quăng Exception, toàn bộ Transaction sẽ rollback
        userRepository.save(user);
        orderRepository.save(order);
    }
}
```

---

## 7. Hướng Dẫn Viết Unit Test

Mọi module mới khi tạo ra **bắt buộc** phải có bài kiểm thử độc lập đặt tại `src/test/java/vn/org/thn/app/modules/<module_name>/`:

```java
package vn.org.thn.app.modules.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testCreateUserSuccess() {
        // Given
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("testuser");
        
        // When
        UserResponse resp = userService.createUser(req);
        
        // Then
        Assertions.assertNotNull(resp.getId());
        Assertions.assertEquals("testuser", resp.getUsername());
    }
}
```

---

## 8. Swagger UI & Cấu Hình Bí Mật (Secrets)

### 8.1 Đường Dẫn Swagger UI & OpenAPI Docs
Khi ứng dụng khởi chạy (mặc định port `8080`):
- **Giao diện Swagger UI**: `http://localhost:8080/api.html`
- **OpenAPI Json Docs**: `http://localhost:8080/doc`

### 8.2 Cấu Hình Bí Mật (`config/secrets.yaml`)
Để tránh lộ mật khẩu CSDL hoặc API Key lên Git repository:
- Credentials thực tế được lưu tại file `config/secrets.yaml` (file này nằm trong `.gitignore`).
- File mẫu cấu trúc: `config/secrets.yaml.example`.
- Spring Boot tự động import qua cấu hình `spring.config.import: "optional:file:./config/secrets.yaml"`.

---

## 9. Mẫu Hướng Dẫn Phát Triển Tính Năng Mới (Recipe for AI Agent)

> 🤖 **Khi nhận yêu cầu tạo mới một module (Ví dụ: `Order`):**
> 1. **Tạo Entity**: `vn.org.thn.app.modules.order.domain.entity.OrderEntity` kế thừa `BaseEntity`, khai báo `@Entity`, `@Table(name = "tbl_order")`, các trường với `@Column`.
> 2. **Tạo Repository**: `vn.org.thn.app.modules.order.infrastructure.OrderRepository` kế thừa `BaseRepositoryImpl<OrderEntity, Long>` với annotation `@Repository`.
> 3. **Tạo DTOs**: `OrderCreateRequest`, `OrderUpdateRequest`, `OrderResponse` trong `vn.org.thn.app.modules.order.api.dto/`.
> 4. **Tạo Service**: `vn.org.thn.app.modules.order.application.OrderService` xử lý logic nghiệp vụ và phân trang với `@Transactional`.
> 5. **Tạo Controller**: `vn.org.thn.app.modules.order.api.OrderCtl` kế thừa `BaseCtl`, gắn `@RestController`, `@RequestMapping("/public/order")`, kèm chú thích Swagger `@Tag` và `@Operation`.
> 6. **Tạo Migration SQL**: Thêm file `database/<db_type>/V<N>__init_order.sql`.
> 7. **Tạo Unit Test**: Thêm `src/test/java/vn/org/thn/app/modules/order/OrderServiceTest.java`.
