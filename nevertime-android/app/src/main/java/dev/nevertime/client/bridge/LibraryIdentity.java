package dev.nevertime.client.bridge;

import java.io.*;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class LibraryIdentity {
    public static final String EXPECTED_SHA256 = "c3ff36e845a7b9d200d01bbed81188784438989153a6a972cb003d92f8e1ceda";
    public static final long EXPECTED_SIZE = 328419680L;
    private LibraryIdentity() {}

    public static String hashLoadedPath(String path) throws Exception {
        if (path == null || path.isEmpty()) throw new IOException("Empty engine path");
        int split = path.indexOf("!/");
        if (split >= 0) {
            try (ZipFile zip = new ZipFile(path.substring(0, split))) {
                ZipEntry entry = zip.getEntry(path.substring(split + 2));
                if (entry == null || entry.isDirectory()) throw new IOException("Engine entry missing in APK");
                try (InputStream in = zip.getInputStream(entry)) { return sha256(in); }
            }
        }
        try (InputStream in = new FileInputStream(path)) { return sha256(in); }
    }

    public static String sha256(InputStream in) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[256 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        StringBuilder out = new StringBuilder(64);
        for (byte value : digest.digest()) out.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return out.toString();
    }

    public static boolean matches(String hash) { return EXPECTED_SHA256.equals(hash); }
}
