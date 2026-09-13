package vn.org.thn.app.base.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Loads the RSA key pair {@link JwtMode#STANDALONE} signs tokens with, from the PEM files at
 * {@code base.security.jwt.standalone.private-key-path}/{@code public-key-path}.
 * <p>
 * If either file is missing: on the {@code dev} or {@code test} Spring profile, a fresh key pair is
 * generated once and written to those exact paths so every later restart reuses it (only the very
 * first run on a freshly-cloned machine pays this cost) - a convenience so a service builds and
 * runs immediately without a manual key-generation step. On every other profile (staging, prod, or
 * no profile at all), a missing file is a hard startup failure instead: silently minting a new prod
 * signing key would invalidate every token already issued and could just as easily be masking a
 * deployment/config mistake (wrong path, key files not shipped with the release) rather than a
 * genuinely new environment.
 */
final class StandaloneRsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(StandaloneRsaKeyProvider.class);

    private static final String KEY_GENERATION_ALLOWED_PROFILES_MESSAGE =
            "set base.security.jwt.standalone.private-key-path/public-key-path to existing PEM files, "
                    + "or generate a key pair up front (this auto-generation only ever runs on the dev/test profile)";

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    StandaloneRsaKeyProvider(JwtProperties properties, Environment environment) {
        JwtProperties.Standalone config = properties.getStandalone();
        Path privateKeyPath = requirePath(config.getPrivateKeyPath(), "base.security.jwt.standalone.private-key-path");
        Path publicKeyPath = requirePath(config.getPublicKeyPath(), "base.security.jwt.standalone.public-key-path");

        boolean bothExist = Files.exists(privateKeyPath) && Files.exists(publicKeyPath);
        if (!bothExist) {
            if (!environment.acceptsProfiles(Profiles.of("dev", "test"))) {
                throw new IllegalStateException(
                        "base.security.jwt.mode=STANDALONE but the RSA key pair is missing ("
                                + privateKeyPath + " / " + publicKeyPath + ") on a non-dev/test profile - "
                                + KEY_GENERATION_ALLOWED_PROFILES_MESSAGE);
            }
            log.warn("RSA key pair not found at {} / {} - generating a new one for this dev/test run "
                    + "(this only happens once; the generated files are reused on every later restart)",
                    privateKeyPath, publicKeyPath);
            generateAndWrite(privateKeyPath, publicKeyPath, config.getKeySize());
        }

        this.privateKey = PemUtils.readPrivateKey(privateKeyPath);
        this.publicKey = PemUtils.readPublicKey(publicKeyPath);
    }

    RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    RSAPublicKey getPublicKey() {
        return publicKey;
    }

    private static Path requirePath(String configuredPath, String propertyName) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException(
                    "base.security.jwt.mode=STANDALONE requires " + propertyName + " to be set");
        }
        return Path.of(configuredPath);
    }

    private static void generateAndWrite(Path privateKeyPath, Path publicKeyPath, int keySize) {
        KeyPair keyPair;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(keySize);
            keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA key generation is unavailable in this JVM", e);
        }
        PemUtils.writePrivateKey(privateKeyPath, (RSAPrivateKey) keyPair.getPrivate());
        PemUtils.writePublicKey(publicKeyPath, (RSAPublicKey) keyPair.getPublic());
    }
}
