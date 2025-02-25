package cn.keking.utils;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PathUtils {
    public static String encode(String path) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (GZIPOutputStream gzipOs = new GZIPOutputStream(out)) {
                gzipOs.write(path.getBytes(StandardCharsets.UTF_8));
            }
            return new String(Base64.getUrlEncoder().encode(out.toByteArray()), StandardCharsets.UTF_8);
        } catch (IOException ignore) {
            return null;
        }
    }

    public static String decode(String path) {
        byte[] bytes = Base64.getUrlDecoder().decode(path.getBytes(StandardCharsets.UTF_8));
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return new String(IOUtils.toByteArray(gzipInputStream), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
