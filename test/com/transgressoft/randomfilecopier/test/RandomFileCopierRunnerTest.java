package com.transgressoft.randomfilecopier.test;

import com.transgressoft.randomfilecopier.*;
import org.junit.*;
import org.junit.rules.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * @author Octavio Calleya
 */
public class RandomFileCopierRunnerTest {

	String tenTestFilesFolder = "test-resources/10testfiles/";
	String testFolderPath;

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	RandomFileCopierRunner randomFileCopierRunner;
	ByteArrayOutputStream outContent = new ByteArrayOutputStream();

	String DOC = "Random File Copier.\n\n" +
			"Usage:\n" +
			"  RandomFileCopier <source_directory> <target_directory> <max_files> [-v] [-s=<maxbytes>] " +
			"[-e=<extension>]...\n\n" +
			"Options:\n" +
			"  -h, --help  Show this help text.\n" +
			"  <max_files>  The maximum number of files.\n" +
			"  -v, --verbose  Show some extra information of the process.\n" +
			"  -e, --extension=<extension> Extension required to the file.\n" +
			"  -s, --space=<maxbytes> Max bytes to copy in the destination.\n\n";

	@Before
	public void setUp() {
		System.setOut(new PrintStream(outContent));
		testFolderPath = testFolder.getRoot().getAbsolutePath();
	}

	@After
	public void tearDown() {
		System.setOut(null);
		testFolder.delete();
	}

	@Test
	public void copyWithPositiveBytesCopiesAllTest() throws Exception {
		File[] sourceFiles = new File(tenTestFilesFolder).listFiles();
		long allFilesBytes = Stream.of(sourceFiles)
									.mapToLong(File::length)
									.sum();

		String[] args = new String[]{tenTestFilesFolder, testFolderPath, "0", "-s=" + allFilesBytes};
		randomFileCopierRunner.main(args);

		StringTokenizer stringTokenizer = new StringTokenizer(outContent.toString(), "\n");
		String firstLine = stringTokenizer.nextToken();
		String secondLine = stringTokenizer.nextToken();
		String thirdLine = stringTokenizer.nextToken();
		String fourthLine = stringTokenizer.nextToken();

		assertEquals("Scanning source directory...", firstLine);
		assertTrue(secondLine.matches("\\d{1,} files found"));
		assertEquals("Copying files to the destination directory...", thirdLine);
		assertTrue(fourthLine.matches("Done. \\d+(.)?\\d* \\w+ copied"));
	}

	@Test
	public void copyWithNegativeBytesCopiesAllTest() throws Exception {
		String[] args = new String[]{tenTestFilesFolder, testFolderPath, "0", "-s=-1"};
		randomFileCopierRunner.main(args);

		StringTokenizer stringTokenizer = new StringTokenizer(outContent.toString(), "\n");
		String firstLine = stringTokenizer.nextToken();
		String secondLine = stringTokenizer.nextToken();
		String thirdLine = stringTokenizer.nextToken();
		String fourthLine = stringTokenizer.nextToken();

		assertEquals("Scanning source directory...", firstLine);
		assertTrue(secondLine.matches("\\d{1,} files found"));
		assertEquals("Copying files to the destination directory...", thirdLine);
		assertTrue(fourthLine.matches("Done. \\d+(.)?\\d* \\w+ copied"));
	}

	@Test
	public void copyWithNonExistentExtensionCopiesNothingTest() throws Exception {
		String[] args = new String[]{tenTestFilesFolder, testFolderPath, "0", "-e=pdf"};
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
		String[] args = new String[]{tenTestFilesFolder + "texttestfile1.txt", testFolderPath, "0"};
		randomFileCopierRunner.main(args);

		String expectedMessage = "ERROR: Source path is not a directory\n\n" + DOC;
		assertEquals(expectedMessage, outContent.toString());
	}

	@Test
	public void sourceNonExistentTest() throws Exception {
		testFolderPath = "./nonexistentfolder/";
		String[] args = new String[]{testFolderPath, testFolderPath, "0"};
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
		testFolderPath = "./nonexistentfolder/";
		File nonExistentTarget = new File(testFolderPath);
		String[] args = new String[]{tenTestFilesFolder, testFolderPath, "0"};
		randomFileCopierRunner.main(args);

		StringTokenizer stringTokenizer = new StringTokenizer(outContent.toString(), "\n");
		String firstLine = stringTokenizer.nextToken();
		String secondLine = stringTokenizer.nextToken();
		String thirdLine = stringTokenizer.nextToken();
		String fourthLine = stringTokenizer.nextToken();

		assertEquals("Scanning source directory...", firstLine);
		assertTrue(secondLine.matches("\\d{1,} files found"));
		assertEquals("Copying files to the destination directory...", thirdLine);
		assertTrue(fourthLine.matches("Done. \\d+(.)?\\d* \\w+ copied"));

		for(File file: nonExistentTarget.listFiles())
			assertTrue(file.delete());
		assertTrue(Files.deleteIfExists(nonExistentTarget.toPath()));
	}

	@Test
	public void maxFilesIsMaxValueTest() throws Exception {
		String[] args = new String[]{tenTestFilesFolder, testFolderPath, Integer.toString(Integer.MAX_VALUE)};
		randomFileCopierRunner.main(args);

		StringTokenizer stringTokenizer = new StringTokenizer(outContent.toString(), "\n");
		String firstLine = stringTokenizer.nextToken();
		String secondLine = stringTokenizer.nextToken();
		String thirdLine = stringTokenizer.nextToken();
		String fourthLine = stringTokenizer.nextToken();

		assertEquals("Scanning source directory...", firstLine);
		assertTrue(secondLine.matches("\\d{1,} files found"));
		assertEquals("Copying files to the destination directory...", thirdLine);
		assertTrue(fourthLine.matches("Done. \\d+(.)?\\d* \\w+ copied"));
	}

	@Test
	public void maxFilesInvalidTest() throws Exception {
		String[] args = new String[]{tenTestFilesFolder, testFolderPath, "a"};
		randomFileCopierRunner.main(args);

		String expectedMessage = "ERROR: MaxFiles must be between 0 and " + Integer.MAX_VALUE + " inclusively\n\n" + DOC;
		assertEquals(expectedMessage, outContent.toString());
	}
}