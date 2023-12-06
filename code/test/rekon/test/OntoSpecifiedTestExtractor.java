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

import java.util.*;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.*;
import org.semanticweb.owlapi.search.*;

abstract class OntoSpecifiedTestExtractor {

	static private final String TEST_CLASS_IRI_BASE = "urn:test#";

	private TestManager manager;

	private OWLDataFactory factory;
	private OWLClass baseCls;

	class TestClass {

		final OWLClass cls;

		private List<OWLClassExpression> equivs = new ArrayList<OWLClassExpression>();
		private List<OWLClassExpression> supers = new ArrayList<OWLClassExpression>();

		TestClass(OWLClass cls) {

			this.cls = cls;

			extractExpressions();
		}

		OWLClassExpression extractQuery() {

			if (equivs.size() == 1 && supers.size() == 0) {

				return equivs.get(0);
			}

			throw new RuntimeException("Cannot extract query: " + cls);
		}

		OWLAxiom extractEntailment() {

			if (equivs.size() == 2 && supers.size() == 0) {

				return factory.getOWLEquivalentClassesAxiom(equivs.get(0), equivs.get(1));
			}

			if (equivs.size() == 1 && supers.size() == 1) {

				return factory.getOWLSubClassOfAxiom(equivs.get(0), supers.get(0));
			}

			throw new RuntimeException("Cannot extract entailment: " + cls);
		}

		private void extractExpressions() {

			for (OWLAxiom ax : getAllClassAxioms(cls, Imports.INCLUDED)) {

				if (ax instanceof OWLEquivalentClassesAxiom) {

					extractEquivs((OWLEquivalentClassesAxiom)ax);
				}

				if (ax instanceof OWLSubClassOfAxiom) {

					extractSuper((OWLSubClassOfAxiom)ax);
				}
			}
		}

		private void extractEquivs(OWLEquivalentClassesAxiom ax) {

			for (OWLClassExpression e : ax.getClassExpressions()) {

				if (!e.equals(cls)) {

					equivs.add(e);
				}
			}
		}

		private void extractSuper(OWLSubClassOfAxiom ax) {

			if (ax.getSubClass().equals(cls)) {

				OWLClassExpression sup = ax.getSuperClass();

				if (!sup.equals(baseCls)) {

					supers.add(sup);
				}
			}
		}
	}

	OntoSpecifiedTestExtractor(TestManager manager) {

		this.manager = manager;

		factory = manager.factory;
		baseCls = getTestBaseClass();

		processTestClasses();
	}

	abstract String getTestBaseClassName();

	abstract void processTestClass(TestClass testClass);

	private void processTestClasses() {

		for (OWLClassExpression clsExp : getSubClasses(baseCls)) {

			OWLClass cls = (OWLClass)clsExp;

			processTestClass(new TestClass(cls));
			removeAllClassAxioms(cls);
		}

		removeAllClassAxioms(baseCls);
	}

	private Collection<OWLClassExpression> getSubClasses(OWLClass cls) {

		return EntitySearcher.getSubClasses(cls, manager.allOntologies);
	}

	private void removeAllClassAxioms(OWLClass cls) {

		for (OWLOntology o : manager.allOntologies) {

			for (OWLAxiom ax : getAllClassAxioms(cls, Imports.EXCLUDED)) {

				manager.manager.removeAxiom(o, ax);
			}
		}
	}

	private Set<OWLClassAxiom> getAllClassAxioms(OWLClass cls, Imports importsStatus) {

		return manager.rootOntology.getAxioms(cls, importsStatus);
	}

	private OWLClass getTestBaseClass() {

		return factory.getOWLClass(getTestBaseClassIRI());
	}

	private IRI getTestBaseClassIRI() {

		return IRI.create(TEST_CLASS_IRI_BASE + getTestBaseClassName());
	}
}
