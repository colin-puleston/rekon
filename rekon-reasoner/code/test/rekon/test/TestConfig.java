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

abstract class TestConfig<O extends TestOpts> {

	static private final String TEST_ONTOLOGIES_DIR = "test/ontologies/";
	static private final String OWL_EXTN = ".owl";

	final ReasonerOpt reasonerOpt;
	final File ontologyFile;

	final String customArg;

	private boolean expectCustomArg;

	TestConfig(String[] args, boolean expectCustomArg) {

		this.expectCustomArg = expectCustomArg;

		ArgReader ar = new ArgReader(this, args);

		customArg = expectCustomArg ? ar.readNext() : "";
		reasonerOpt = ReasonerOpt.valueOf(ar.readNext());

		if (ar.hasNext()) {

			String testDir = ar.readNext();
			String testFile = ar.hasNext() ? ar.readNext() : getLeafDir(testDir);

			ontologyFile = new File(TEST_ONTOLOGIES_DIR + testDir, testFile + OWL_EXTN);
		}
		else {

			ontologyFile = null;
		}
	}

	void exitOnError(String erroMsg) {

		System.err.println("");
		System.err.println("ERROR: " + erroMsg);
		System.err.println("");
		System.err.println("ARGS: " + describeArgs());
		System.err.println("");

		String argOpts = describeCustomArgOptions();

		if (!argOpts.isEmpty()) {

			System.err.println(argOpts);
			System.err.println("");
		}

		System.exit(0);
	}

	abstract O createOpts();

	String describeCustomArg() {

		throw new Error("No custom arg!");
	}

	String describeCustomArgOptions() {

		return "";
	}

	private String getLeafDir(String dir) {

		return dir.substring(dir.lastIndexOf(File.separatorChar) + 1);
	}

	private String describeArgs() {

		StringBuilder args = new StringBuilder();

		if (expectCustomArg) {

			args.append(describeCustomArg() + " ");
		}

		args.append("<reasoner> [<dir> [<file>]]");

		return args.toString();
	}
}
