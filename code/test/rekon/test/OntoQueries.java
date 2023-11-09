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

class OntoQueries {

	static private final IRI QUERIES_BASE_CLASS_IRI = IRI.create("urn:test#_Query");

	private TestManager manager;
	private List<OWLClassExpression> queryList = new ArrayList<OWLClassExpression>();

	private class QueryExtractor {

		private TestManager manager;

		QueryExtractor(TestManager manager) {

			this.manager = manager;

			extractQueries();
		}

		private void extractQueries() {

			OWLClass baseCls = getBaseQueryClass();

			for (OWLClassExpression clsExp : getSubClasses(baseCls)) {

				OWLClass cls = (OWLClass)clsExp;
				OWLClassExpression q = lookForQuery(cls);

				if (q != null) {

					queryList.add(q);
				}

				removeAllClassAxioms(cls);
			}

			removeAllClassAxioms(baseCls);
		}

		private OWLClassExpression lookForQuery(OWLClass cls) {

			for (OWLAxiom ax : getAllClassAxioms(cls, Imports.INCLUDED)) {

				if (ax instanceof OWLEquivalentClassesAxiom) {

					return extractQuery((OWLEquivalentClassesAxiom)ax);
				}
			}

			return null;
		}

		private OWLClassExpression extractQuery(OWLEquivalentClassesAxiom ax) {

			for (OWLClassExpression e : ax.getClassExpressions()) {

				if (!(e instanceof OWLClass)) {

					return e;
				}
			}

			return null;
		}

		private OWLClass getBaseQueryClass() {

			return manager.getFactory().getOWLClass(QUERIES_BASE_CLASS_IRI);
		}

		private Collection<OWLClassExpression> getSubClasses(OWLClass cls) {

			return EntitySearcher.getSubClasses(cls, manager.getAllOntologies());
		}

		private void removeAllClassAxioms(OWLClass cls) {

			for (OWLOntology o : manager.getAllOntologies()) {

				for (OWLAxiom ax : getAllClassAxioms(cls, Imports.EXCLUDED)) {

					manager.manager.removeAxiom(o, ax);
				}
			}
		}

		private Set<OWLClassAxiom> getAllClassAxioms(OWLClass cls, Imports importsStatus) {

			return manager.rootOntology.getAxioms(cls, importsStatus);
		}
	}

	private class QueryListIterator extends QueryProvider {

		private Iterator<OWLClassExpression> queries = queryList.iterator();

		OWLClassExpression next() {

			return queries.hasNext() ? queries.next() : null;
		}
	}

	OntoQueries(TestManager manager) {

		this.manager = manager;

		new QueryExtractor(manager);
	}

	QueryProvider createQueryProvider() {

		return new QueryListIterator();
	}
}
