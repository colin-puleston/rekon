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

public class Run extends Tester {

	static private class RunInvoker extends TestInvoker<RunOpts> {

		RunInvoker(TestConfig config) {

			super(config);
		}

		RunOpts createCustomOpts(String customConfig) {

			return new RunOpts(customConfig);
		}

		void run(RunOpts customOpts, ReasonerOpt reasonerOpt, File ontologyFile) {

			new Run(customOpts, reasonerOpt, ontologyFile);
		}
	}

	static public void main(String[] args) {

		new RunInvoker(new TestConfig(args, true));
	}

	private OWLReasoner reasoner;

	private class RunTimer {

		private long start = System.currentTimeMillis();
		private Long timeInSecs = null;

		void stop() {

			timeInSecs = (System.currentTimeMillis() - start) / 1000;
		}

		void showTime(String title) {

			if (timeInSecs == null) {

				stop();
			}

			showOptionalInfo(title, timeInSecs);
		}

		void showInfo(String info) {

			showOptionalInfo(info);
		}
	}

	private abstract class Tester {

		private List<OpTester> opTesters = new ArrayList<OpTester>();

		abstract class OpTester {

			private String op;

			private int count = 0;
			private RunTimer timer = new RunTimer();

			OpTester(String op) {

				this.op = op;

				opTesters.add(this);

				for (OWLOntology o : manager.allOntologies) {

					testOp(this, o);
				}

				timer.stop();
			}

			abstract Set<OWLClass> doOp(OWLClassExpression c);

			void printCount() {

				showGeneralInfo("  " + op, count);
			}

			void printTime() {

				timer.showTime("  " + op);
			}

			void test(OWLClassExpression c) {

				count += doOp(c).size();
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

			RunTimer timer = new RunTimer();

			showGeneralInfo("");
			showGeneralInfo("TESTING-" + opGroup + "...");

			new EquivsTester();
			new AllSubsTester();
			new DirectSubsTester();
			new AllSupersTester();
			new DirectSupersTester();

			timer.stop();

			showGeneralInfo("");
			showGeneralInfo("COUNTS:");

			for (OpTester opTester : opTesters) {

				opTester.printCount();
			}

			timer.showInfo("");
			timer.showInfo("TIME:");

			for (OpTester opTester : opTesters) {

				opTester.printTime();
			}

			timer.showInfo("");
			timer.showTime("TOTAL-TIME");
		}

		abstract void testOp(OpTester opTester, OWLOntology o);
	}

	private class ClassOpTester extends Tester {

		ClassOpTester() {

			run("CLASS");
		}

		void testOp(OpTester opTester, OWLOntology o) {

			for (OWLClass c : o.getClassesInSignature()) {

				opTester.test(c);
			}
		}
	}

	private class QueryOpTester extends Tester {

		private int maxQueries;

		QueryOpTester(int maxQueries) {

			this.maxQueries = maxQueries;

			run("QUERY");
		}

		void testOp(OpTester opTester, OWLOntology o) {

			AutoQueryGenerator queries = new AutoQueryGenerator(manager, maxQueries);

			while (true) {

				OWLClassExpression next = queries.next();

				if (next == null) {

					break;
				}

				opTester.test(next);
			}
		}
	}

	private Run(RunOpts runOpts, ReasonerOpt reasonerOpt, File ontologyFile) {

		super(ontologyFile, runOpts.runningGeneralTests());

		reasoner = startRunReasoner(reasonerOpt);

		for (int i = 0 ; i < runOpts.classRuns ; i++) {

			new ClassOpTester();
		}

		for (int i = 0 ; i < runOpts.queryRuns ; i++) {

			new QueryOpTester(runOpts.maxQueries);
		}
	}

	private OWLReasoner startRunReasoner(ReasonerOpt opt) {

		RunTimer timer = new RunTimer();
		OWLReasoner r = startReasoner(opt);

		timer.showInfo("");
		timer.showTime("PRECOMPUTE-TIME");

		return r;
	}
}
