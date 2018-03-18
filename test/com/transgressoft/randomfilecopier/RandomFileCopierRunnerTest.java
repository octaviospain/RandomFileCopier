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

import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
public class RandomFileCopierRunnerTest {

	String tenTestFilesFolder = "test-resources/10testfiles/";

	static Path parentTestFolder;
	Path testFolder;

    RandomFileCopierRunner randomFileCopierRunner;
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();

	String DOC = "Random File Copier.\n\n" +
			"Usage:\n" +
			"  RandomFileCopier <source_directory> <target_directory> <max_files> [-v] [-s=<maxbytes>] " +
			"[-e=<extension>]...\n\n" +
			"Options:\n" +
			"  -h, --help                     Show this help text.\n" +
			"  <max_files>                    The maximum number of files.\n" +
			"  -v, --verbose                  Show some extra information of the process.\n" +
			"  -e, --extension=<extension>    A required extension of a file to be copied\n" +
			"  -s, --space=<maxbytes>         The maximum bytes to copy in the destination.\n\n";

	@BeforeAll
    public static void beforeAll() throws IOException {
        parentTestFolder =  Files.createTempDirectory("RandomFileCopierTest");
    }

	@BeforeEach
	public void setUp() throws IOException {
		System.setOut(new PrintStream(outContent));
		testFolder =  Files.createTempDirectory(parentTestFolder, "case");
	}

	@Test
	public void copyWithPositiveBytesCopiesAllTest() throws Exception {
		File[] sourceFiles = new File(tenTestFilesFolder).listFiles();
		long allFilesBytes = Stream.of(sourceFiles)
									.mapToLong(File::length)
									.sum();

		String[] args = new String[]{tenTestFilesFolder, testFolder.toString(), "0", "-s=" + allFilesBytes};
		randomFileCopierRunner.main(args);

		StringTokenizer stringTokenizer = new StringTokenizer(outContent.toString(), "\n");
		String firstLine = stringTokenizer.nextToken();
		String secondLine = stringTokenizer.nextToken();
		String thirdLine = stringTokenizer.nextToken();
		String fourthLine = stringTokenizer.nextToken();

		assertEquals("Scanning source directory...", firstLine);
		assertTrue(secondLine.matches("\\d{1,} files found"));
		assertEquals("Copying files to the destination directory...", thirdLine);
		assertTrue(fourthLine.matches("Done. \\d+ files, \\d+(.)?\\d* \\w+ copied"));
	}

	@Test
	public void copyWithNegativeBytesCopiesAllTest() throws Exception {
		String[] args = new String[]{tenTestFilesFolder, testFolder.toString(), "0", "-s=-1"};
		randomFileCopierRunner.main(args);

		StringTokenizer stringTokenizer = new StringTokenizer(outContent.toString(), "\n");
		String firstLine = stringTokenizer.nextToken();
		String secondLine = stringTokenizer.nextToken();
		String thirdLine = stringTokenizer.nextToken();
		String fourthLine = stringTokenizer.nextToken();

		assertEquals("Scanning source directory...", firstLine);
		assertTrue(secondLine.matches("\\d{1,} files found"));
		assertEquals("Copying files to the destination directory...", thirdLine);
		assertTrue(fourthLine.matches("Done. \\d+ files, \\d+(.)?\\d* \\w+ copied"));
	}

	@Test
	public void copyWithNonExistentExtensionCopiesNothingTest() throws Exception {
		String[] args = new String[]{tenTestFilesFolder, testFolder.toString(), "0", "-e=pdf"};
		randomFileCopierRunner.main(args);

		StringTokenizer stringTokenizer = new StringTokenizer(outContent.toString(), "\n");
		String firstLine = stringTokenizer.nextToken();
		String secondLine = stringTokenizer.nextToken();

		assertEquals("Scanning source directory...", firstLine);
		assertEquals("No files found with the given constraints", secondLine);
	}

	@Test
	public void sourceAndTargetAreTheSameTest() throws Exception {
		String[] args = new String[]{tenTestFilesFolder, tenTestFilesFolder, "0"};
		randomFileCopierRunner.main(args);

		String expectedMessage = "ERROR: Source and target directory are the same\n\n" + DOC;
		assertEquals(expectedMessage, outContent.toString());
	}

	@Test
	public void sourceNotDirectoryTest() throws Exception {
		String[] args = new String[]{tenTestFilesFolder + "texttestfile1.txt", testFolder.toString(), "0"};
		randomFileCopierRunner.main(args);

		String expectedMessage = "ERROR: Source path is not a directory\n\n" + DOC;
		assertEquals(expectedMessage, outContent.toString());
	}

	@Test
	public void sourceNonExistentTest() throws Exception {
        testFolder = Paths.get("test-resources", "nonexistentfolder/");
		String[] args = new String[]{testFolder.toString(), testFolder.toString(), "0"};
		randomFileCopierRunner.main(args);

		String expectedMessage = "ERROR: Source path doesn't exist\n\n" + DOC;
		assertEquals(expectedMessage, outContent.toString());
	}

	@Test
	public void targetNotDirectoryTest() throws Exception {
		String[] args = new String[]{tenTestFilesFolder, tenTestFilesFolder + "texttestfile1.txt", "0"};
		randomFileCopierRunner.main(args);

		String expectedMessage = "ERROR: Target path is not a directory\n\n" + DOC;
		assertEquals(expectedMessage, outContent.toString());
	}

	@Test
	public void targetNonExistentShouldCreateItTest() throws Exception {
	    testFolder = Paths.get("test-resources", "nonexistentfolder/");
		File nonExistentTarget = new File(testFolder.toString());
		String[] args = new String[]{tenTestFilesFolder, testFolder.toString(), "0"};
		randomFileCopierRunner.main(args);

		StringTokenizer stringTokenizer = new StringTokenizer(outContent.toString(), "\n");
		String firstLine = stringTokenizer.nextToken();
		String secondLine = stringTokenizer.nextToken();
		String thirdLine = stringTokenizer.nextToken();
		String fourthLine = stringTokenizer.nextToken();

		assertEquals("Scanning source directory...", firstLine);
		assertTrue(secondLine.matches("\\d{1,} files found"));
		assertEquals("Copying files to the destination directory...", thirdLine);
		assertTrue(fourthLine.matches("Done. \\d+ files, \\d+(.)?\\d* \\w+ copied"));

		for(File file: nonExistentTarget.listFiles())
			assertTrue(file.delete());
		assertTrue(Files.deleteIfExists(nonExistentTarget.toPath()));
	}

	@Test
	public void maxFilesIsMaxValueTest() throws Exception {
		String[] args = new String[]{tenTestFilesFolder, testFolder.toString(), Integer.toString(Integer.MAX_VALUE)};
		randomFileCopierRunner.main(args);

		StringTokenizer stringTokenizer = new StringTokenizer(outContent.toString(), "\n");
		String firstLine = stringTokenizer.nextToken();
		String secondLine = stringTokenizer.nextToken();
		String thirdLine = stringTokenizer.nextToken();
		String fourthLine = stringTokenizer.nextToken();

		assertEquals("Scanning source directory...", firstLine);
		assertTrue(secondLine.matches("\\d{1,} files found"));
		assertEquals("Copying files to the destination directory...", thirdLine);
		assertTrue(fourthLine.matches("Done. \\d+ files, \\d+(.)?\\d* \\w+ copied"));
	}

	@Test
	public void maxFilesInvalidTest() throws Exception {
		String[] args = new String[]{tenTestFilesFolder, testFolder.toString(), "a"};
		randomFileCopierRunner.main(args);

		String expectedMessage = "ERROR: MaxFiles must be between 0 and " + Integer.MAX_VALUE + " inclusively\n\n" + DOC;
		assertEquals(expectedMessage, outContent.toString());
	}
}