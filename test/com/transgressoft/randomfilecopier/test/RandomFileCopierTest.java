package com.transgressoft.randomfilecopier.test;

import com.transgressoft.randomfilecopier.*;
import org.junit.*;
import org.junit.rules.*;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Octavio Calleya
 */
public class RandomFileCopierTest {

	String tenTestFilesFolder = "test-resources/10testfiles/";
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

	File sourceFolderFile;
	File[] sourceFiles;
	File[] targetFiles;
	RandomFileCopier randomFileCopier;

	@Test
	public void copyAllFiles() throws Exception {
		sourceFolderFile = new File(tenTestFilesFolder);
		sourceFiles = sourceFolderFile.listFiles();
		randomFileCopier = new RandomFileCopier(tenTestFilesFolder, testFolder.getRoot().getAbsolutePath(), 0);

		assertEquals(0, randomFileCopier.getFilterExtensions().length);
		assertNotEquals(0, randomFileCopier.getMaxBytesToCopy());

		randomFileCopier.randomCopy();

		targetFiles = testFolder.getRoot().listFiles();

		assertEquals(10, targetFiles.length);
		assertTrue(areTheSameFiles(sourceFiles, targetFiles));
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