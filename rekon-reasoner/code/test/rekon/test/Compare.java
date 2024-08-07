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

public class Compare extends Tester {

	static private class CompareInvoker extends TestInvoker<CompareOpts> {

		CompareInvoker(TestConfig config) {

			super(config);
		}

		CompareOpts createCustomOpts(String customConfig) {

			return new CompareOpts(customConfig);
		}

		void run(CompareOpts customOpts, ReasonerOpt reasonerOpt, File ontologyFile) {

			new Compare(customOpts, reasonerOpt, ontologyFile);
		}
	}

	static public void main(String[] args) {

		new CompareInvoker(new TestConfig(args, true));
	}

	private OWLReasoner rekon;
	private OWLReasoner control;

	private CompareOpts compareOpts;

	private OntoQueries ontoQueries;
	private LinkComparator linkComparator = new LinkComparator();

	private class LinkComparator implements Comparator<OWLEntity> {

		public int compare(OWLEntity first, OWLEntity second) {

			return first.toString().compareTo(second.toString());
		}
	}

	private abstract class ScopedCompareOps {

		private CompareOpts.TestScope scope;

		ScopedCompareOps(CompareOpts.TestScope scope) {

			this.scope = scope;
		}

		abstract void performOps();

		void performOp(OWLClassExpression c) {

			Set<? extends OWLEntity> rl = getLinks(rekon, c);
			Set<? extends OWLEntity> cl = getLinks(control, c);

			if (!rl.equals(cl)) {

				Set<OWLEntity> rOnly = new HashSet<OWLEntity>(rl);
				Set<OWLEntity> cOnly = new HashSet<OWLEntity>(cl);
				Set<OWLEntity> common = new HashSet<OWLEntity>(rl);

				rOnly.removeAll(cl);
				cOnly.removeAll(rl);

				common.removeAll(rOnly);

				showGeneralInfo("");
				showGeneralInfo("MISMATCH", c);
				showGeneralInfo("Rekon + Control", orderLinks(common));
				showGeneralInfo("Rekon Only", orderLinks(rOnly));
				showGeneralInfo("Control Only", orderLinks(cOnly));
			}
		}

		private Set<? extends OWLEntity> getLinks(OWLReasoner reasoner, OWLClassExpression e) {

			switch (scope) {

				case EQUIVS:
					return reasoner.getEquivalentClasses(e).getEntities();

				case SUPS:
					return reasoner.getSuperClasses(e, true).getFlattened();

				case SUBS:
					return reasoner.getSubClasses(e, true).getFlattened();

				case SUB_INDS:
					return reasoner.getInstances(e, true).getFlattened();

				case ANCS:
					return reasoner.getSuperClasses(e, false).getFlattened();

				case DECS:
					return reasoner.getSubClasses(e, false).getFlattened();

				case DEC_INDS:
					return reasoner.getInstances(e, false).getFlattened();
			}

			throw new Error("Unrecognised compare option: " + scope);
		}

		private SortedSet<OWLEntity> orderLinks(Collection<OWLEntity> links) {

			SortedSet<OWLEntity> ordered = new TreeSet<OWLEntity>(linkComparator);

			ordered.addAll(links);

			return ordered;
		}
	}

	private class ClassCompareOps extends ScopedCompareOps {

		ClassCompareOps(CompareOpts.TestScope scope) {

			super(scope);
		}

		void performOps() {

			for (OWLOntology o : manager.allOntologies) {

				for (OWLClass c : o.getClassesInSignature()) {

					performOp(c);
				}
			}
		}
	}

	private abstract class QueryCompareOps extends ScopedCompareOps {

		QueryCompareOps(CompareOpts.TestScope scope) {

			super(scope);
		}

		void performOps() {

			QueryProvider queries = createQueryProvider();

			while (true) {

				OWLClassExpression next = queries.next();

				if (next == null) {

					break;
				}

				performOp(next);
			}
		}

		abstract QueryProvider createQueryProvider();
	}

	private class AutoQueryCompareOps extends QueryCompareOps {

		AutoQueryCompareOps(CompareOpts.TestScope scope) {

			super(scope);
		}

		QueryProvider createQueryProvider() {

			return new AutoQueryGenerator(manager, compareOpts.maxQueries);
		}
	}

	private class OntoQueryCompareOps extends QueryCompareOps {

		OntoQueryCompareOps(CompareOpts.TestScope scope) {

			super(scope);
		}

		QueryProvider createQueryProvider() {

			return ontoQueries.createQueryProvider();
		}
	}

	private Compare(CompareOpts compareOpts, ReasonerOpt reasonerOpt, File ontologyFile) {

		super(ontologyFile, false);

		this.compareOpts = compareOpts;

		ontoQueries = new OntoQueries(manager);

		rekon = startReasoner(ReasonerOpt.REKON);
		control = startReasoner(reasonerOpt);

		compareAll();
	}

	private void compareAll() {

		for (CompareOpts.TestScope scope : compareOpts.testScope.toAtoms()) {

			compareScoped(scope);
		}
	}

	private void compareScoped(CompareOpts.TestScope scope) {

		showGeneralInfo("");
		showGeneralInfo("START COMPARE " + scope + "...");

		createScopedCompareOps(scope).performOps();

		showGeneralInfo("DONE COMPARE " + scope);
	}

	private ScopedCompareOps createScopedCompareOps(CompareOpts.TestScope scope) {

		if (compareOpts.testType == CompareOpts.TestType.CLASS) {

			return new ClassCompareOps(scope);
		}

		if (compareOpts.querySource == CompareOpts.QuerySource.AUTO) {

			return new AutoQueryCompareOps(scope);
		}

		return new OntoQueryCompareOps(scope);
	}
}
