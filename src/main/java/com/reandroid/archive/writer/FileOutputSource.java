/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.archive.writer;

import com.reandroid.archive.InputSource;
import com.reandroid.archive.block.LocalFileHeader;
import com.reandroid.archive.io.ZipFileOutput;
import com.reandroid.archive.io.ZipOutput;

import java.io.IOException;
import java.nio.channels.FileChannel;

class FileOutputSource extends OutputSource {
    private EntryBuffer entryBuffer;
    private boolean writeDirectly;

    FileOutputSource(InputSource inputSource){
        super(inputSource);
    }
    /**
     * Prepares this entry to be written into the archive as it is encoded, instead of to the buffer
     * first, unless it can be copied as it is from the archive it is from.
     */
    void prepareDirectWrite() {
        if (entryBuffer != null) {
            return;
        }
        EntryBuffer entryBuffer = makeFromEntry();
        if (entryBuffer != null) {
            this.entryBuffer = entryBuffer;
        } else {
            writeDirectly = true;
        }
    }
    void makeBuffer(BufferFileInput input, BufferFileOutput output) throws IOException {
        EntryBuffer entryBuffer = this.entryBuffer;
        if(entryBuffer != null){
            return;
        }
        entryBuffer = makeFromEntry();
        if(entryBuffer != null){
            this.entryBuffer = entryBuffer;
            return;
        }
        this.entryBuffer = writeBuffer(input, output);
    }
    private EntryBuffer writeBuffer(BufferFileInput input, ZipOutput output) throws IOException {
        long offset = output.position();
        writeBuffer(output);
        long length = output.position() - offset;
        return new EntryBuffer(input, offset, length);
    }
    EntryBuffer makeFromEntry(){
        return null;
    }
    void writeApk(ZipFileOutput zipFileOutput, ZipAligner zipAligner) throws IOException{
        if (writeDirectly) {
            writeApkDirectly(zipFileOutput, zipAligner);
            return;
        }
        logLargeFileWrite();
        EntryBuffer entryBuffer = this.entryBuffer;
        FileChannel input = entryBuffer.getZipFileInput().getFileChannel();
        input.position(entryBuffer.getOffset());
        writeLFH(zipFileOutput, zipAligner);
        writeData(input, entryBuffer.getLength(), zipFileOutput);
        writeDD(zipFileOutput);
    }
    /**
     * Writes the header, then the data as it is encoded, and then the header again with the sizes and
     * checksum of the data, which are known only once it is written. The header keeps its length, so the
     * data stays where it is aligned.
     */
    private void writeApkDirectly(ZipFileOutput zipFileOutput, ZipAligner zipAligner) throws IOException {
        LocalFileHeader lfh = getLocalFileHeader();
        long headerOffset = zipFileOutput.position();
        writeLFH(zipFileOutput, zipAligner);
        long dataOffset = zipFileOutput.position();
        lfh.setFileOffset(dataOffset);
        writeBuffer(zipFileOutput);
        long dataEnd = zipFileOutput.position();

        zipFileOutput.position(headerOffset);
        lfh.writeBytes(zipFileOutput.getOutputStream());
        if (zipFileOutput.position() != dataOffset) {
            throw new IOException("Header of " + getInputSource().getAlias() + " changed length after its data was written");
        }
        zipFileOutput.position(dataEnd);
        logLargeFileWrite();
        writeDD(zipFileOutput);
    }
    private void writeData(FileChannel input, long length, ZipFileOutput apkFileWriter) throws IOException{
        long offset = apkFileWriter.position();
        LocalFileHeader lfh = getLocalFileHeader();
        lfh.setFileOffset(offset);
        apkFileWriter.write(input, length);
    }


}
