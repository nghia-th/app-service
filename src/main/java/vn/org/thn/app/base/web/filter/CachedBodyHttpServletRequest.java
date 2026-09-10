package vn.org.thn.app.base.web.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Wraps a request and eagerly reads its body into memory, so the body can be logged
 * ({@link RequestContextFilter}) and still be read normally by the actual handler downstream
 * (a request's input stream can only be consumed once otherwise).
 * <p>
 * Only constructed by {@link RequestContextFilter} for bodies it has already confirmed are within
 * its size cap ({@code MAX_LOGGED_BODY_BYTES}) - this class itself does not enforce any limit, so
 * it should not be reused elsewhere to wrap a request of unknown/unbounded size.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    /** Reads the whole request body into memory up front. */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    /** The cached body, decoded as UTF-8. */
    public String getBody() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    /** A fresh stream over the cached body, so it can be read again by the real handler. */
    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(cachedBody);
    }

    /** A reader over the cached body (delegates to {@link #getInputStream()}). */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    /** Minimal {@link ServletInputStream} over an in-memory byte array (no async read-listener support needed here). */
    private static final class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream buffer;

        CachedBodyServletInputStream(byte[] body) {
            this.buffer = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return buffer.read();
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
        }
    }
}
