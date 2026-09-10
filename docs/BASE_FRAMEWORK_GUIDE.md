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
5. [Cấu Hình Đa CSDL (Database Configuration)](#5-cấu-hình-đa-csdl-database-configuration)
6. [Mẫu Hướng Dẫn Phát Triển Tính Năng Mới (Recipe for AI Agent)](#6-mẫu-hướng-dẫn-phát-triển-tính-năng-mới-recipe-for-ai-agent)

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
package vn.org.thn.app.demo.domain;

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
package vn.org.thn.app.demo.infrastructure;

import org.springframework.stereotype.Repository;
import vn.org.thn.app.base.persistence.repository.BaseRepositoryImpl;
import vn.org.thn.app.demo.domain.Product;

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

---

### 2.7 Native Query & MyBatis Mapper
Trong trường hợp cần viết câu SQL phức tạp hoặc JOIN nhiều bảng:

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

---

## 3. Phân Hệ Web Core & Controller

### 3.1 Controller Kế Thừa `BaseCtl`
Mọi REST Controller nên kế thừa `BaseCtl` để sử dụng các helper response:

```java
package vn.org.thn.app.demo.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.org.thn.app.base.core.dto.page.PageResponse;
import vn.org.thn.app.base.core.response.ApiResponse;
import vn.org.thn.app.base.web.controller.BaseCtl;
import vn.org.thn.app.demo.domain.Product;
import vn.org.thn.app.demo.infrastructure.ProductRepository;

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

---

## 4. Phân Hệ Đa Ngôn Ngữ i18n

Dự án có sẵn service quản lý dịch đa ngôn ngữ đồng bộ giữa DB và JSON file.
- **Service**: `LanguageService`
- **Tải danh sách ngôn ngữ**: `languageService.loadLanguage()`
- **Cập nhật key dịch**: `languageService.updateLanguage(request)`

---

## 5. Cấu Hình Đa CSDL (Database Configuration)

Cấu hình trong file [application.yaml](file:///Volumes/Data/04.MyProject/java-project/app-service/src/main/resources/application.yaml):

```yaml
base:
  database:
    type: SQLITE              # Lựa chọn: SQLITE / POSTGRESQL / MYSQL / SQLSERVER / ORACLE
    db-name: app_db           # Tên CSDL
    sql-log: BASIC            # Log SQL: OFF / BASIC / FULL
```

---

## 6. Mẫu Hướng Dẫn Phát Triển Tính Năng Mới (Recipe for AI Agent)

> **Khi nhận yêu cầu tạo mới một module (Ví dụ: `Order`):**
> 1. **Tạo Entity**: `vn.org.thn.app.demo.domain.Order` kế thừa `BaseEntity`, khai báo `@Entity`, `@Table(name = "tbl_order")`, các trường với `@Column`.
> 2. **Tạo Repository**: `vn.org.thn.app.demo.infrastructure.OrderRepository` kế thừa `BaseRepositoryImpl<Order, Long>` với annotation `@Repository`.
> 3. **Tạo Service**: `vn.org.thn.app.demo.application.OrderService` (nếu có xử lý logic phức tạp).
> 4. **Tạo Controller**: `vn.org.thn.app.demo.api.OrderCtl` kế thừa `BaseCtl`, gắn `@RestController`, `@RequestMapping("/public/order")`, cùng Swagger `@Operation` và `@ApiResponses`.
