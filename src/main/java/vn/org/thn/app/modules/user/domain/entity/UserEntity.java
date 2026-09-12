package vn.org.thn.app.modules.user.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.org.thn.app.base.core.entity.BaseEntity;
import vn.org.thn.app.base.persistence.annotation.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_user")
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "full_name")
    private String fullName;

    /**
     * Unaccented mirror of {@link #fullName}, auto-populated by the framework's {@code @Unaccent}
     * handling on every insert/update - see docs/BASE_FRAMEWORK_GUIDE.md 2.8 and
     * docs/PROMPT_TEMPLATES.md "Mau 2.3" for the smart-search pattern this backs (matched via
     * {@code likeAnyOrderUnaccent} in {@code UserService#getUsersPaged}), and
     * database/*&#47;V4__add_user_search_indexes.sql for the migration that adds this column and
     * its index. This entity is the flagship example those docs already used for the feature
     * (searching "Truong Hieu Nghia" regardless of word order/diacritics) - it previously never
     * actually declared it (Medium finding #13, 2026-09-12 review).
     */
    @Unaccent(from = "fullName")
    @Column(name = "full_name_unaccent")
    private String fullNameUnaccent;

    @Column(name = "status")
    private String status;

    @Column(name = "role")
    private String role;
}
