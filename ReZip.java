/*
 * Copyright 2015-2017 Carl Osterwisch <costerwi@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Read ZIP content and writes (optionally <i>uncompressed</i>) ZIP out.
 * Uncompressed files are stored more efficiently in Git.
 * {@see https://github.com/costerwi/rezip}
 */
public class ReZip {

    /**
     * Reads a ZIP file from stdin and writes new ZIP to stdout.
     * With the --store command line argument the output will be an
     * uncompressed zip.
     */
    public static void main(final String[] argv) throws IOException {
        int compression = ZipEntry.DEFLATED;
        for (final String arg : argv) {
            if (arg.equals("--store")) {
                compression = ZipEntry.STORED;
            } else {
                System.err.printf("Usage: %s {--store} <in.zip >out.zip\n", ReZip.class.getSimpleName());
                System.exit(1);
            }
        }

        final byte[] buffer = new byte[8192];
        ZipEntry entry;
        final ByteArrayOutputStream uncompressedOutRaw = new ByteArrayOutputStream();
        final CRC32 checksum = new CRC32();
        final CheckedOutputStream uncompressedOutChecked = new CheckedOutputStream(uncompressedOutRaw, checksum);
        try (final ZipInputStream zipIn = new ZipInputStream(System.in); final ZipOutputStream zipOut = new ZipOutputStream(System.out)) {
            while ((entry = zipIn.getNextEntry()) != null) {
                uncompressedOutRaw.reset();
                checksum.reset();

                // Copy file from zipIn into uncompressed, check-summed output stream
                int len;
                while ((len = zipIn.read(buffer)) > 0) {
                    uncompressedOutChecked.write(buffer, 0, len);
                }
                zipIn.closeEntry();

                // Modify ZIP entry for destination ZIP
                entry.setSize(uncompressedOutRaw.size());
                entry.setCrc(checksum.getValue());
                entry.setMethod(compression);
                entry.setCompressedSize(-1); // Unknown compressed size

                // Copy uncompressed file into destination ZIP
                zipOut.putNextEntry(entry);
                uncompressedOutRaw.writeTo(zipOut);
                zipOut.closeEntry();
            }
        }
    }
}
