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

package com.transgressoft.util;

import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;

/**
 * Class that does some useful operations with files, directories, strings
 * or other operations utilities to be used for the application
 *
 * @author Octavio Calleya
 *
 */
public class TransgressoftUtils {

	private TransgressoftUtils() {}

	/**
	 * Retrieves a {@link List} with at most <tt>maxFiles</tt> files that are in a folder or
	 * any of the subfolders in that folder satisfying a condition.
	 * If <tt>maxFilesRequired</tt> is 0 all the files will be retrieved.
	 *
	 * @param rootFolder The folder from within to find the files
	 * @param filter The {@link FileFilter} condition
	 * @param maxFilesRequired Maximun number of files in the List. 0 indicates no maximum
	 * @return The list containing all the files
	 * @throws IllegalArgumentException Thrown if <tt>maxFilesRequired</tt> argument is less than zero
	 */
	public static List<File> getAllFilesInFolder(File rootFolder, FileFilter filter, int maxFilesRequired) {
		if(maxFilesRequired < 0)
			throw new IllegalArgumentException("maxFilesRequired argument less than zero");
		if(rootFolder == null || filter == null)
			throw new IllegalArgumentException("folder or filter null");
		if(!rootFolder.exists() || !rootFolder.isDirectory())
			throw new IllegalArgumentException("rootFolder argument is not a directory");

		List<File> finalFiles = new ArrayList<>();
		int remainingFiles = maxFilesRequired;
		if(!Thread.currentThread().isInterrupted()) {

			File[] filesInFolder = rootFolder.listFiles(filter);
			int numFilesInFolder = filesInFolder.length;

			if(maxFilesRequired == 0)
				finalFiles.addAll(Arrays.asList(filesInFolder));
			else if(maxFilesRequired < numFilesInFolder) {

				Random rnd = new Random();
				List<Integer> selectedFiles = new ArrayList<>();
				while(remainingFiles <= maxFilesRequired) {

					int randomInt = rnd.nextInt(numFilesInFolder);
					if(!selectedFiles.contains(randomInt)) {
						finalFiles.add(filesInFolder[randomInt]);
						selectedFiles.add(randomInt);
						remainingFiles--;
					}
				}
			} else if(numFilesInFolder > 0) {
				finalFiles.addAll(Arrays.asList(filesInFolder));
				remainingFiles -= finalFiles.size();
			}

			if(maxFilesRequired == 0 || remainingFiles > 0) {

				File[] subfolders = rootFolder.listFiles(File::isDirectory);
				int numSubfolders = subfolders.length;
				int foldersCount = 0;
				Random rnd = new Random();
				List<Integer> selectedFolders = new ArrayList<>();
				while((foldersCount < numSubfolders) && !Thread.currentThread().isInterrupted()) {

					int randomInt = rnd.nextInt(numSubfolders);
					if(!selectedFolders.contains(randomInt)) {

						File subfolder = subfolders[randomInt];
						List<File> filesInSubfolders = getAllFilesInFolder(subfolder, filter, remainingFiles);
						finalFiles.addAll(filesInSubfolders);
						foldersCount++;
					}

					if(remainingFiles > 0)
						remainingFiles = maxFilesRequired - finalFiles.size();
					if(maxFilesRequired > 0 && remainingFiles == 0)
						break;
				}
			}
		}
		return finalFiles;
	}

	/**
	 * Returns a {@link String} representing the given <tt>bytes</tt>, with a textual representation
	 * depending if the given amount can be represented as KB, MB, GB or TB
	 *
	 * @param bytes The <tt>bytes</tt> to be represented
	 * @return The <tt>String</tt> that represents the given bytes
	 * @throws IllegalArgumentException Thrown if <tt>bytes</tt> is negative
	 */
	public static String byteSizeString(long bytes) {
		if(bytes < 0)
			throw new IllegalArgumentException("Given bytes can't be less than zero");

		String sizeText;
		String[] bytesUnits = {"B", "KB", "MB", "GB", "TB"};
		long bytesAmount = bytes;
		short binRemainder;
		float decRemainder = 0;
		int u;
		for(u = 0; bytesAmount > 1024 && u < bytesUnits.length; u++) {
			bytesAmount /= 1024;
			binRemainder = (short) (bytesAmount % 1024);
			decRemainder += Float.valueOf((float) binRemainder / 1024);
		}
		String remainderStr = String.format("%f", decRemainder).substring(2);
		sizeText = bytesAmount + (remainderStr.equals("0") ? "" : ","+remainderStr) + " " + bytesUnits[u];
		return sizeText;
	}

	/**
	 * Returns a {@link String} representing the given <tt>bytes</tt>, with a textual representation
	 * depending if the given amount can be represented as KB, MB, GB or TB, limiting the number
	 * of decimals, if there are any
	 *
	 * @param bytes The <tt>bytes</tt> to be represented
	 * @param numDecimals The maximum number of decimals to be shown after the comma
	 * @return The <tt>String</tt> that represents the given bytes
	 * @throws IllegalArgumentException Thrown if <tt>bytes</tt> or <tt>numDecimals</tt> are negative
	 */
	public static String byteSizeString(long bytes, int numDecimals) {
		if(numDecimals < 0)
			throw new IllegalArgumentException("Given number of decimals can't be less than zero");

		String byteSizeString = byteSizeString(bytes);
		String decimalSharps = "";
		for(int n = 0; n < numDecimals; n++)
			decimalSharps += "#";
		DecimalFormat decimalFormat = new DecimalFormat("#." + decimalSharps);
		decimalFormat.setRoundingMode(RoundingMode.CEILING);

		int unitPos = byteSizeString.lastIndexOf(' ');
		String stringValue = byteSizeString.substring(0, unitPos);
		stringValue = stringValue.replace(',', '.');
		float floatValue = Float.parseFloat(stringValue);
		byteSizeString = decimalFormat.format(floatValue) + byteSizeString.substring(unitPos);
		return byteSizeString;
	}


	/**
	 * Ensures that the file name given is unique in the target directory, appending
	 * (1), (2)... (n+1) to the file name in case it already exists
	 *
	 * @param fileName The string of the file name
	 * @param targetPath The path to check if there is a file with the name equals <tt>fileName</tt>
	 * @return The modified string
	 */
	public static String ensureFileNameOnPath(Path targetPath, String fileName) {
		String newName = fileName;
		if(targetPath.resolve(fileName).toFile().exists()) {
			int pos = fileName.lastIndexOf('.');
			newName = fileName.substring(0, pos) + "(1)." + fileName.substring(pos+1);
		}
		while(targetPath.resolve(newName).toFile().exists()) {
			int posL = newName.lastIndexOf('(');
			int posR = newName.lastIndexOf(')');
			int num = Integer.parseInt(newName.substring(posL + 1, posR));
			newName = newName.substring(0, posL + 1) + ++num + newName.substring(posR);
		}
		return newName;
	}


	/**
	 * This class implements <code>{@link java.io.FileFilter}</code> to
	 * accept a file with some of the given extensionsToFilter. If no extensionsToFilter are given
	 * the file is accepted. The extensionsToFilter must be given without the dot.
	 *
	 * @author Octavio Calleya
	 *
	 */
	public static class ExtensionFileFilter implements FileFilter {

		private String[] extensionsToFilter;
		private int numExtensions;

		public ExtensionFileFilter(String... extensionsToFilter) {
			this.extensionsToFilter = extensionsToFilter;
			numExtensions = extensionsToFilter.length;
		}

		public ExtensionFileFilter() {
			extensionsToFilter = new String[] {};
			numExtensions = 0;
		}

		public void addExtension(String extension) {
			boolean contains = false;
			for(String someExtension: extensionsToFilter)
				if(someExtension != null && extension.equals(someExtension))
					contains = true;
			if(!contains) {
				ensureArrayLength();
				extensionsToFilter[numExtensions++] = extension;
			}
		}

		public void removeExtension(String extension) {
			for(int i = 0; i< extensionsToFilter.length; i++)
				if(extensionsToFilter[i].equals(extension)) {
					extensionsToFilter[i] = null;
					numExtensions--;
				}
			extensionsToFilter = Arrays.copyOf(extensionsToFilter, numExtensions);
		}

		public boolean hasExtension(String extension) {
			for(String someExtension: extensionsToFilter)
				if(extension.equals(someExtension))
					return true;
			return false;
		}

		public void setExtensionsToFilter(String... extensionsToFilter) {
			if(extensionsToFilter == null)
				this.extensionsToFilter = new String[] {};
			else
				this.extensionsToFilter = extensionsToFilter;
			numExtensions = this.extensionsToFilter.length;
		}

		public String[] getExtensionsToFilter() {
			return extensionsToFilter;
		}

		private void ensureArrayLength() {
			if(numExtensions == extensionsToFilter.length)
				extensionsToFilter = Arrays.copyOf(extensionsToFilter, numExtensions == 0 ? 1 : 2 * numExtensions);

		}

		@Override
		public boolean accept(File pathname) {
			boolean res = false;
			if(!pathname.isDirectory() && !pathname.isHidden()) {
				int pos = pathname.getName().lastIndexOf('.');
				if(pos != -1) {
					String extension = pathname.getName().substring(pos + 1);
					if(numExtensions == 0)
						res = true;
					else
						res = hasExtension(extension);
				}
			}
			return res;
		}
	}
}