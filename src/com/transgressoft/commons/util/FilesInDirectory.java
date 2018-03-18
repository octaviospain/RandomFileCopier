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

package com.transgressoft.commons.util;

import java.io.*;
import java.util.*;

/**
 * Retrieves a {@link List} of files that are in a directory and any of the subdirectories
 * in that directory satisfying a condition specified by a {@link FileFilter}.
 * If {@code maxFilesRequired} is 0 all the files will be retrieved.
 *
 * @author Octavio Calleya
 * @version 0.2.6
 */
public class FilesInDirectory {

    private File rootDirectory;
    private int maxFilesRequired;
    private FileFilter filter;
    private List<File> files;

    /**
     * Default constructor
     *
     * @param rootDirectory The directory from within to find the files
     */
    public FilesInDirectory(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public List<File> filtered(FileFilter filter) {
        return filteredAndBounded(filter, 0);
    }

    /**
     * Retrieves a {@link List} with at most {@code maxFilesRequired} files that are in a directory or
     * any of the subdirectories in that directory satisfying a condition specified by a {@link FileFilter}.
     * If {@code maxFilesRequired} is 0 all the files will be retrieved.
     *
     * @param filter           The {@code FileFilter} condition
     * @param maxFilesRequired Maximum number of files required. 0 means no maximum
     *
     * @return The list containing all the files
     *
     * @throws IllegalArgumentException Thrown if {@code maxFilesRequired} argument is less than zero
     */
    public List<File> filteredAndBounded(FileFilter filter, int maxFilesRequired) {
        this.filter = filter;
        this.maxFilesRequired = maxFilesRequired;
        files = new ArrayList<>();
        if (! Thread.currentThread().isInterrupted()) {
            if (maxFilesRequired < 0)
                throw new IllegalArgumentException("maxFilesRequired argument less than zero");
            if (rootDirectory == null || filter == null)
                throw new IllegalArgumentException("directory or filter null");
            if (! rootDirectory.exists() || ! rootDirectory.isDirectory())
                throw new IllegalArgumentException("Provided root directory is a file or does not exist");

            int remainingFiles = addFilesRegardingMaxRequired(rootDirectory.listFiles(filter));

            if (maxFilesRequired == 0 || remainingFiles > 0) {
                File[] rootSubdirectories = rootDirectory.listFiles(File::isDirectory);
                addFilesFromDirectories(rootSubdirectories, remainingFiles);
            }
        }
        return files;
    }

    /**
     * Add files to the {@link List} regarding the maximum required.
     * <ul>
     * <li>
     * If it's 0, all files are added.
     * </li>
     * <li>
     * If it's greater than the actual number of files, all files are added too.
     * </li>
     * <li>
     * If it's less than the actual number of files, the required number
     * of files are added
     * </li>
     * </ul>
     *
     * @param subFiles A {@code File} {@code Array} to add to the collection
     *
     * @return The number of files that were not added from the {@code Array}
     */
    private int addFilesRegardingMaxRequired(File[] subFiles) {
        int remainingFiles = maxFilesRequired;
        if (maxFilesRequired == 0)                              // No max = add all files
            files.addAll(Arrays.asList(subFiles));
        else if (maxFilesRequired < subFiles.length) {          // There are more valid files than the required
            files.addAll(Arrays.asList(Arrays.copyOfRange(subFiles, 0, maxFilesRequired)));
            remainingFiles -= files.size();                     // Zero files remaining in the directory
        }
        else if (subFiles.length > 0) {
            files.addAll(Arrays.asList(subFiles));             // Add all valid files
            remainingFiles -= files.size();
        }
        return remainingFiles;
    }

    /**
     * Adds files to the {@link List} from several directories regarding the remaining required
     *
     * @param directories    The folders where the files are
     * @param remainingFiles The remaining number of files to add
     */
    private void addFilesFromDirectories(File[] directories, int remainingFiles) {
        int subdirectoriesCount = 0;
        int remaining = remainingFiles;
        while ((subdirectoriesCount < directories.length) && ! Thread.currentThread().isInterrupted()) {
            File subdirectory = directories[subdirectoriesCount++];
            List<File> subdirectoryFiles = new FilesInDirectory(subdirectory).filteredAndBounded(filter, remaining);
            files.addAll(subdirectoryFiles);
            if (remaining > 0)
                remaining = maxFilesRequired - files.size();
            if (maxFilesRequired > 0 && remaining == 0)
                break;
        }
    }
}