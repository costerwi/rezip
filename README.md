# ReZip
For more efficient Git packing of ZIP based files.

## Motivation

Many popular applications, such as
[Microsoft](http://en.wikipedia.org/wiki/Office_Open_XML) and
[Open](http://en.wikipedia.org/wiki/OpenDocument) Office,
save their documents as XML in compressed zip containers.
Small changes to these document's contents may result in big changes to their
compressed binary container file.
When compressed files are stored in a Git repository
these big differences make delta compression inefficient or impossible
and the repository size is roughly the sum of its revisions.

This small program acts as a Git clean filter driver.
It reads a ZIP file from stdin and outputs the same ZIP content to stdout,
but without compression.

##### pros

+ human readbale/plain-text diffs of (ZIP based) archives,
  (if they contain plain-text files)
+ smaller overall repository size if the archive contents change frequently

##### cons

- slower `git add`/`git commit` process
- (optional) slower checkout process

## How it works

On every `git add` operation, the files assigned to the ZIP based file type in
_.gitattributes_ are piped through this filter to remove their compression.
Git internally uses zlib compression to store the resulting blob,
so the final size of the loose object in the repository is usually comparable
to the size of the original compressed ZIP document.

The advantage of passing uncompressed data to Git,
is that during garbage collection,
when Git merges loose objects into packfiles,
the delta compression it uses will be able to more efficiently pack the common
data it finds among these uncompressed revisions.
This can reduce the repository size by up to 50%, depending on the data.

The smudge filter will re-compress the
ZIP documents when they are checked out.
The rezipped file may be a different size than the original,
because of the compression level used by the filter.
The use of this filter at checkout will save disk space in the working
directory, at the expense of performance during checkout.
I have not found any application yet, that refused to read an
uncompressed ZIP document, so the smudge filter is optional.
This also means that repositories may be downloaded and used immediately,
without any special burdon on the recipients to install this filter driver.

If other contributors add compressed ZIP documents to the repository
without using the clean filter (the one applied during `add`/`commit`),
the only harm will be the usual loss of packing efficiency for compressed
documents during garbage collection, and non-verbose diffs.

## Inspiration and similar projects

The idea to commit ZIP documents to the repository in uncompressed form was
based on concepts demonstrated in the
[Mercurial Zipdoc extension](http://mercurial.selenic.com/wiki/ZipdocExtension)
by Andreas Gobell.

[OoXmlUnpack](https://bitbucket.org/htilabs/ooxmlunpack) is a similar program
for Mercurial, written in C#, which also pretty-prints the XML files and adds
some file handling features specific to Excel.

[callegar/Rezip](https://github.com/callegar/Rezip) should be compatible with
this Git filter, but is written as a bash script to drive Info-ZIP zip/unzip
executables.

[Zippey](https://bitbucket.org/sippey/zippey) is a similar method available
for Git, written in python,
but it stores uncompressed data as custom records within the Git repository.
This format is not directly usable without the smudge filter, so it is a less
portable option.

## Human readable diffing

This filter is only concerned with the efficient storage of ZIP data within Git.
For human readable diffs between revisions,
You will need to add a Git `textconv` program that can convert your format into text.
Direct merges are not possible, since they would corrupt the ZIP CRC checksum.
If the data within the ZIP is plain-text,
then you could visualize differences with a `textconv` program like
[zipdoc](https://github.com/costerwi/zipdoc).
For more complex documents, there are domain specific options.
For example for
[word processing](http://blog.martinfenner.org/2014/08/25/using-microsoft-word-with-git/),
[Excel](https://github.com/tokuhirom/git-xlsx-textconv),
and
[Simulink](https://github.com/costerwi/simulink-mergeDiff).

## Installation

This program requires Java JRE 8 or newer.
Store _ReZip.class_ somewhere in your home directory,
for example `~/bin`, or in your repository.

Define the filter drivers in `~/.gitconfig`:
```
git config --global --replace-all filter.rezip.clean "java -cp ~/bin ReZip --store"
# optionally add smudge filter:
git config --global --add filter.rezip.smudge "java -cp ~/bin ReZip"
```

Assign filter attributes to paths in `<repo-root>/.gitattributes`:
```
# MS Office
*.docx  filter=rezip
*.xlsx  filter=rezip
*.pptx  filter=rezip
# OpenOffice
*.odt   filter=rezip
*.ods   filter=rezip
*.odp   filter=rezip
# Misc
*.mcdx  filter=rezip
*.slx   filter=rezip
```

As described in [gitattributes](http://git-scm.com/docs/gitattributes),
you may see unnecessary merge conflicts when you add attributes to a file that
causes the repository format for that file to change.
To prevent this, Git can be told to run a virtual check-out and check-in of all
three stages of a file when resolving a three-way merge:
```
git config --add --bool merge.renormalize true
```

## Observations

The following are based on my experience in real-world cases.
Use at your own risk.
Your mileage may vary.

### Simulink

* One packed repository with rezip was 54% of the size of the packed repository
  storing compressed ZIPs.
* Another repository with 280 \*.slx files and over 3000 commits was originally 281 MB
  and was reduced to 156 MB using this technique (55% of baseline).

### Powerpoint

I found that the loose objects stored without this filter were about 5% smaller
than the original file size (zlib on top of zip compression).
When using the rezip filter, the loose objects were about 10% smaller than the
original files, since zlib could work more efficiently on uncompressed data.
The packed repository with rezip was only 10% smaller than the packed repository
storing compressed zips.
I think this unremarkable efficiency improvement is due to a large number of
\*.png files in the presentation which were already stored without compression in the original \*.pptx.

