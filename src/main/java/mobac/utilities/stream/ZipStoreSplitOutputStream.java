/*******************************************************************************
 * Copyright (c) MOBAC developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.utilities.stream;

import java.io.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipStoreSplitOutputStream extends OutputStream {
    private final long MAX_FILE_SIZE = 1000 * 1000 * 1024; // Whatever size you want
    private ZipOutputStream zipOutputStream;

    private String name;
    private String postfix;

    private long currentSize;
    private int currentChunkIndex = 1;

    private CRC32 crc = new CRC32();

    public ZipStoreSplitOutputStream(File f) throws FileNotFoundException {
        String filePath = f.getPath();
        int idx = filePath.lastIndexOf(".");
        name = filePath.substring(0, idx);
        postfix = filePath.substring(idx);
        constructNewStream();
    }

    /**
     * Warning this method is not thread safe!
     *
     * @param name file name including path in the zip
     * @param data
     * @throws IOException
     */
    public void writeStoredEntry(String name, byte[] data) throws IOException {
        ZipEntry ze = new ZipEntry(name);
        ze.setMethod(ZipEntry.STORED);
        ze.setCompressedSize(data.length);
        ze.setSize(data.length);
        crc.reset();
        crc.update(data);
        ze.setCrc(crc.getValue());

        long entrySize = ze.getCompressedSize();
        if ((currentSize + entrySize) > MAX_FILE_SIZE) {
            closeStream();
            constructNewStream();
        }

        try {
            zipOutputStream.putNextEntry(ze);
            zipOutputStream.write(data);
            zipOutputStream.closeEntry();
            currentSize += entrySize;
        }catch (Exception e){

        }
    }

    private void closeStream() throws IOException {
        zipOutputStream.flush();
        zipOutputStream.close();
    }

    private void constructNewStream() throws FileNotFoundException {
        zipOutputStream = new ZipOutputStream(new FileOutputStream(new File(constructCurrentPartName())));
        currentChunkIndex++;
        currentSize = 0;
    }

    private String constructCurrentPartName() {
        return name + String.format(".%03d", currentChunkIndex) + postfix;
    }

    @Override
    public void write(int b) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void close() throws IOException {
        zipOutputStream.close();
    }

    public void flush() throws IOException {
        zipOutputStream.flush();
    }
}
