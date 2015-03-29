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
