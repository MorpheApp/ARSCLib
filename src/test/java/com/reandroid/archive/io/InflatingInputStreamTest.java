package com.reandroid.archive.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

public class InflatingInputStreamTest {

    @Test
    public void testReadsWhatWasDeflated() throws IOException {
        byte[] data = "res/layout/main.xml".getBytes(StandardCharsets.UTF_8);
        try (InflatingInputStream inputStream = open(data)) {
            Assert.assertArrayEquals(data, readAll(inputStream));
        }
    }

    @Test
    public void testCloseReleasesInflater() throws IOException {
        InflaterProbe inputStream = new InflaterProbe(deflate(new byte[]{1, 2, 3}));
        inputStream.close();
        try {
            inputStream.inflater().getBytesRead();
            Assert.fail("Inflater still holds native memory after close");
        } catch (NullPointerException ignored) {
            // Thrown by an Inflater that has ended
        }
        inputStream.close();
    }

    private static InflatingInputStream open(byte[] data) throws IOException {
        return new InflatingInputStream(new ByteArrayInputStream(deflate(data)), 512);
    }

    private static byte[] deflate(byte[] data) throws IOException {
        ByteArrayOutputStream deflated = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        try (DeflaterOutputStream outputStream = new DeflaterOutputStream(deflated, deflater)) {
            outputStream.write(data);
        } finally {
            deflater.end();
        }
        return deflated.toByteArray();
    }

    // The Inflater is only visible to a subclass
    private static class InflaterProbe extends InflatingInputStream {
        InflaterProbe(byte[] deflated) {
            super(new ByteArrayInputStream(deflated), 512);
        }

        Inflater inflater() {
            return inf;
        }
    }

    private static byte[] readAll(InflatingInputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[64];
        int read;
        while ((read = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }
}
