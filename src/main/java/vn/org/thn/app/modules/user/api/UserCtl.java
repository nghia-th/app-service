package vn.org.thn.app.modules.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.org.thn.app.base.core.dto.page.PageResponse;
import vn.org.thn.app.base.core.response.ApiResponse;
import vn.org.thn.app.base.web.controller.BaseCtl;
import vn.org.thn.app.modules.user.api.dto.UserCreateRequest;
import vn.org.thn.app.modules.user.api.dto.UserResponse;
import vn.org.thn.app.modules.user.api.dto.UserUpdateRequest;
import vn.org.thn.app.modules.user.application.UserService;

@Tag(name = "User Management API", description = "Microservice API endpoints for User management module")
@RestController
@RequestMapping("/public/user")
public class UserCtl extends BaseCtl {

    @Autowired
    private UserService userService;

    @Operation(summary = "Get paged list of users", description = "Retrieve paged users with optional keyword and status filter")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved user list")
    })
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ok(userService.getUsersPaged(page, size, keyword, status));
    }

    @Operation(summary = "Get user by ID", description = "Retrieve a single user details by user ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User details found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable("id") Long id) {
        return ok(userService.getUserById(id));
    }

    @Operation(summary = "Create new user", description = "Create a new user account")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or username exists")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@RequestBody UserCreateRequest request) {
        return ok(userService.createUser(request));
    }

    @Operation(summary = "Update user", description = "Update details of an existing user account")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable("id") Long id,
            @RequestBody UserUpdateRequest request) {
        return ok(userService.updateUser(id, request));
    }

    @Operation(summary = "Delete user", description = "Delete a user account by ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ok();
    }
}
