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

import org.docopt.*;

import java.io.*;
import java.util.*;

/**
 * Runner of the {@link RandomFileCopier} application from command line
 * using the docopt library.
 *
 * @author Octavio Calleya
 * @version 0.2.6
 *
 * <a href="https://github.com/docopt/docopt.java">docopt java</a>
 */
public class RandomFileCopierRunner {

	/**************************** Command line usage ***************************/

	private static final String DOC = "Random File Copier.\n\n" +
			"Usage:\n" +
			"  RandomFileCopier <source_directory> <target_directory> <max_files> [-v] [-s=<maxbytes>] " +
			"[-e=<extension>]...\n\n" +
			"Options:\n" +
			"  -h, --help                     Show this help text.\n" +
			"  <max_files>                    The maximum number of files.\n" +
			"  -v, --verbose                  Show some extra information of the process.\n" +
			"  -e, --extension=<extension>    A required extension of a file to be copied\n" +
			"  -s, --space=<maxbytes>         The maximum bytes to copy in the destination.\n";

	private static File sourceFile;
	private static File targetFile;
	private static String sourceString;
	private static String targetString;
	private static String[] extensionsCmd;
	private static int maxFilesCmd;
	private static boolean verboseCmd;
	private static long maxBytesCmd;

	public static void main(String[] args) throws IOException {
		parseArguments(args);

		if (validArguments()) {
			RandomFileCopier copier = new RandomFileCopier(sourceFile.toPath(), targetFile.toPath(), maxFilesCmd);
			copier.setVerbose(verboseCmd);
			copier.setFilterExtensions(extensionsCmd);
			if (maxBytesCmd > 0)
				copier.setMaxBytesToCopy(maxBytesCmd);
			copier.randomCopy();
		}
	}

	@SuppressWarnings ("unchecked")
	private static void parseArguments(String... args) {
		Map<String, Object> opts = new Docopt(DOC).withVersion("Random File Copier 0.2.3").parse(args);
		sourceString = (String) opts.get("<source_directory>");
		targetString = (String) opts.get("<target_directory>");
		verboseCmd = (Boolean) opts.get("--verbose");

		List<String> extensionsList = (List<String>) opts.get("--extension");
		extensionsCmd = Arrays.stream(extensionsList.toArray())
							  .map(s -> ((String) s).substring(1))
							  .toArray(String[]::new);

		String maxBytesString = (String) opts.get("--space");
		maxBytesCmd = 0;
		if (maxBytesString != null)
			maxBytesCmd = Long.valueOf(maxBytesString.substring(1));

		String maxFilesString = (String) opts.get("<max_files>");
		try {
			maxFilesCmd = Integer.parseInt(maxFilesString);
		}
		catch (NumberFormatException exception) {
			maxFilesCmd = - 1;
		}
	}

	private static boolean validArguments() {
		boolean result = isValidSource();
		if (result) {
			result = isValidTarget();
			if (result)
				result = isValidMaxFilesString();
		}

		if (result) {
			if (sourceFile.equals(targetFile)) {
				printUsage("Source and target directory are the same");
				result &= false;
			}
			else
				result &= true;
		}

		return result;
	}

	private static boolean isValidSource() {
		boolean result = false;
		sourceFile = new File(sourceString);
		if (! sourceFile.exists())
			printUsage("Source path doesn't exist");
		else if (! sourceFile.isDirectory())
			printUsage("Source path is not a directory");
		else
			result = true;
		return result;
	}

	private static boolean isValidTarget() {
		boolean result = false;
		targetFile = new File(targetString);
		if (! targetFile.exists())
			targetFile.mkdir();
		if (targetFile.exists() && ! targetFile.isDirectory())
			printUsage("Target path is not a directory");
		else
			result = true;
		return result;
	}

	private static boolean isValidMaxFilesString() {
		boolean res = false;
		if (maxFilesCmd >= 0 && maxFilesCmd <= Integer.MAX_VALUE)
			res = true;
		else
			printUsage("MaxFiles must be between 0 and " + Integer.MAX_VALUE + " inclusively");
		return res;
	}

	private static void printUsage(String detail) {
		System.out.println("ERROR: " + detail + "\n\n" + DOC);
	}
}
