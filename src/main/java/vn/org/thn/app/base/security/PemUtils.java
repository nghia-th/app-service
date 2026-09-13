package vn.org.thn.app.base.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Reads and writes RSA keys as plain PEM text files (PKCS#8 for the private key, X.509 for the
 * public key) - the format {@code base.security.jwt.standalone.private-key-path}/
 * {@code public-key-path} point at. No external crypto library needed: {@link KeyFactory} already
 * understands both encodings, this class only handles the PEM (base64 + header/footer) wrapping.
 */
final class PemUtils {

    private static final String PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_KEY_FOOTER = "-----END PRIVATE KEY-----";
    private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----";

    private PemUtils() {
    }

    static RSAPrivateKey readPrivateKey(Path path) {
        byte[] der = decodePem(readFile(path), PRIVATE_KEY_HEADER, PRIVATE_KEY_FOOTER);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Cannot parse RSA private key from " + path, e);
        }
    }

    static RSAPublicKey readPublicKey(Path path) {
        byte[] der = decodePem(readFile(path), PUBLIC_KEY_HEADER, PUBLIC_KEY_FOOTER);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(der));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Cannot parse RSA public key from " + path, e);
        }
    }

    static void writePrivateKey(Path path, RSAPrivateKey key) {
        writePem(path, key.getEncoded(), PRIVATE_KEY_HEADER, PRIVATE_KEY_FOOTER);
    }

    static void writePublicKey(Path path, RSAPublicKey key) {
        writePem(path, key.getEncoded(), PUBLIC_KEY_HEADER, PUBLIC_KEY_FOOTER);
    }

    private static byte[] decodePem(String pem, String header, String footer) {
        String base64 = pem
                .replace(header, "")
                .replace(footer, "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private static void writePem(Path path, byte[] der, String header, String footer) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        String pem = header + "\n" + base64 + "\n" + footer + "\n";
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, pem, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write key to " + path, e);
        }
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read key file " + path, e);
        }
    }
}
