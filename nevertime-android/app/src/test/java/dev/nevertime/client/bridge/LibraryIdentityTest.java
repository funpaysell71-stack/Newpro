package dev.nevertime.client.bridge;

import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import static org.junit.Assert.*;

public class LibraryIdentityTest {
    private static final String ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    @Test public void hashesKnownVector() throws Exception {
        assertEquals(ABC, LibraryIdentity.sha256(new ByteArrayInputStream(new byte[]{97,98,99})));
    }
    @Test public void identifiesMismatchWithoutEnablingAnything() {
        assertFalse(LibraryIdentity.matches(ABC));assertFalse(LibraryIdentity.matches(null));
        assertTrue(LibraryIdentity.matches(LibraryIdentity.EXPECTED_SHA256));
    }
    @Test public void readsExtractedLibrary() throws Exception {
        Path file = Files.createTempFile("minecraft", ".so");
        try { Files.write(file,new byte[]{97,98,99}); assertEquals(ABC,LibraryIdentity.hashLoadedPath(file.toString())); }
        finally { Files.deleteIfExists(file); }
    }
    @Test public void readsApkLibraryAndRejectsMissingEntry() throws Exception {
        Path file = Files.createTempFile("minecraft", ".apk");
        try {
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(file))) {
                out.putNextEntry(new ZipEntry("lib/arm64-v8a/libminecraftpe.so"));out.write(new byte[]{97,98,99});out.closeEntry();
            }
            assertEquals(ABC,LibraryIdentity.hashLoadedPath(file + "!/lib/arm64-v8a/libminecraftpe.so"));
            assertThrows(IOException.class,()->LibraryIdentity.hashLoadedPath(file + "!/missing.so"));
        } finally { Files.deleteIfExists(file); }
    }
}
