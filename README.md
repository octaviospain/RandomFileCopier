# RandomFileCopier
A java class that copies random files that are located in a folder and it subsequent folders to a destination, supplying options such as limiting the number of files, the total space in bytes to copy, or filtering the files by its extension; ensuring that the files with the same name are not overwritten by renaming them.
## Features
* Limits the number of files to copy
* Limits the bytes to be copied into the destinaion
* Filter the available files by several extensions

### Coming soon
* Include hidden files
* Option to copy the files with the folders where they were
* Filter the files by matching some string in the name

# Usage
You can use the RandomFileCopier by two ways:
1. Instanciating the `RandomFileCopier.java` class in your java project
2. Using it as a command line program with the packaged `.jar` (available in [releases](https://github.com/octaviospain/RandomFileCopier/releases)) passing arguments to it (thanks to [docopt](https://github.com/docopt/docopt.java)) with the following usage:

`java -jar RandomFileCopier <source_directory> <target_directory> <max_files> [-v] [-s=<maxbytes>] [-e=<extension>]...`

## License
RandomFileCopier is licensed under Apache License 2.0