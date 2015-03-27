#!/usr/bin/env python
"""Read zip format file from stdin and write new zip to stdout.

With the --store option the output will be an uncompressed zip.
Uncompressed files are stored more efficiently in Git.

https://github.com/costerwi/rezip
"""

import sys
import io
from zipfile import *

import argparse
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--store", 
        help="Store data to stdout zip without compression",
        action="store_true")
args = parser.parse_args()
if args.store:
    compression = ZIP_STORED
else:
    compression = ZIP_DEFLATED

if not hasattr(sys.stdout, 'buffer'):
    raise RuntimeError('Sorry, Python3 is required.')

# Use BytesIO objects as random access source and destination files
with io.BytesIO(sys.stdin.buffer.read()) as source, io.BytesIO() as dest:

    # Read and re-zip the file in memory
    with ZipFile(source, 'r') as source_zip, ZipFile(dest, 'w') as dest_zip:
        for info in source_zip.infolist(): # Iterate over each file in zip
            dest_zip.writestr(info, source_zip.read(info), compression)
        dest_zip.comment = source_zip.comment # Copy the comment if any

    # Write the dest file as binary to stdout
    dest.seek(0)
    sys.stdout.buffer.write(dest.read())
