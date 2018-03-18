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

import junitx.framework.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
public class RandomFileCopierTest {


	Path tenTestFilesFolder = Paths.get("test-resources", "10testfiles");
	Path testFolderPath;

	File[] sourceFiles = tenTestFilesFolder.toFile().listFiles();
	File[] destinationFiles;
	RandomFileCopier randomFileCopier;

	@BeforeEach
	public void setUp() throws IOException {
        testFolderPath = Files.createTempDirectory(getClass().getName());
	}

	@AfterEach
	public void tearDown() {
        testFolderPath.toFile().delete();
	}

	@Test
	public void setMaxBytesAsMinorFileSizeShouldCopyMinorFile() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		File minorFile = Stream.of(sourceFiles)
							   .min((file1, file2) -> Long.valueOf(file1.length()).compareTo(file2.length())).get();
		long minorFileBytes = minorFile.length();

		randomFileCopier.setMaxBytesToCopy(minorFileBytes);
		randomFileCopier.randomCopy();

		destinationFiles = testFolderPath.toFile().listFiles();
		assertEquals(1, destinationFiles.length);
		Stream.of(destinationFiles).forEach(f -> FileAssert.assertBinaryEquals(minorFile, f));
	}

	@Test
	public void setMaxBytesToCopySmallerThanDestinationSpace() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		long maxBytesInDestination = testFolderPath.toFile().getUsableSpace() - 1;

		randomFileCopier.setMaxBytesToCopy(maxBytesInDestination);

		assertEquals(maxBytesInDestination, randomFileCopier.getMaxBytesToCopy());
	}

	@Test
	public void setMaxBytesToCopyGreaterThanDestinationSpace() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		long maxBytesInDestination = testFolderPath.toFile().getUsableSpace();

		randomFileCopier.setMaxBytesToCopy(maxBytesInDestination + 1);

		assertEquals(maxBytesInDestination, randomFileCopier.getMaxBytesToCopy());
	}

	@Test
	public void filterExtensionsAreTheSame() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		String[] extensions = new String[]{"mp3", "m4a", "txt", "java"};

		randomFileCopier.setFilterExtensions(extensions);

		assertTrue(extensions.equals(randomFileCopier.getFilterExtensions()));
	}

	@Test
	public void copyWithGivenPrintStreamVerboseIsCorrect() throws Exception {
		File logFile = Files.createTempFile(testFolderPath, "log", "txt").toFile();
		PrintStream printStream = new PrintStream(logFile);
		sourceFiles = tenTestFilesFolder.toFile().listFiles();
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0, printStream);
		randomFileCopier.setVerbose(true);
		randomFileCopier.randomCopy();

		Scanner logScanner = new Scanner(logFile);
		String scanningSourceLine = logScanner.nextLine();
		String filesFoundLine = logScanner.nextLine();
		String copyingFilesLine = logScanner.nextLine();

		int numCopiedFilesPos = filesFoundLine.indexOf(' ');
		int numCopiedFiles = Integer.parseInt(filesFoundLine.substring(0, numCopiedFilesPos));
		int numCopiedFilesPrint;
		for(numCopiedFilesPrint = 0; numCopiedFilesPrint < numCopiedFiles; numCopiedFilesPrint++) {
			logScanner.nextLine();
		}

		String doneLine = logScanner.nextLine();
		logScanner.close();
		assertTrue(logFile.delete());
		destinationFiles = testFolderPath.toFile().listFiles();

		assertEquals(sourceFiles.length, destinationFiles.length);
		assertEquals(sourceFiles.length, numCopiedFiles);
		assertEquals(sourceFiles.length, numCopiedFilesPrint);
		assertEquals("Scanning source directory...", scanningSourceLine);
		assertTrue(filesFoundLine.matches("\\d{1,} files found"));
		assertEquals("Copying files to the destination directory...", copyingFilesLine);
		assertTrue(doneLine.matches("Done. \\d+ files, \\d+(.)?\\d* \\w+ copied"));
	}

	@Test
	public void copyFromEmptyFolder() throws Exception {
        File logFile = Files.createTempFile("log", "txt").toFile();
		PrintStream printStream = new PrintStream(logFile);
		Path emptyFolder = Files.createTempDirectory(getClass().getName());
		randomFileCopier = new RandomFileCopier(emptyFolder, testFolderPath, 0, printStream);
		randomFileCopier.setVerbose(true);
		randomFileCopier.randomCopy();

		Scanner logScanner = new Scanner(logFile);
		String scanningSourceLine = logScanner.nextLine();
		String noFilesFoundLine = logScanner.nextLine();
		logScanner.close();
		assertTrue(logFile.delete());
        destinationFiles = testFolderPath.toFile().listFiles();

		assertEquals(0, destinationFiles.length);
		assertEquals("Scanning source directory...", scanningSourceLine);
		assertEquals("No files found with the given constraints", noFilesFoundLine);
	}

	@Test
	public void copyAllFilesLimitingTheNumber() throws Exception {
		int maxFilesToCopy = 5;
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 5);
		randomFileCopier.randomCopy();

		destinationFiles = testFolderPath.toFile().listFiles();
		assertTrue(maxFilesToCopy >= destinationFiles.length);
	}

	@Test
	public void copyAllFilesRegardingHalfBytesOfSources() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		long totalBytesInSource = Stream.of(sourceFiles).mapToLong(File::length).sum();

		randomFileCopier.setMaxBytesToCopy(totalBytesInSource / 2);
		randomFileCopier.randomCopy();

		destinationFiles = testFolderPath.toFile().listFiles();
		long totalBytesCopiedInDestination = Stream.of(destinationFiles).mapToLong(File::length).sum();
		assertTrue(totalBytesCopiedInDestination <= totalBytesInSource / 2);
	}

	@Test
	public void copyAllFilesRegardingMoreBytesThanAvailable() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		long maxBytesInDestination = testFolderPath.toFile().getUsableSpace() + 1;

		randomFileCopier.setMaxBytesToCopy(maxBytesInDestination);
		randomFileCopier.randomCopy();

        destinationFiles = testFolderPath.toFile().listFiles();

        assertEquals(sourceFiles.length, destinationFiles.length);
        for (int f = 0; f < sourceFiles.length; f++)
            FileAssert.assertBinaryEquals(sourceFiles[f], destinationFiles[f]);
	}

	@Test
	public void copyMoreFilesThanAvailableShouldCopyAll() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 11);
		randomFileCopier.randomCopy();

        destinationFiles = testFolderPath.toFile().listFiles();

		assertEquals(sourceFiles.length, destinationFiles.length);
		for (int f = 0; f < sourceFiles.length; f++)
		    FileAssert.assertBinaryEquals(sourceFiles[f], destinationFiles[f]);
	}

	@Test
	public void copyAllFiles() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		randomFileCopier.randomCopy();

		destinationFiles = testFolderPath.toFile().listFiles();

        assertEquals(sourceFiles.length, destinationFiles.length);
        for (int f = 0; f < sourceFiles.length; f++)
            FileAssert.assertBinaryEquals(sourceFiles[f], destinationFiles[f]);
	}
}