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

class TestConfig {

	static private final String TEST_ONTOLOGIES_DIR = "test/ontologies/";
	static private final String OWL_EXTN = ".owl";

	static class ArgReader {

		private String[] args;
		private int current = 0;

		ArgReader(String[] args, int min) {

			this.args = args;

			if (args.length < min) {

				throw new RuntimeException("ERROR: Not enough args!");
			}
		}

		boolean more() {

			return current < args.length;
		}

		String next() {

			return args[current++];
		}
	}

	final ReasonerOpt reasonerOpt;
	final File ontologyFile;

	final String customConfig;

	TestConfig(String[] args, boolean expectCustomConfig) {

		ArgReader ar = new ArgReader(args, expectCustomConfig ? 2 : 1);

		customConfig = expectCustomConfig ? ar.next() : "";
		reasonerOpt = ReasonerOpt.valueOf(ar.next());

		if (ar.more()) {

			String testDir = ar.next();
			String testFile = ar.more() ? ar.next() : testDir;

			ontologyFile = new File(TEST_ONTOLOGIES_DIR + testDir, testFile + OWL_EXTN);
		}
		else {

			ontologyFile = null;
		}
	}
}
