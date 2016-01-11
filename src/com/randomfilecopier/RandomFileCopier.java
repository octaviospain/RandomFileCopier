/**
 * Copyright 2016 Octavio Calleya
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.randomfilecopier;

import static java.nio.file.StandardCopyOption.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * This class copies random files that are located in a folder and it
 * subsequent folders to a destination, supplying options such as limiting
 * the number of files, the total space to copy, or filtering the files by its extension.
 * 
 * @version 0.1
 * @author Octavio Calleya
 *
 */
public class RandomFileCopier {
	
	
	private File sourceDir, targetDir;
	private int maxFiles;
	private long maxBytes;
	private List<File> randomFiles;
	private FileFilter filter;
	private boolean verbose;
	private String[] filterExtensions;
	private Random rnd;
	private PrintStream out;
	
	/**
	 * Constructor for a <tt>RandomFileCopier</tt> object 
	 * 
	 * @param sourcePath The source folder where the desired files are
	 * @param targetPath The target folder to copy the files
	 * @param maxFiles The maximum number of files to copy. 0 will copy all the files
	 */
	public RandomFileCopier(String sourcePath, String targetPath, int maxFiles) {
		sourceDir = new File(sourcePath);
		targetDir = new File(targetPath);
		this.maxFiles = maxFiles;
		verbose = false;
		rnd = new Random();
		randomFiles = new ArrayList<>();
		out = System.out;
		maxBytes = targetDir.getUsableSpace();
	}
	
	/**
	 * Constructor for a <tt>RandomFileCopier</tt> object
	 * 
	 * @param sourcePath The source folder where the desired files are
	 * @param targetPath The target folder to copy the files
	 * @param maxFiles The maximum number of files to copy. 0 will copy all the files
	 * @param output The OutputStream where the log messages will be printed
	 */
	public RandomFileCopier(String sourcePath, String targetPath, int maxFiles, PrintStream output) {
		this(sourcePath, targetPath, maxFiles);
		out = output;
	}
	
	public void setFilterExtensions(String... extensions) {
		filterExtensions = extensions;
	}
	
	public String[] getFilterExtensions() {
		return this.filterExtensions;
	}
	
	public void setMaxBytesToCopy(long maxBytesToCopy) {
		if(maxBytesToCopy < targetDir.getUsableSpace())
			maxBytes = maxBytesToCopy;
		else
			maxBytes = targetDir.getUsableSpace();
	}
	
	public long getMaxBytesToCopy() {
		return maxBytes <= targetDir.getUsableSpace() ? maxBytes : targetDir.getUsableSpace();
	}
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	/**
	 * Copies random files from a source path to a target path
	 * up to a maximum number satisfying a file filter condition
	 * 
	 * @throws IOException 
	 */
	public void randomCopy() throws IOException {		
		buildFilter();
		getRandomFilesInFolderTree();
		copyFiles();
	}
	
	/**
	 * Creates the file filter taking into account the given conditions
	 */
	private void buildFilter() {
		filter = file -> {
			boolean res = false;
			if(!file.isDirectory() && !file.isHidden()) {
				int pos = file.getName().lastIndexOf(".");
				if(pos != -1) {
					String fileExtension = file.getName().substring(pos);
					if(filterExtensions.length == 0)
						res =  true;
					else
						for(String requiredExtension: filterExtensions)
							if(fileExtension.equals(requiredExtension))
								res = true;
				}	
			}	
			return res;
		};
	}
	
	/**
	 * Scans the source folder and its subfolders to collect the files satisfying
	 * the filter conditions and selects randomly a certain number of them
	 */
	private void getRandomFilesInFolderTree() {
		randomFiles.clear();
		out.println("Scanning source directory... ");
		List<File> allFiles = getAllFilesInFolder(sourceDir, filter, 0);
		long allFilesBytes = allFiles.stream().mapToLong(File::length).sum();
		out.println(allFiles.size() + " files found");
		int selectedIndex;
		if(maxFiles >= allFiles.size() || maxFiles == 0) {
			if(allFilesBytes > getMaxBytesToCopy())
				while(allFilesBytes > getMaxBytesToCopy()) {
					selectedIndex = rnd.nextInt(allFiles.size());
					allFilesBytes -= allFiles.get(selectedIndex).length();
					allFiles.remove(selectedIndex);
				}
			randomFiles.addAll(allFiles);
		} else {
			long currentBytes = 0;
			while (randomFiles.size() < maxFiles && currentBytes <= getMaxBytesToCopy()) {
				selectedIndex = rnd.nextInt(allFiles.size());
				randomFiles.add(allFiles.get(selectedIndex));
				currentBytes += allFiles.get(selectedIndex).length();						
				allFiles.remove(selectedIndex);
			}
		}
	}
	
	/**
	 * Copies the randomly selected files to the target destination
	 * 
	 * @throws IOException 
	 */
	private void copyFiles() throws IOException {
		CopyOption[] options = new CopyOption[]{COPY_ATTRIBUTES, REPLACE_EXISTING};
		out.println("Copying files to target directory... ");
		for(File f: randomFiles)
			if(!Thread.currentThread().isInterrupted())	{
				Path filePath = f.toPath();
				Files.copy(filePath, targetDir.toPath().resolve(f.getName()), options);
				if(verbose)
					out.println("Copied "+".../"+filePath.subpath(filePath.getNameCount()-3, filePath.getNameCount()));
			}
		out.println("Done");
	}

	/**
	 * Retrieves a list with at most <tt>maxFiles</tt> files that are in a folder or
	 * any of the subfolders in that folder satisfying a condition.
	 * If <tt>maxFilesRequired</tt> is 0 all the files will be retrieved.
	 * 
	 * @param rootFolder The folder from within to find the files
	 * @param filter The FileFilter condition
	 * @param maxFilesRequired Maximun number of files in the List. 0 indicates no maximum
	 * @return The list containing all the files
	 * @throws IllegalArgumentException Thrown if <tt>maxFilesRequired</tt> argument is less than zero
	 */
	private List<File> getAllFilesInFolder(File rootFolder, FileFilter filter, int maxFilesRequired) throws IllegalArgumentException {
		List<File> finalFiles = new ArrayList<>();
		if(!Thread.currentThread().isInterrupted()) {
			if(maxFilesRequired < 0)
				throw new IllegalArgumentException("maxFilesRequired argument less than zero");
			if(rootFolder == null || filter == null)
				throw new IllegalArgumentException("folder or filter null");
			if(!rootFolder.exists() || !rootFolder.isDirectory())
				throw new IllegalArgumentException("rootFolder argument is not a directory");
			File[] subFiles = rootFolder.listFiles(filter);
			int remainingFiles = maxFilesRequired;
			if(maxFilesRequired == 0)	// No max = add all files
				finalFiles.addAll(Arrays.asList(subFiles));
			else if(maxFilesRequired < subFiles.length) {	// There are more valid files than the required
					finalFiles.addAll(Arrays.asList(Arrays.copyOfRange(subFiles, 0, maxFilesRequired)));
					remainingFiles -= finalFiles.size();		// Zero files remaining
				}
			else if (subFiles.length > 0) {
						finalFiles.addAll(Arrays.asList(subFiles));	// Add all valid files
						remainingFiles -= finalFiles.size();		// If remainingFiles == 0, end;
					}
			
			if(maxFilesRequired == 0 || remainingFiles > 0) {
				File[] rootSubFolders = rootFolder.listFiles(file -> {return file.isDirectory();});
				int sbFldrsCount = 0;
				while((sbFldrsCount < rootSubFolders.length) && !Thread.currentThread().isInterrupted()) {
					File subFolder = rootSubFolders[sbFldrsCount++];
					List<File> subFolderFiles = getAllFilesInFolder(subFolder, filter, remainingFiles);
					finalFiles.addAll(subFolderFiles);
					if(remainingFiles > 0)
						remainingFiles = maxFilesRequired - finalFiles.size();
					if(maxFilesRequired > 0 && remainingFiles == 0)
						break;
				}
			}
		}
		return finalFiles;
	}
}