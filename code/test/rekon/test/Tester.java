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

import org.semanticweb.owlapi.reasoner.*;

abstract class Tester {

	final TestManager manager;

	private boolean terseOutput;

	Tester(File ontologyFile, boolean terseOutput) {

		this.terseOutput = terseOutput;

		manager = new TestManager(ontologyFile);

		showGeneralInfo("");
		showGeneralInfo("ONTOLOGY: " + ontologyFile);
	}

	TestManager getManager() {

		return manager;
	}

	OWLReasoner startReasoner(ReasonerOpt opt) {

		OWLReasoner reasoner = manager.createReasoner(opt);

		showGeneralInfo("REASONER", reasoner.getClass().getSimpleName());

		reasoner.precomputeInferences();

		return reasoner;
	}

	void showGeneralInfo(String title, Object info) {

		showGeneralInfo(title + ": " + info);
	}

	void showGeneralInfo(String info) {

		System.out.println(info);
	}

	void showOptionalInfo(String title, Object info) {

		showOptionalInfo(title + ": " + info);
	}

	void showOptionalInfo(String info) {

		if (!terseOutput) {

			System.out.println(info);
		}
	}
}
