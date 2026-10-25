/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/ARSCLib
 */
package com.reandroid.archive.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

// InflaterInputStream never releases the native zlib memory of an Inflater it is handed, which
// otherwise waits for the garbage collector and piles up over the thousands of entries of an apk
public class InflatingInputStream extends InflaterInputStream {

    public InflatingInputStream(InputStream inputStream, int bufferSize) {
        super(inputStream, new Inflater(true), bufferSize);
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            inf.end();
        }
    }
}
