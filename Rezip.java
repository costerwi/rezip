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

public class Rezip {
    /**
     * Read zip format file from stdin and write new zip to stdout.
     *
     * With the --store command line argument the output will be an
     * uncompressed zip.
     * Uncompressed files are stored more efficiently in Git.
     * {@see https://github.com/costerwi/rezip}
     */
    public static void main(final String[] argv) throws IOException {
        int compression = ZipEntry.DEFLATED;
        for (final String arg : argv) {
            if (arg.equals("--store")) {
                compression = ZipEntry.STORED;
            } else {
                System.err.printf("Usage: %s {--store} <in.zip >out.zip\n", Rezip.class.getSimpleName());
                System.exit(1);
            }
        }

        final byte[] buffer = new byte[8192];
        ZipEntry entry;
        final ByteArrayOutputStream uncompressed_bs = new ByteArrayOutputStream();
        final CRC32 checksum = new CRC32();
        final CheckedOutputStream uncompressed_os = new CheckedOutputStream(uncompressed_bs, checksum);
        try (final ZipInputStream source_zip = new ZipInputStream(System.in); final ZipOutputStream dest_zip = new ZipOutputStream(System.out)) {
            while ((entry = source_zip.getNextEntry()) != null) {
                uncompressed_bs.reset();
                checksum.reset();

                // Copy file from source_zip into uncompressed, check-summed output stream
                int len;
                while ((len = source_zip.read(buffer)) > 0) {
                    uncompressed_os.write(buffer, 0, len);
                }
                source_zip.closeEntry();

                // Modify zip entry for destination zip
                entry.setSize(uncompressed_bs.size());
                entry.setCrc(checksum.getValue());
                entry.setMethod(compression);
                entry.setCompressedSize(-1); // Unknown compressed size

                // Copy uncompressed file into destination zip
                dest_zip.putNextEntry(entry);
                uncompressed_bs.writeTo(dest_zip);
                dest_zip.closeEntry();
            }
        }
    }
}

