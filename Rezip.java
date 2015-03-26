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
                System.err.println(arg);
                System.err.println("Usage: Rezip {--store} <in.zip >out.zip");
                System.exit(1);
            }
        }

        ZipInputStream source_zip = new ZipInputStream(System.in);
        ZipOutputStream dest_zip = new ZipOutputStream(System.out);
        byte[] buffer = new byte[2048];
        ZipEntry source_entry;
        try {
            while ((source_entry = source_zip.getNextEntry()) != null) {
                ZipEntry dest_entry = (ZipEntry) source_entry.clone();
                dest_entry.setCompressedSize(-1);   // Unknown size
                dest_entry.setMethod(compression);
                dest_zip.putNextEntry(dest_entry);
                int len = 0;
                while ((len = source_zip.read(buffer)) > 0) {
                    dest_zip.write(buffer, 0, len);
                }
                source_zip.closeEntry();
                dest_zip.closeEntry();
            }
        } finally {
            source_zip.close();
            dest_zip.close();
        }
    }
}
