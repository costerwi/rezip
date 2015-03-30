#Rezip for more efficient Git packing
##About
Many popular applications, such as 
[Microsoft](http://en.wikipedia.org/wiki/Office_Open_XML) and 
[Open](http://en.wikipedia.org/wiki/OpenDocument) Office, 
save their documents as xml in compressed zip containers.
Small changes to these document's contents may result in big changes to their 
compressed binary container file.
When compressed files are stored in a Git repository
these big differences make delta compression inefficient or impossible
and the repository size is roughly the sum of its revisions.

This small program acts as a Git clean filter driver.
It reads a zip file from stdin and outputs the same zip content to stdout 
but without compression.

On every "git add" operation, the files assigned to the zip type in 
.gitattributes are piped through this filter to remove their compression.
Git internally uses zlib compression to store the resulting blob 
so the final size of the loose object in the repository is usually comparable 
to the size of the original compressed zip document.

The advantage of passing uncompressed data to Git is that during garbage 
collection, when Git merges loose objects into packfiles, 
the delta compression it uses will be able to more efficiently pack the common 
data it finds among these uncompressed revisions.
This can reduce the repository size by up to 50% depending on the data.

The smudge filter will re-compress the
zip documents when they are checked out.
The rezipped file may be a different size than the original because of the
compression level used in by the filter.
The use of this filter at checkout will save disk space in the working 
directory at the expense of performance during checkout.
I have not found an application that refused to read an 
uncompressed zip document so the smudge filter is optional.
This also means that repositories may be downloaded and used immediately
without any special burdon on the recipients to install this filter driver.

If other contributors add compressed zip documents to the repository 
without using the clean filter
the only harm will be the usual loss of packing efficiency for compressed
documents during garbage collection.

##Inspiration and similar projects
The idea to commit zip documents to the repository in uncompressed form was 
based on concepts demonstrated in the 
[Mercurial Zipdoc extension](http://mercurial.selenic.com/wiki/ZipdocExtension)
by Andreas Gobell.

[OoXmlUnpack](https://bitbucket.org/htilabs/ooxmlunpack) is a similar program 
for Mercurial, written in c#, which also pretty-prints the xml files and adds 
some file handling features specific to Excel.

[callegar/Rezip](https://github.com/callegar/Rezip) should be compatible with
this Git filter but is written as a bash script to drive Info-ZIP zip/unzip 
executables.

[Zippey](https://bitbucket.org/sippey/zippey) is a similar method available 
for Git is but
it stores uncompressed data as custom records within the Git repository.
This format is not directly usable without the smudge filter so it is a less
portable option.

##Diffing
Sorry, you'll still need a custom diff for your particular format or 
[textconv](http://blog.martinfenner.org/2014/08/25/using-microsoft-word-with-git/)
if you want to see a human readable diff between revisions.

##Installation
This program requires Java JRE 6 or newer.
Store Rezip.class somewhere in your home directory, for example ~/bin.

Define the filter drivers in ~/.gitconfig :
```
git config --global --replace-all filter.zip.clean "java -cp ~/bin Rezip --store"
# optionally add smudge filter:
git config --global --add filter.zip.smudge "java -cp ~/bin Rezip"
```

Assign filter attributes to paths in .gitattributes:
```
# MS Office
*.docx  filter=zip
*.xlsx  filter=zip
*.pptx  filter=zip
# OpenOffice
*.odt   filter=zip
*.ods   filter=zip
*.odp   filter=zip
# Misc
*.mcdx  filter=zip
*.slx   filter=zip
```

As described in [gitattributes](http://git-scm.com/docs/gitattributes),
you may see unnecessary merge conflicts when you add attributes to a file that 
causes the repository format for that file to change.
To prevent this, Git can be told to run a virtual check-out and check-in of all
three stages of a file when resolving a three-way merge:
```
git config --add --bool merge.renormalize true
```

##Observations
The following are based on my experience in real-world cases.
Use at your own risk.
Your mileage may vary.
###Simulink
The packed repository with rezip was 54% of the size of the packed repository storing compressed zips.
###Powerpoint
I found that the loose objects stored without this filter were about 5% smaller
than the original file size (zlib on top of zip compression).
When using the rezip filter the loose objects were about 10% smaller than the 
original files since zlib could work more efficiently on uncompressed data.
The packed repository with Rezip was only 10% smaller than packed repository
storing compressed zips.
I think this unremarkable efficiency improvement is due to a large number of
png files in the presentation which were already stored without compression in the original pptx.

