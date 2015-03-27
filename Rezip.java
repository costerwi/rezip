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

        byte[] buffer = new byte[2048];
        ZipEntry source_entry;
        CRC32 cksum = new CRC32();
        try {
            while ((source_entry = source_zip.getNextEntry()) != null) {
                ByteArrayOutputStream uncomp_bs = new ByteArrayOutputStream();
                CheckedOutputStream uncomp_os = new CheckedOutputStream(uncomp_bs, cksum);
                cksum.reset();

                // Copy file from source_zip into uncompressed, checksummed output stream
                int len = 0;
                while ((len = source_zip.read(buffer)) > 0) {
                    uncomp_os.write(buffer, 0, len);
                }
                source_zip.closeEntry();

                // Create destination entry based on source entry
                ZipEntry dest_entry = new ZipEntry(source_entry);
                dest_entry.setSize(uncomp_bs.size());
                dest_entry.setCrc(cksum.getValue());
                dest_entry.setMethod(compression);
                dest_entry.setCompressedSize(-1); // Unknown compressed size

                // Copy uncompressed file into destination zip
                dest_zip.putNextEntry(dest_entry);
                uncomp_bs.writeTo(dest_zip);
                dest_zip.closeEntry();
            }
        } finally {
            source_zip.close();
            dest_zip.close();
        }
    }
}
