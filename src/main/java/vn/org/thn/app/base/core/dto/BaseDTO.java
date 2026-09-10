package vn.org.thn.app.base.core.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Common fields shared by response DTOs that mirror an audited entity.
 * Extend this rather than repeating id/createdAt/updatedAt on every DTO.
 */
@Data
public abstract class BaseDTO {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
