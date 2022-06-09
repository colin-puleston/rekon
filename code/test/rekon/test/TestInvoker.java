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

abstract class TestInvoker<O extends TestOpts> {

	private File GENEERAL_TESTS_DIR = new File("test/ontologies/test");

	TestInvoker(String[] args) {

		TestConfig config = new TestConfig(args);
		O customOpts = createCustomOpts(config.customConfig);

		if (config.ontologyFile == null) {

			runGeneralTests(customOpts, config.reasonerOpt);
		}
		else {

			run(customOpts, config.reasonerOpt, config.ontologyFile);
		}
	}

	abstract O createCustomOpts(String customConfig);

	abstract void run(O customOpts, ReasonerOpt reasonerOpt, File ontologyFile);

	private void runGeneralTests(O customOpts, ReasonerOpt reasonerOpt) {

		customOpts.setRunningGeneralTests();

		for (File file : GENEERAL_TESTS_DIR.listFiles()) {

			if (file.getName().endsWith(".owl")) {

				System.out.println("-------------------------------");

				run(customOpts, reasonerOpt, file);
			}
		}
	}
}
