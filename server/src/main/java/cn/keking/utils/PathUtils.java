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
    public static String concat(String... paths) {

        // 将所有\处理为/
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            if (path == null) {
                throw new IllegalArgumentException("路径不能为null");
            }
            paths[i] = path.replaceAll("\\\\", "/");
        }
        StringBuilder sb = new StringBuilder(paths[0]);
        for (int i = 1; i < paths.length; i++) {
            String curPath = paths[i];
            boolean hasLeftSlash = false;
            if (sb.length() > 0) {
                hasLeftSlash = sb.charAt(sb.length() - 1) == '/';
            }
            boolean hasRightSlash = curPath.startsWith("/");
            if (hasLeftSlash && hasRightSlash) {
                sb.append(curPath.substring(1));
            } else if (!hasLeftSlash && !hasRightSlash) {
                sb.append('/').append(curPath);
            } else {
                sb.append(curPath);
            }
        }
        return sb.toString();
    }

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
