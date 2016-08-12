package com.transgressoft.randomfilecopier;

import com.transgressoft.util.*;
import com.transgressoft.util.TransgressoftUtils.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardCopyOption.*;

/**
 * This class copies random files that are located in a folder and it
 * subsequent folders to a destination, supplying copyOptions such as limiting
 * the number of files, the total space to copy, or filtering the files by its extension.
 *
 * @author Octavio Calleya
 * @version 0.2.3
 */
public class RandomFileCopier {

	private Path sourcePath;
	private Path destinationPath;
	private int maxFilesToCopy;
	private long maxBytesToCopy;
	private long filesInSourceBytes;
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
	public RandomFileCopier(String sourcePath, String destinationPath, int maxFilesToCopy, PrintStream output) {
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
	public RandomFileCopier(String source, String destination, int maxFilesToCopy) {
		outStream = System.out;
		sourcePath = Paths.get(source, "");
		destinationPath = Paths.get(destination, "");
		this.maxFilesToCopy = maxFilesToCopy;
		verbose = false;
		random = new Random();
		randomSelectedFiles = new ArrayList<>();
		filesInSource = new ArrayList<>();
		filter = new ExtensionFileFilter();
		copiedBytes = 0;
		maxBytesToCopy = getUsableBytesInDestination(destinationPath.toFile());
	}

	/**
	 * Returns the amount of bytes that are usable in the destination path
	 *
	 * @param destinationFolder The {@link File} of the destination folder
	 *
	 * @return
	 */
	private long getUsableBytesInDestination(File destinationFolder) {
		File root = destinationFolder;
		while (root.getParentFile() != null)
			root = root.getParentFile();
		return root.getUsableSpace();
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

	public long getMaxBytesToCopy() {
		boolean areAvailableBytes = maxBytesToCopy <= getUsableBytesInDestination(destinationPath.toFile());
		return areAvailableBytes ? maxBytesToCopy : getUsableBytesInDestination(destinationPath.toFile());
	}

	/**
	 * Sets the maximum number of bytes that should be copied to the destination.
	 *
	 * @param maxBytesToCopy The maximum number of bytes
	 */
	public void setMaxBytesToCopy(long maxBytesToCopy) {
		if (maxBytesToCopy < getUsableBytesInDestination(destinationPath.toFile()))
			this.maxBytesToCopy = maxBytesToCopy;
		else
			this.maxBytesToCopy = getUsableBytesInDestination(destinationPath.toFile());
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
		filesInSourceBytes = 0;
		getRandomFilesInFolderTree();
		if (! filesInSource.isEmpty())
			copyRandomFilesToDestination();
	}

	/**
	 * Scans the source folder and its subfolders to collect the files satisfying
	 * the given conditions and selects randomly a certain number of them
	 */
	private void getRandomFilesInFolderTree() {
		randomSelectedFiles.clear();

		outStream.println("Scanning source directory...");
		filesInSource = TransgressoftUtils.getAllFilesInFolder(sourcePath.toFile(), filter, 0);
		filesInSourceBytes = filesInSource.stream().mapToLong(File::length).sum();

		if (filesInSource.isEmpty()) {
			outStream.println("No files found with the given constraints");
		}
		else {
			outStream.println(Integer.toString(filesInSource.size()) + " files found");

			if (filesInSource.size() < maxFilesToCopy || maxFilesToCopy == 0)
				selectRandomFilesLimitingBytes();
			else
				selectRandomFilesLimitingBytesAndNumber();
		}
	}

	private void selectRandomFilesLimitingBytes() {
		int selectedIndex;
		long fileLength;
		long remainingBytesInSource = filesInSourceBytes;
		while (remainingBytesInSource > getMaxBytesToCopy()) {
			selectedIndex = random.nextInt(filesInSource.size());
			fileLength = filesInSource.get(selectedIndex).length();

			filesInSource.remove(selectedIndex);
			remainingBytesInSource -= fileLength;
		}
		copiedBytes = remainingBytesInSource;
		randomSelectedFiles.addAll(filesInSource);
	}

	private void selectRandomFilesLimitingBytesAndNumber() {
		while (randomSelectedFiles.size() < maxFilesToCopy) {
			File selectedFile = filesInSource.get(random.nextInt(filesInSource.size()));
			long fileLength = selectedFile.length();

			if (fileLength + copiedBytes <= getMaxBytesToCopy()) {        // checking the maximum bytes to be copied
				randomSelectedFiles.add(selectedFile);
				copiedBytes += fileLength;
			}
			filesInSource.remove(selectedFile);
		}
	}

	/**
	 * Copies the randomly selected files to the destination path
	 * Renames duplicated files to ensure that files with the same name are not overwritten
	 *
	 * @throws IOException
	 */
	private void copyRandomFilesToDestination() throws IOException {
		outStream.println("Copying files to the destination directory...");

		for (File randomFileToCopy : randomSelectedFiles)
			copyFile(randomFileToCopy);

		outStream.println("Done. " + TransgressoftUtils.byteSizeString(copiedBytes, 4) + " copied");
	}

	private void copyFile(File fileToCopy) throws IOException {
		Path filePath = fileToCopy.toPath();
		String path = filePath.subpath(filePath.getNameCount() - 3, filePath.getNameCount()).toString();
		String ensuredFileName = TransgressoftUtils.ensureFileNameOnPath(destinationPath, fileToCopy.getName());
		Files.copy(filePath, destinationPath.resolve(ensuredFileName), copyOptions);
		if (verbose) {
			String sizeString = TransgressoftUtils.byteSizeString(fileToCopy.length(), 2);
			outStream.println("Copied " + ".../" + path + " [" + sizeString + "]");
		}
	}
}