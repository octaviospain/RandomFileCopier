[![Build Status](https://travis-ci.org/octaviospain/RandomFileCopier.svg?branch=master)](https://travis-ci.org/octaviospain/RandomFileCopier)
[![codecov](https://codecov.io/gh/octaviospain/RandomFileCopier/branch/master/graph/badge.svg)](https://codecov.io/gh/octaviospain/RandomFileCopier)
[![license](https://img.shields.io/badge/license-apache%202-brightgreen.svg)](https://github.com/octaviospain/TimecodeString/blob/master/LICENSE.txt)

# RandomFileCopier
A java class that copies random files that are located in a folder and it subsequent folders to a
destination, supplying options such as limiting the number of files, the total space in bytes to copy,
or filtering the files by its extension; ensuring that the files with the same name are not overwritten by renaming them.

## Features
* Limits the number of files to copy
* Limits the bytes to be copied into the destinaion
* Filter the available files by several extensions

### To be done
* Include hidden files
* Option to copy the files with the folders where they were
* Filter the files by matching some string in the name

# Usage
You can use the RandomFileCopier by two ways:

1. Instantiating the `RandomFileCopier` class in your java project

```java
int maxFilesToCopy = 25;
int maxBytes = 50000;
String[] extensions = new String[]{"mp3", "txt", "xml", "pdf"};

RandomFileCopier copier = new RandomFileCopier("/source_folder/", "/target_folder/", maxFilesToCopy);
copier.setFilterExtensions(extensions);
copier.setVerbose(true);
copier.setMaxBytesToCopy(maxBytes);
copier.randomCopy();
// copier.abort()
```

2. Using it as a command line program with the packaged `.jar` (available in
[releases](https://github.com/octaviospain/RandomFileCopier/releases)) passing arguments to it
(thanks to [docopt](https://github.com/docopt/docopt.java)) with the following usage:

```
Usage:
    java -jar RandomFileCopier.jar <source_directory> <target_directory> <max_files> [-v] [-s=<maxbytes>] [-e=<extension>]...

Options:
    -h, --help                     Show this help text.
    <max_files>                    The maximum number of files.
    -v, --verbose                  Show some extra information of the process.
    -e, --extension=<extension>    A required extension of a file to be copied.
    -s, --space=<maxbytes>         The maximum bytes to copy in the destination.
```

Example:

```
java -jar RandomFileCopier-0.2.3.jar /source /target 10 -v -s=5000 -e=pdf -e=txt  
```

Would print:
```
Scanning source directory...
XY files found
Copying files to the destination directory...
Copied .../folder/under/source/file0.txt [31415 B]
Copied .../folder/under/source/file1.txt [31415 B]
Copied .../folder/under/source/file2.txt [31415 B]
Copied .../folder/under/source/file3.pdf [31415 B]
Copied .../folder/under/source/file4.pdf [31416 B]
Done. 5 files, xyz B copied
```