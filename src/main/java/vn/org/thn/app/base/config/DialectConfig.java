package vn.org.thn.app.base.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.org.thn.app.base.persistence.dialect.DialectFactory;
import vn.org.thn.app.base.persistence.dialect.SqlDialect;

/** Registers the single {@link SqlDialect} bean matching the configured {@link DatabaseProperties#getType()}. */
@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class DialectConfig {

    /** All SQL-generation code in the ORM depends on this one bean rather than switching on the engine itself. */
    @Bean
    public SqlDialect sqlDialect(DatabaseProperties properties) {
        return DialectFactory.create(properties.getType());
    }
}
