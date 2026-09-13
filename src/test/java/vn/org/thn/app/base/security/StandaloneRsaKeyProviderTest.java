package vn.org.thn.app.base.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers {@link StandaloneRsaKeyProvider}'s three-way branch: auto-generate once on dev/test when
 * missing, fail fast on any other profile when missing, and always fail fast on a blank/missing
 * path regardless of profile (2026-09-12 review's Critical finding #2 follow-up: a silently
 * regenerated prod signing key would invalidate every already-issued token).
 */
class StandaloneRsaKeyProviderTest {

    @TempDir
    Path tempDir;

    private JwtProperties propertiesWithPaths(Path privatePath, Path publicPath) {
        JwtProperties properties = new JwtProperties();
        properties.getStandalone().setPrivateKeyPath(privatePath.toString());
        properties.getStandalone().setPublicKeyPath(publicPath.toString());
        return properties;
    }

    @Test
    @DisplayName("Should auto-generate and persist a key pair once when missing on the dev profile")
    void missingKeys_devProfile_generatesAndWritesKeyPair() {
        Path privatePath = tempDir.resolve("private.pem");
        Path publicPath = tempDir.resolve("public.pem");
        JwtProperties properties = propertiesWithPaths(privatePath, publicPath);
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "dev");
        environment.setActiveProfiles("dev");

        StandaloneRsaKeyProvider provider = new StandaloneRsaKeyProvider(properties, environment);

        assertTrue(Files.exists(privatePath));
        assertTrue(Files.exists(publicPath));
        assertNotNull(provider.getPrivateKey());
        assertNotNull(provider.getPublicKey());
    }

    @Test
    @DisplayName("Should reuse existing keys on a later run instead of regenerating them")
    void existingKeys_devProfile_reusesWithoutRegenerating() {
        Path privatePath = tempDir.resolve("private.pem");
        Path publicPath = tempDir.resolve("public.pem");
        JwtProperties properties = propertiesWithPaths(privatePath, publicPath);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        StandaloneRsaKeyProvider first = new StandaloneRsaKeyProvider(properties, environment);
        StandaloneRsaKeyProvider second = new StandaloneRsaKeyProvider(properties, environment);

        assertEquals(first.getPrivateKey().getModulus(), second.getPrivateKey().getModulus());
        assertEquals(first.getPublicKey().getModulus(), second.getPublicKey().getModulus());
    }

    @Test
    @DisplayName("Should fail fast when keys are missing on a non-dev/test profile (e.g. prod)")
    void missingKeys_prodProfile_throws() {
        Path privatePath = tempDir.resolve("private.pem");
        Path publicPath = tempDir.resolve("public.pem");
        JwtProperties properties = propertiesWithPaths(privatePath, publicPath);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new StandaloneRsaKeyProvider(properties, environment));
        assertTrue(ex.getMessage().contains("non-dev/test"));
        assertFalse(Files.exists(privatePath), "must not write a key file when it refuses to generate one");
    }

    @Test
    @DisplayName("Should fail fast when the private key path is blank, even on dev")
    void blankPrivateKeyPath_throwsRegardlessOfProfile() {
        JwtProperties properties = new JwtProperties();
        properties.getStandalone().setPrivateKeyPath("");
        properties.getStandalone().setPublicKeyPath(tempDir.resolve("public.pem").toString());
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        assertThrows(IllegalStateException.class, () -> new StandaloneRsaKeyProvider(properties, environment));
    }

    @Test
    @DisplayName("Should fail fast when the public key path is not configured at all")
    void nullPublicKeyPath_throws() {
        JwtProperties properties = new JwtProperties();
        properties.getStandalone().setPrivateKeyPath(tempDir.resolve("private.pem").toString());
        properties.getStandalone().setPublicKeyPath(null);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        assertThrows(IllegalStateException.class, () -> new StandaloneRsaKeyProvider(properties, environment));
    }
}
