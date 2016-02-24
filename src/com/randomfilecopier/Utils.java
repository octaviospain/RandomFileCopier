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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that does some useful operations with files, directories, strings
 * or other operations utilities to be used for the application
 * 
 * @author Octavio Calleya
 *
 */
public class Utils {

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
	public static List<File> getAllFilesInFolder(File rootFolder, FileFilter filter, int maxFilesRequired) throws IllegalArgumentException {
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
	
	/**
	 * Returns a {@link String} representing the given <tt>bytes</tt>, with a textual representation
	 * depending if the given amount can be represented as KB, MB, GB or TB
	 * 
	 * @param bytes The <tt>bytes</tt> to be represented
	 * @return The <tt>String</tt> that represents the given bytes
	 * @throws IllegalArgumentException Thrown if <tt>bytes</tt> is negative
	 */
	public static String byteSizeString(long bytes) throws IllegalArgumentException {
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
			binRemainder = (short) (bytesAmount%1024);
			decRemainder += (((float) binRemainder)/1024);
		}
		String remainderStr = ("" + decRemainder).substring(2);
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
	public static String byteSizeString(long bytes, int numDecimals) throws IllegalArgumentException {
		if(numDecimals < 0)
			throw new IllegalArgumentException("Given number of decimals can't be less than zero");
		String byteSizeString = byteSizeString(bytes);
		int pos = byteSizeString.lastIndexOf(",");
		int unitPos = byteSizeString.lastIndexOf(" ");
		if(pos != -1 && numDecimals > 0) {
			String abs = byteSizeString.substring(0, pos+1);
			int rem = Integer.valueOf(byteSizeString.substring(pos+1, unitPos));
			short numDigits = (short) (""+rem).length();
			int remainderIndex = numDecimals < numDigits ? numDecimals : numDigits;
			int magnitudeEsc = (int)Math.pow(10, (numDigits-remainderIndex));
			int boundedRemainder = rem/magnitudeEsc; 
			int remainderRem = rem%magnitudeEsc;
			if(boundedRemainder != 0) {
				int a = 5*magnitudeEsc/10;
				if(remainderRem > a)
					boundedRemainder ++;
				byteSizeString = abs + (boundedRemainder%10 == 0 ? boundedRemainder/10 : boundedRemainder) + byteSizeString.substring(unitPos);
			}
		}
		return byteSizeString;
	}
	
	/**
	 * This class implements <code>{@link java.io.FileFilter}</code> to
	 * accept a file with some of the given extensions. If no extensions are given
	 * the file is accepted. The extensions must be given without the dot.
	 * 
	 * @author Octavio Calleya
	 *
	 */
	public static class ExtensionFileFilter implements FileFilter {
		
		private String[] extensions;
		private int numExtensions;
		
		public ExtensionFileFilter(String... extensions) {
			this.extensions = extensions;
			numExtensions = extensions.length;
		}
		
		public ExtensionFileFilter() {
			extensions = new String[] {};
			numExtensions = 0;
		}
		
		public void addExtension(String ext) {
			boolean contains = false;
			for(String e: extensions)
				if(e != null && ext.equals(e))
					contains = true;
			if(!contains) {
				ensureArrayLength();
				extensions[numExtensions++] = ext;
			}
		}
		
		public void removeExtension(String ext) {
			for(int i=0; i<extensions.length; i++)
				if(extensions[i].equals(ext)) {
					extensions[i] = null;
					numExtensions--;
				}
			extensions = Arrays.copyOf(extensions, numExtensions);
		}
		
		public boolean hasExtension(String ext) {
			for(String e: extensions)
				if(ext.equals(e))
					return true;
			return false;
		}
		
		public void setExtensions(String... extensions) {
			if(extensions == null)
				this.extensions = new String[] {};
			else
				this.extensions = extensions;
			numExtensions = this.extensions.length;
		}
		
		public String[] getExtensions() {
			return extensions;
		}
		
		private void ensureArrayLength() {
			if(numExtensions == extensions.length)
				extensions = Arrays.copyOf(extensions, numExtensions == 0 ? 1 : 2*numExtensions);
			
		}

		@Override
		public boolean accept(File pathname) {
			boolean res = false;
			if(!pathname.isDirectory() && !pathname.isHidden()) {
				int pos = pathname.getName().lastIndexOf(".");
				if(pos != -1) {
					String extension = pathname.getName().substring(pos+1);
					if(numExtensions == 0)
						res = true;
					else
						for(String requiredExtension: extensions)
							if(extension.equals(requiredExtension))
								res = true;
				}
			}
			return res;
		}		
	}
}