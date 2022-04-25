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

public class Run {

	static private class RunInvoker extends TestInvoker<RunOpts> {

		RunInvoker(String[] args) {

			super(args);
		}

		RunOpts createCustomOpts(String customConfig) {

			return new RunOpts(customConfig);
		}

		void run(RunOpts customOpts, ReasonerOpt reasonerOpt, File ontologyFile) {

			new Run(customOpts, reasonerOpt, ontologyFile);
		}
	}

	static public void main(String[] args) {

		new RunInvoker(args);
	}

	private TestManager manager;

	private OWLOntology ontology;
	private OWLDataFactory factory;
	private OWLReasoner reasoner;

	private RunOpts runOpts;

	private abstract class Tester {

		private List<OpTester> opTesters = new ArrayList<OpTester>();

		abstract class OpTester {

			private String op;

			private int count = 0;
			private long timeInSecs = 0;

			OpTester(String op) {

				this.op = op;

				opTesters.add(this);

				run();
			}

			abstract Set<OWLClass> doOp(OWLClassExpression c);

			void printCount() {

				System.out.println("  " + op + ": " + count);
			}

			void printTime() {

				System.out.println("  " + op + ": " + timeInSecs);
			}

			void test(OWLClassExpression c) {

				count += doOp(c).size();
			}

			private void run() {

				long startTime = System.currentTimeMillis();

				for (OWLOntology o : manager.manager.getOntologies()) {

					testOp(this, o);
				}

				timeInSecs = (System.currentTimeMillis() - startTime) / 1000;
			}
		}

		private class EquivsTester extends OpTester {

			EquivsTester() {

				super("EQUIVS");
			}

			Set<OWLClass> doOp(OWLClassExpression c) {

				return reasoner.getEquivalentClasses(c).getEntities();
			}
		}

		private class DirectSubsTester extends OpTester {

			DirectSubsTester() {

				super("DIRECT-SUBS");
			}

			Set<OWLClass> doOp(OWLClassExpression c) {

				return reasoner.getSubClasses(c, true).getFlattened();
			}
		}

		private class DirectSupersTester extends OpTester {

			DirectSupersTester() {

				super("DIRECT-SUPERS");
			}

			Set<OWLClass> doOp(OWLClassExpression c) {

				return reasoner.getSuperClasses(c, true).getFlattened();
			}
		}

		private class AllSubsTester extends OpTester {

			AllSubsTester() {

				super("ALL-SUBS");
			}

			Set<OWLClass> doOp(OWLClassExpression c) {

				return reasoner.getSubClasses(c, false).getFlattened();
			}
		}

		private class AllSupersTester extends OpTester {

			AllSupersTester() {

				super("ALL-SUPERS");
			}

			Set<OWLClass> doOp(OWLClassExpression c) {

				return reasoner.getSuperClasses(c, false).getFlattened();
			}
		}

		void run(String opGroup) {

			long startTime = System.currentTimeMillis();

			System.out.println("TESTING-" + opGroup + "...");

			new EquivsTester();
			new AllSubsTester();
			new DirectSubsTester();
			new AllSupersTester();
			new DirectSupersTester();

			long timeInSecs = (System.currentTimeMillis() - startTime) / 1000;

			System.out.println("");
			System.out.println("COUNTS:");

			for (OpTester opTester : opTesters) {

				opTester.printCount();
			}

			System.out.println("");
			System.out.println("TIME:");

			for (OpTester opTester : opTesters) {

				opTester.printTime();
			}

			System.out.println("");
			System.out.println("TOTAL-TIME: " + timeInSecs);
			System.out.println("\n");
		}

		abstract void testOp(OpTester opTester, OWLOntology o);
	}

	private class RetrieveTester extends Tester {

		RetrieveTester() {

			run("RETRIEVE");
		}

		void testOp(OpTester opTester, OWLOntology o) {

			for (OWLClass c : o.getClassesInSignature()) {

				opTester.test(c);
			}
		}
	}

	private class QueryTester extends Tester {

		private int maxQueries;

		QueryTester(int maxQueries) {

			this.maxQueries = maxQueries;

			run("QUERY");
		}

		void testOp(OpTester opTester, OWLOntology o) {

			QueryGenerator generator = new QueryGenerator(o, factory, maxQueries);

			while (true) {

				OWLObjectIntersectionOf next = generator.next();

				if (next == null) {

					break;
				}

				opTester.test(next);
			}
		}
	}

	private Run(RunOpts runOpts, ReasonerOpt reasonerOpt, File ontologyFile) {

		manager = new TestManager();

		ontology = manager.loadOntology(ontologyFile);
		factory = manager.getFactory();
		reasoner = manager.createReasoner(ontology, reasonerOpt);

		System.out.println("\nONTOLOGY: " + ontologyFile);
		System.out.println("REASONER: " + reasoner.getClass().getSimpleName());
		System.out.println("");

		precomputeInferences();

		for (int i = 0 ; i < runOpts.retrieveRuns ; i++) {

			new RetrieveTester();
		}

		for (int i = 0 ; i < runOpts.queryRuns ; i++) {

			new QueryTester(runOpts.maxQueries);
		}
	}

	private void precomputeInferences() {

		long startTime = System.currentTimeMillis();

		reasoner.precomputeInferences();

		long timeInSecs = (System.currentTimeMillis() - startTime) / 1000;

		System.out.println("PRECOMPUTE-TIME: " + timeInSecs);
		System.out.println("");
	}
}
