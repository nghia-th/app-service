package vn.org.thn.app.base.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.*;

/** Round-trip tests for PemUtils: a key written to disk must read back byte-for-byte equivalent. */
class PemUtilsTest {

    @TempDir
    Path tempDir;

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Test
    @DisplayName("Should write and read back an RSA key pair with the same modulus/exponents")
    void writeAndReadKeyPair_roundTrips() throws Exception {
        KeyPair keyPair = generateKeyPair();
        RSAPrivateKey originalPrivate = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey originalPublic = (RSAPublicKey) keyPair.getPublic();

        Path privatePath = tempDir.resolve("private.pem");
        Path publicPath = tempDir.resolve("public.pem");

        PemUtils.writePrivateKey(privatePath, originalPrivate);
        PemUtils.writePublicKey(publicPath, originalPublic);

        assertTrue(Files.exists(privatePath));
        assertTrue(Files.exists(publicPath));

        RSAPrivateKey readPrivate = PemUtils.readPrivateKey(privatePath);
        RSAPublicKey readPublic = PemUtils.readPublicKey(publicPath);

        assertEquals(originalPrivate.getModulus(), readPrivate.getModulus());
        assertEquals(originalPrivate.getPrivateExponent(), readPrivate.getPrivateExponent());
        assertEquals(originalPublic.getModulus(), readPublic.getModulus());
        assertEquals(originalPublic.getPublicExponent(), readPublic.getPublicExponent());
    }

    @Test
    @DisplayName("Should create parent directories that don't exist yet")
    void writePrivateKey_missingParentDirectory_createsIt() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Path nested = tempDir.resolve("nested/dir/private.pem");

        PemUtils.writePrivateKey(nested, (RSAPrivateKey) keyPair.getPrivate());

        assertTrue(Files.exists(nested));
    }

    @Test
    @DisplayName("Written PEM file uses standard PKCS8/X.509 headers")
    void writtenFiles_haveExpectedPemHeaders() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Path privatePath = tempDir.resolve("private.pem");
        Path publicPath = tempDir.resolve("public.pem");

        PemUtils.writePrivateKey(privatePath, (RSAPrivateKey) keyPair.getPrivate());
        PemUtils.writePublicKey(publicPath, (RSAPublicKey) keyPair.getPublic());

        String privateContent = Files.readString(privatePath);
        String publicContent = Files.readString(publicPath);

        assertTrue(privateContent.contains("BEGIN PRIVATE KEY"));
        assertTrue(publicContent.contains("BEGIN PUBLIC KEY"));
    }
}
