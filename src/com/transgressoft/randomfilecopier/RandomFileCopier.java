/******************************************************************************
 * Copyright 2016-2018 Octavio Calleya                                        *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 * http://www.apache.org/licenses/LICENSE-2.0                                 *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package com.transgressoft.randomfilecopier;

import com.transgressoft.commons.util.*;

import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardCopyOption.*;

/**
 * This class copies random files that are located in a folder and it
 * subsequent folders to a destination, supplying copy options such as limiting
 * the number of files, the total space to copy, or filtering the files by its extension.
 *
 * @author Octavio Calleya
 * @version 0.2.6
 */
public class RandomFileCopier {

    private Path sourcePath;
    private Path destinationPath;
    private int maxFilesToCopy;
    private long maxBytesToCopy;
    private long copiedBytes;
    private List<File> filesInSource;
    private List<File> randomSelectedFiles;
    private ExtensionFileFilter filter;
    private boolean verbose;
    private Random random;
    private PrintStream outStream;
    private CopyOption[] copyOptions = new CopyOption[]{COPY_ATTRIBUTES};

    /**
     * Constructor for a <tt>RandomFileCopier</tt> object
     *
     * @param sourcePath      The source folder where the desired files are
     * @param destinationPath The destination folder to copy the files
     * @param maxFilesToCopy  The maximum number of files to copy. 0 will copy all the files
     * @param output          The OutputStream where the log messages will be printed
     */
    public RandomFileCopier(Path sourcePath, Path destinationPath, int maxFilesToCopy, PrintStream output) {
        this(sourcePath, destinationPath, maxFilesToCopy);
        outStream = output;
    }

    /**
     * Constructor for a <tt>RandomFileCopier</tt> object
     *
     * @param source         The source folder where the desired files are
     * @param destination    The destination folder to copy the files
     * @param maxFilesToCopy The maximum number of files to copy. 0 will copy all the files
     */
    public RandomFileCopier(Path source, Path destination, int maxFilesToCopy) {
        outStream = System.out;
        sourcePath = source;
        destinationPath = destination;
        this.maxFilesToCopy = maxFilesToCopy;
        verbose = false;
        random = new Random();
        randomSelectedFiles = new ArrayList<>();
        filesInSource = new ArrayList<>();
        filter = new ExtensionFileFilter();
        copiedBytes = 0;
        maxBytesToCopy = destinationPath.toFile().getUsableSpace();
    }

    public String[] getFilterExtensions() {
        return filter.getExtensionsToFilter();
    }

    /**
     * Sets the extensions that the files must match to be copied
     *
     * @param extensions A String array containing the extensions without the initial dot '.'
     */
    public void setFilterExtensions(String... extensions) {
        filter.setExtensionsToFilter(extensions);
    }

    /**
     * Sets if the application should print to the standard or given output some useful info
     *
     * @param verbose
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Copies random files from a source path to a destination path
     * up to a maximum number satisfying a file filter condition
     *
     * @throws IOException
     */
    public void randomCopy() throws IOException {
        random.setSeed(System.currentTimeMillis());
        filesInSource.clear();
        randomSelectedFiles.clear();
        copiedBytes = 0;
        getRandomFilesInFolderTree();
        if (! randomSelectedFiles.isEmpty())
            copyRandomFilesToDestination();
    }

    /**
     * Scans the source folder and its subfolders to collect the files satisfying
     * the given conditions and selects randomly a certain number of them
     */
    private void getRandomFilesInFolderTree() {
        randomSelectedFiles.clear();

        if (outStream != null)
            outStream.println("Scanning source directory...");
        filesInSource = new FilesInDirectory(sourcePath.toFile()).filteredAndBounded(filter, 0);

        if (filesInSource.isEmpty()) {
            if (outStream != null)
                outStream.println("No files found with the given constraints");
        }
        else {
            if (outStream != null)
                outStream.println(Integer.toString(filesInSource.size()) + " files found");
            selectedFilesLimitingBytesAndNumber();
        }
    }

    /**
     * Copies the randomly selected files to the destination path
     * Renames duplicated files to ensure that files with the same name are not overwritten
     *
     * @throws IOException
     */
    private void copyRandomFilesToDestination() throws IOException {
        if (outStream != null)
            outStream.println("Copying files to the destination directory...");

        for (File randomFileToCopy : randomSelectedFiles)
            copyFile(randomFileToCopy);

        int numFilesCopied = randomSelectedFiles.size();
        ByteSizeRepresentation byteSizeRepresentation = new ByteSizeRepresentation(copiedBytes);
        String sizeCopied = byteSizeRepresentation.withMaximumDecimals(4, RoundingMode.CEILING);
        if (outStream != null)
            outStream.println("Done. " + numFilesCopied + " files, " + sizeCopied + " copied");
    }

    private void selectedFilesLimitingBytesAndNumber() {
        while (continueFileSelection()) {
            File randomSourceFile = filesInSource.get(random.nextInt(filesInSource.size()));
            long fileLength = randomSourceFile.length();

            if (fileLength <= getMaxBytesToCopy() - copiedBytes) {
                randomSelectedFiles.add(randomSourceFile);
                copiedBytes += fileLength;
            }

            filesInSource.remove(randomSourceFile);
        }
    }

    private void copyFile(File fileToCopy) throws IOException {
        Path filePath = fileToCopy.toPath();
        String path = filePath.subpath(filePath.getNameCount() - 3, filePath.getNameCount()).toString();
        String ensuredFileName = ensuredFileNameOnPath(destinationPath, fileToCopy.getName());
        Files.copy(filePath, destinationPath.resolve(ensuredFileName), copyOptions);
        if (verbose) {
            ByteSizeRepresentation byteSizeRepresentation = new ByteSizeRepresentation(fileToCopy.length());
            String sizeString = byteSizeRepresentation.withMaximumDecimals(2, RoundingMode.CEILING);
            if (outStream != null)
                outStream.println("Copied " + ".../" + path + " [" + sizeString + "]");
        }
    }

    /**
     * Ensures that the file name given is unique in the target directory, appending
     * (1), (2)... (n+1) to the file name in case it already exists
     *
     * @param fileName   The string of the file name
     * @param targetPath The path to check if there is a file with the name equals <tt>fileName</tt>
     *
     * @return The modified string
     */
    public String ensuredFileNameOnPath(Path targetPath, String fileName) {
        String newName = fileName;
        if (targetPath.resolve(fileName).toFile().exists()) {
            int pos = fileName.lastIndexOf('.');
            newName = fileName.substring(0, pos) + "(1)." + fileName.substring(pos + 1);
        }
        while (targetPath.resolve(newName).toFile().exists()) {
            int posL = newName.lastIndexOf('(');
            int posR = newName.lastIndexOf(')');
            int num = Integer.parseInt(newName.substring(posL + 1, posR));
            newName = newName.substring(0, posL + 1) + ++ num + newName.substring(posR);
        }
        return newName;
    }

    private boolean continueFileSelection() {
        boolean continueSelection;
        if (filesInSource.size() < maxFilesToCopy || maxFilesToCopy == 0)
            continueSelection = copiedBytes < getMaxBytesToCopy() && ! filesInSource.isEmpty();
        else
            continueSelection = randomSelectedFiles.size() < maxFilesToCopy;
        return continueSelection;
    }

    public long getMaxBytesToCopy() {
        boolean areAvailableBytes = maxBytesToCopy <= destinationPath.toFile().getUsableSpace();
        return areAvailableBytes ? maxBytesToCopy : destinationPath.toFile().getUsableSpace();
    }

    /**
     * Sets the maximum number of bytes that should be copied to the destination.
     *
     * @param maxBytesToCopy The maximum number of bytes
     */
    public void setMaxBytesToCopy(long maxBytesToCopy) {
        if (maxBytesToCopy < destinationPath.toFile().getUsableSpace())
            this.maxBytesToCopy = maxBytesToCopy;
        else
            this.maxBytesToCopy = destinationPath.toFile().getUsableSpace();
    }
}