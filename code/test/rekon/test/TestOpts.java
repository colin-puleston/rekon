/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Colin Puleston
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package rekon.test;

import java.io.*;
import java.util.*;

class TestOpts {

	static private final File GENERAL_TESTS_BASE_DIR = new File("test/ontologies/");
	static private final String GENERAL_TESTS_DEFAULT_LEAF_DIR = "test";

	private boolean runningGeneralTests = false;

	File getGeneralTestsDir() {

		return new File(GENERAL_TESTS_BASE_DIR, getGeneralTestsLeafDir());
	}

	String getGeneralTestsLeafDir() {

		return GENERAL_TESTS_DEFAULT_LEAF_DIR;
	}

	void setRunningGeneralTests() {

		runningGeneralTests = true;
	}

	boolean runningGeneralTests() {

		return runningGeneralTests;
	}

	Iterator<String> parseArg(String arg, int minLen, int maxLen) {

		String[] vals = arg.split(":");

		if (vals.length < minLen || vals.length > maxLen) {

			throw new RuntimeException("ERROR: Bad run-opts string!");
		}

		return Arrays.asList(vals).iterator();
	}

	Integer parseInt(String value) {

		try {

			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {

			throw new RuntimeException("ERROR: Bad integer!");
		}
	}
}
