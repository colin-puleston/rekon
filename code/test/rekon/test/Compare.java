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

public class Compare {

	static private class CompareInvoker extends TestInvoker<CompareOpts> {

		CompareInvoker(String[] args) {

			super(args);
		}

		CompareOpts createCustomOpts(String customConfig) {

			return new CompareOpts(customConfig);
		}

		void run(CompareOpts customOpts, ReasonerOpt reasonerOpt, File ontologyFile) {

			new Compare(customOpts, reasonerOpt, ontologyFile);
		}
	}

	static public void main(String[] args) {

		new CompareInvoker(args);
	}

	private TestManager manager;

	private OWLOntology ontology;
	private OWLDataFactory factory;

	private OWLReasoner rekon;
	private OWLReasoner control;

	private CompareOpts compareOpts;

	private LinkComparator linkComparator = new LinkComparator();

	private class LinkComparator implements Comparator<OWLClass> {

		public int compare(OWLClass first, OWLClass second) {

			return first.toString().compareTo(second.toString());
		}
	}

	private Compare(CompareOpts compareOpts, ReasonerOpt reasonerOpt, File ontologyFile) {

		this.compareOpts = compareOpts;

		manager = new TestManager();
		ontology = manager.loadOntology(ontologyFile);
		factory = manager.getFactory();

		System.out.println("ONTOLOGY: " + ontologyFile);

		rekon = startReasoner(ReasonerOpt.REKON);
		control = startReasoner(reasonerOpt);

		doCompare();
	}

	private void doCompare() {

		System.out.println("\nSTART COMPARE " + compareOpts.specific + "...");

		for (OWLOntology o : manager.manager.getOntologies()) {

			if (compareOpts.general == CompareOpts.General.RETRIEVE) {

				compareRetrieve(o);
			}
			else {

				compareQuery(o);
			}
		}

		System.out.println("\nDONE COMPARE!");
	}

	private void compareRetrieve(OWLOntology o) {

		for (OWLClass c : o.getClassesInSignature()) {

			compare(c);
		}
	}

	private void compareQuery(OWLOntology o) {

		QueryGenerator generator = new QueryGenerator(o, factory, compareOpts.maxQueries);

		while (true) {

			OWLObjectIntersectionOf next = generator.next();

			if (next == null) {

				break;
			}

			compare(next);
		}
	}

	private void compare(OWLClassExpression c) {

		Set<OWLClass> rl = getLinks(rekon, c);
		Set<OWLClass> cl = getLinks(control, c);

		if (!rl.equals(cl)) {

			Collection<OWLClass> rlo = orderLinks(rl);
			Collection<OWLClass> clo = orderLinks(cl);

			System.out.println("\nMISMATCH: " + c);
			System.out.println("Rekon: " + rlo);
			System.out.println("Control: " + clo);

			rlo.removeAll(cl);
			clo.removeAll(rl);

			System.out.println("Rekon-only: " + rlo);
			System.out.println("Control-only: " + clo);
		}
	}

	private Set<OWLClass> getLinks(OWLReasoner reasoner, OWLClassExpression e) {

		switch (compareOpts.specific) {

			case EQUIVS:
				return reasoner.getEquivalentClasses(e).getEntities();

			case SUPS:
				return reasoner.getSuperClasses(e, true).getFlattened();

			case SUBS:
				return reasoner.getSubClasses(e, true).getFlattened();

			case ANCS:
				return reasoner.getSuperClasses(e, false).getFlattened();

			case DECS:
				return reasoner.getSubClasses(e, false).getFlattened();
		}

		throw new Error("Unrecognised compare option: " + compareOpts.specific);
	}

	private Set<OWLClass> orderLinks(Collection<OWLClass> links) {

		SortedSet<OWLClass> ordered = new TreeSet<OWLClass>(linkComparator);

		ordered.addAll(links);

		return ordered;
	}

	private OWLReasoner startReasoner(ReasonerOpt opt) {

		OWLReasoner reasoner = manager.createReasoner(ontology, opt);

		System.out.println("REASONER: " + reasoner.getClass().getSimpleName());

		reasoner.precomputeInferences();

		return reasoner;
	}
}
