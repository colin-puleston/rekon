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

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

public class Entails extends Tester {

	static private final String ENTAILMENTS_BASE_CLASS_NAME = "_Entailment";

	static private class EntailsInvoker extends TestInvoker<EntailsOpts> {

		EntailsInvoker(TestConfig config) {

			super(config);
		}

		EntailsOpts createCustomOpts(String customConfig) {

			return new EntailsOpts();
		}

		void run(EntailsOpts customOpts, ReasonerOpt reasonerOpt, File ontologyFile) {

			new Entails(reasonerOpt, ontologyFile);
		}
	}

	static public void main(String[] args) {

		new EntailsInvoker(new TestConfig(args, false));
	}

	private OWLReasoner reasoner;

	private List<Entailment> entailments = new ArrayList<Entailment>();

	private class Entailment {

		private OWLClass cls;
		private OWLAxiom axiom;

		Entailment(OWLClass cls, OWLAxiom axiom) {

			this.cls = cls;
			this.axiom = axiom;
		}

		void test() {

			showGeneralInfo(getTitle(), reasoner.isEntailed(axiom));
		}

		private String getTitle() {

			return cls.getIRI().toURI().getFragment();
		}
	}

	private class EntailmentExtractor extends OntoSpecifiedTestExtractor {

		EntailmentExtractor() {

			super(manager);
		}

		String getTestBaseClassName() {

			return ENTAILMENTS_BASE_CLASS_NAME;
		}

		void processTestClass(TestClass testClass) {

			entailments.add(
				new Entailment(
						testClass.cls,
						testClass.extractEntailment()));
		}
	}

	private Entails(ReasonerOpt reasonerOpt, File ontologyFile) {

		super(ontologyFile, false);

		new EntailmentExtractor();

		reasoner = startReasoner(reasonerOpt);

		showGeneralInfo("");

		for (Entailment e : entailments) {

			e.test();
		}
	}
}
