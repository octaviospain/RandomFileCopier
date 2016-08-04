package com.transgressoft.randomfilecopier.test;

import com.transgressoft.randomfilecopier.*;
import org.junit.*;
import org.junit.rules.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import static org.junit.Assert.*;

/**
 * @author Octavio Calleya
 * @version 0.2.3
 */
public class RandomFileCopierTest {

	String tenTestFilesFolder = "test-resources/10testfiles/";
	String testFolderPath;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

	File sourceFolderFile = new File(tenTestFilesFolder);
	File[] sourceFiles = sourceFolderFile.listFiles();
	File[] destinationFiles;
	RandomFileCopier randomFileCopier;

	@Before
	public void setUp() {
		testFolderPath = testFolder.getRoot().getAbsolutePath();
	}

	@After
	public void tearDown() {
		testFolder.delete();
	}

	@Test
	public void setMaxBytesToCopySmallerThanDestinationSpace() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		long maxBytesInDestination = testFolder.getRoot().getUsableSpace() - 1;

		randomFileCopier.setMaxBytesToCopy(maxBytesInDestination);

		assertEquals(maxBytesInDestination, randomFileCopier.getMaxBytesToCopy());
	}

	@Test
	public void setMaxBytesToCopyGreaterThanDestinationSpace() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		long maxBytesInDestination = testFolder.getRoot().getUsableSpace();

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
		File logFile = testFolder.newFile("log.txt");
		PrintStream printStream = new PrintStream(logFile);
		sourceFiles = sourceFolderFile.listFiles();
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
		destinationFiles = testFolder.getRoot().listFiles();

		assertEquals(10, destinationFiles.length);
		assertEquals(10, numCopiedFiles);
		assertEquals(10, numCopiedFilesPrint);
		assertEquals("Scanning source directory...", scanningSourceLine);
		assertTrue(filesFoundLine.matches("\\d{1,} files found"));
		assertEquals("Copying files to the destination directory...", copyingFilesLine);
		assertTrue(doneLine.matches("Done. \\d+(.)?\\d* \\w+ copied"));
	}

	@Test
	public void copyAllFilesLimitingTheNumber() throws Exception {
		int maxFilesToCopy = 5;
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 5);
		randomFileCopier.randomCopy();

		destinationFiles = testFolder.getRoot().listFiles();
		assertTrue(maxFilesToCopy >= destinationFiles.length);
	}

	@Test
	public void copyAllFilesRegardingHalfBytesOfSources() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		long totalBytesInSource = Stream.of(sourceFiles).mapToLong(File::length).sum();

		randomFileCopier.setMaxBytesToCopy(totalBytesInSource / 2);
		randomFileCopier.randomCopy();

		destinationFiles = testFolder.getRoot().listFiles();
		long totalBytesCopiedInDestination = Stream.of(destinationFiles).mapToLong(File::length).sum();
		assertTrue(totalBytesCopiedInDestination <= totalBytesInSource / 2);
	}

	@Test
	public void copyAllFilesRegardingMoreBytesThanAvailable() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		long maxBytesInDestination = testFolder.getRoot().getUsableSpace() + 1;

		randomFileCopier.setMaxBytesToCopy(maxBytesInDestination);
		randomFileCopier.randomCopy();

		destinationFiles = testFolder.getRoot().listFiles();

		assertEquals(10, destinationFiles.length);
		assertTrue(areTheSameFiles(sourceFiles, destinationFiles));
	}
	
	@Test
	public void copyAllFiles() throws Exception {
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolderPath, 0);
		randomFileCopier.randomCopy();

		destinationFiles = testFolder.getRoot().listFiles();

		assertEquals(10, destinationFiles.length);
		assertTrue(areTheSameFiles(sourceFiles, destinationFiles));
	}

	private boolean areTheSameFiles(File[] sourceFiles, File[] targetFiles) {
		boolean result = true;
		Map<String, Long> lengthByFileNameMap = new HashMap<>();
		for(File sourceFile: sourceFiles)
			lengthByFileNameMap.put(sourceFile.getName(), sourceFile.length());

		for(File expectedFile: targetFiles) {
			String expectedFileName = expectedFile.getName();
			long expectedFileLength = expectedFile.length();

			result &= lengthByFileNameMap.containsKey(expectedFileName);
			result &= lengthByFileNameMap.get(expectedFileName).equals(expectedFileLength);
			if(!result)
				break;
		}
		return result;
	}
}
