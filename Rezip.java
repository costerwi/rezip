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

import java.io.*;
import java.util.zip.*;

public class Rezip {
    /**
     * Read zip format file from stdin and write new zip to stdout.
     *
     * With the --store command line argument the output will be an 
     * uncompressed zip.
     * Uncompressed files are stored more efficiently in Git.
     * {@link https://github.com/costerwi/rezip}
     */
    public static void main(String argv[]) throws IOException {
        int compression = ZipEntry.DEFLATED;
        for (String arg : argv) {
            if (arg.equals("--store")) {
                compression = ZipEntry.STORED;
            } else {
                System.err.println("Usage: Rezip {--store} <in.zip >out.zip");
                System.exit(1);
            }
        }

        ZipInputStream source_zip = new ZipInputStream(System.in);
        ZipOutputStream dest_zip = new ZipOutputStream(System.out);

        byte[] buffer = new byte[8192];
        ZipEntry entry;
        ByteArrayOutputStream uncomp_bs = new ByteArrayOutputStream();
        CRC32 cksum = new CRC32();
        CheckedOutputStream uncomp_os = new CheckedOutputStream(uncomp_bs, cksum);
        try {
            while ((entry = source_zip.getNextEntry()) != null) {
                uncomp_bs.reset();
                cksum.reset();

                // Copy file from source_zip into uncompressed, checksummed output stream
                int len = 0;
                while ((len = source_zip.read(buffer)) > 0) {
                    uncomp_os.write(buffer, 0, len);
                }
                source_zip.closeEntry();

                // Modify zip entry for destination zip
                entry.setSize(uncomp_bs.size());
                entry.setCrc(cksum.getValue());
                entry.setMethod(compression);
                entry.setCompressedSize(-1); // Unknown compressed size

                // Copy uncompressed file into destination zip
                dest_zip.putNextEntry(entry);
                uncomp_bs.writeTo(dest_zip);
                dest_zip.closeEntry();
            }
        } finally {
            source_zip.close();
            dest_zip.close();
        }
    }
}
