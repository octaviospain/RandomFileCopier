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

package com.transgressoft.randomfilecopier;

import com.transgressoft.randomfilecopier.Utils.*;
import org.docopt.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardCopyOption.*;

/**
 * This class copies random files that are located in a folder and it
 * subsequent folders to a destination, supplying copyOptions such as limiting
 * the number of files, the total space to copy, or filtering the files by its extension.
 *
 * @version 0.1
 * @author Octavio Calleya
 *
 */
public class RandomFileCopier {


	/***************************		Command line usage		***************************/

	private static final String DOC = "Random File Copier.\n\n"+
			"Usage:\n"+
			"  RandomFileCopier <source_directory> <target_directory> <max_files> [-v] [-s=<maxbytes>] [-e=<extension>]...\n\n"+
			"Options:\n"+
			"  -h, --help  Show this help text.\n"+
			"  <max_files>  The maximum number of files.\n"+
			"  -v, --verbose  Show some extra information of the process.\n"+
			"  -e, --extension=<extension> Extension required to the file.\n"+
			"  -s, --space=<maxbytes> Max bytes to copy in the destination.\n";

	private static File sourceFile;
	private static File targetFile;
	private static String sourceString;
	private static String targetString;
	private static String[] extensionsCmd;
	private static int maxFilesCmd;
	private static boolean verboseCmd;
	private static long maxBytesCmd;

	public static void main(String[] args) throws IOException {
		parseArguments();

		if(validArguments()) {
			RandomFileCopier copier = new RandomFileCopier(sourceFile.toString(), targetFile.toString(), maxFilesCmd);
			copier.setVerbose(verboseCmd);
			copier.setFilterExtensions(extensionsCmd);
			if(maxBytesCmd > 0)
				copier.setMaxBytesToCopy(maxBytesCmd);
			copier.randomCopy();
		}
	}

	@SuppressWarnings("unchecked")
	private static void parseArguments(String... args) {
		Map<String, Object> opts = new Docopt(DOC).withVersion("Random File Copier 0.1").parse(args);
		sourceString = (String) opts.get("<source_directory>");
		targetString = (String) opts.get("<target_directory>");
		verboseCmd = (Boolean) opts.get("--verbose");

		List<String> extensionsList = (List<String>) opts.get("--extension");
		extensionsCmd = Arrays.stream(extensionsList.toArray()).map(s -> ((String) s).substring(1)).toArray(String[]::new);

		String maxBytesString = (String) opts.get("--space");
		maxBytesCmd = 0;
		if(maxBytesString != null)
			maxBytesCmd = Long.valueOf(maxBytesString.substring(1));

		String maxFilesString = (String) opts.get("<max_files>");
		try {
			maxFilesCmd = Integer.parseInt(maxFilesString);
		} catch (NumberFormatException e) {
			maxFilesCmd = -1;
		}
	}

	private static boolean validArguments() {
		boolean result = isValidSource();
		result &= isValidTarget();
		result &= isValidMaxFilesString();

		if(sourceFile.equals(targetFile)) {
			printUsage("Source and target directory are the same");
			result &= false;
		}
		else
			result &= true;

		return result;
	}

	private static boolean isValidSource() {
		boolean result = false;
		sourceFile = new File(sourceString);
		if(!sourceFile.exists())
			printUsage("Source path doesn't exist");
		else if(!sourceFile.isDirectory())
			printUsage("Source path is not a directory");
		else
			result = true;
		return result;
	}

	private static boolean isValidTarget() {
		boolean result = false;
		targetFile = new File(targetString);
		if(!targetFile.exists())
			targetFile.mkdir();
		if(targetFile.exists() && !targetFile.isDirectory())
			printUsage("Target path is not a directory");
		else
			result = true;
		return result;
	}

	private static boolean isValidMaxFilesString() {
		boolean res = false;
		if(maxFilesCmd >= 0 && maxFilesCmd <= Integer.MAX_VALUE)
			res = true;
		else
			printUsage("MaxFiles must be between 0 and "+Integer.MAX_VALUE+" inclusives");

		if(!res)
			printUsage("Invalid arguments");
		return res;
	}

	private static void printUsage(String detail) {
		System.out.println("ERROR: " + detail + "\n\n" + DOC);
	}

	/***************************		Object usage			***************************/

	private Path sourcePath;
	private Path targetPath;
	private int maxFiles;
	private long maxBytes;
	private long copiedBytes;
	private List<File> randomFiles;
	private ExtensionFileFilter filter;
	private boolean verbose;
	private Random rnd;
	private PrintStream out;
	private CopyOption[] copyOptions = new CopyOption[]{COPY_ATTRIBUTES};

	/**
	 * Constructor for a <tt>RandomFileCopier</tt> object
	 *
	 * @param source The source folder where the desired files are
	 * @param target The target folder to copy the files
	 * @param maxFiles The maximum number of files to copy. 0 will copy all the files
	 */
	public RandomFileCopier(String source, String target, int maxFiles) {
		sourcePath = Paths.get(source, "");
		targetPath = Paths.get(target.startsWith("/") ? target : "/"+ target, "");
		this.maxFiles = maxFiles;
		verbose = false;
		rnd = new Random();
		randomFiles = new ArrayList<>();
		out = System.out;
		filter = new ExtensionFileFilter();
		copiedBytes = 0;
		maxBytes = getUsableBytesInTarget(targetPath.toFile());
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

	/**
	 * Sets the extensions that the files must match to be copied
	 * @param extensions A String array containing the extensions without the initial dot '.'
	 */
	public void setFilterExtensions(String... extensions) {
		filter.setExtensionsToFilter(extensions);
	}

	public String[] getFilterExtensions() {
		return filter.getExtensionsToFilter();
	}

	/**
	 * Sets the maximum number of bytes that should be copied to the destination.
	 * @param maxBytesToCopy The maximum number of bytes
	 */
	public void setMaxBytesToCopy(long maxBytesToCopy) {
		if(maxBytesToCopy < getUsableBytesInTarget(targetPath.toFile()))
			maxBytes = maxBytesToCopy;
		else
			maxBytes = getUsableBytesInTarget(targetPath.toFile());
	}

	public long getMaxBytesToCopy() {
		return maxBytes <= getUsableBytesInTarget(targetPath.toFile()) ? maxBytes : getUsableBytesInTarget(targetPath.toFile());
	}

	private long getUsableBytesInTarget(File targetDestination) {
		File root = targetDestination;
		while(root.getParentFile() != null)
			root = root.getParentFile();
		return root.getUsableSpace();
	}

	/**
	 * Sets if the application should print to the standard or given output some useful info
	 * @param verbose
	 */
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
		getRandomFilesInFolderTree();
		copyRandomFilesToTarget();
	}

	/**
	 * Scans the source folder and its subfolders to collect the files satisfying
	 * the given conditions and selects randomly a certain number of them
	 */
	private void getRandomFilesInFolderTree() {
		randomFiles.clear();

		out.println("Scanning source directory... ");
		List<File> allFiles = Utils.getAllFilesInFolder(sourcePath.toFile(), filter, 0);
		long allFilesBytes = allFiles.stream().mapToLong(File::length).sum();
		out.println(allFiles.size() + " files found");

		if(maxFiles >= allFiles.size() || maxFiles == 0) {		// if all files should be copied regarding the maxFiles constraint
			if(allFilesBytes > getMaxBytesToCopy())				// checks the maximum bytes required to be copied
				selectRandomFilesRegardingBytes(allFiles, allFilesBytes);
		}
		else													// if a certain number of files should be copied
			selectRandomFilesRegardingBytesAndNumber(allFiles);
	}

	private void selectRandomFilesRegardingBytes(List<File> files, long allFilesBytes){
		int selectedIndex;
		long fileLength;
		long bytesToRegard = allFilesBytes;
		while(allFilesBytes > getMaxBytesToCopy()) {
			selectedIndex = rnd.nextInt(files.size());
			files.remove(selectedIndex);

			fileLength = files.get(selectedIndex).length();
			bytesToRegard -= fileLength;
		}
		copiedBytes = bytesToRegard;
		randomFiles.addAll(files);
	}

	private void selectRandomFilesRegardingBytesAndNumber(List<File> files) {
		while (randomFiles.size() < maxFiles && !files.isEmpty()) {
			File selectedFile = files.get(rnd.nextInt(files.size()));
			long fileLength = selectedFile.length();

			if(fileLength + copiedBytes <= getMaxBytesToCopy()) {		// checking the maximum bytes to be copied
				randomFiles.add(selectedFile);
				copiedBytes += fileLength;
			}
			files.remove(selectedFile);
		}
	}

	/**
	 * Copies the randomly selected files to the target destination
	 * Renames duplicated files to ensure that files with the same name are not overwritten
	 *
	 * @throws IOException
	 */
	private void copyRandomFilesToTarget() throws IOException {
		out.println("Copying files to target directory... ");
		for(File randomFileToCopy: randomFiles)
			if(!Thread.currentThread().isInterrupted())
				copyFile(randomFileToCopy);
		out.println("Done. " + Utils.byteSizeString(copiedBytes, 4) + " copied");
	}

	private void copyFile(File fileToCopy) throws IOException {
		Path filePath = fileToCopy.toPath();
		String path = filePath.subpath(filePath.getNameCount()-3, filePath.getNameCount()).toString();
		Files.copy(filePath, targetPath.resolve(Utils.ensureFileNameOnPath(targetPath, fileToCopy.getName())), copyOptions);
		if(verbose)
			out.println("Copied " + ".../" + path + " [" + Utils.byteSizeString(fileToCopy.length(), 2) + "]");
	}
}