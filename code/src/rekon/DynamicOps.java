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

package rekon;

import java.util.*;

import org.semanticweb.owlapi.model.*;

/**
 * @author Colin Puleston
 */
class DynamicOps {

	private MappedNames mappedNames;
	private DynamicPatternOps patternOps;

	DynamicOps(Ontology ontology) {

		mappedNames = ontology.getMappedNames();
		patternOps = new DynamicPatternOps(ontology);
	}

	Names getEquivalents(OWLClassExpression expr) {

		return createOpsHandler(expr).getEquivalents();
	}

	Names getSupers(OWLClassExpression expr, boolean direct) {

		return createOpsHandler(expr).getSupers(direct);
	}

	Names getSubs(OWLClassExpression expr, boolean direct) {

		return createOpsHandler(expr).getSubs(direct);
	}

	Names getInstances(OWLClassExpression expr, boolean direct) {

		return createOpsHandler(expr).getInstances(direct);
	}

	Names getTypes(OWLNamedIndividual ind, boolean direct) {

		return createNodeOpsHandler(ind).getSupers(direct);
	}

	boolean equivalents(OWLClassExpression e1, OWLClassExpression e2) {

		return createOpsHandler(e1).equivalentTo(createOpsHandler(e2));
	}

	boolean subsumption(OWLClassExpression sup, OWLClassExpression sub) {

		return createOpsHandler(sup).subsumes(createOpsHandler(sub));
	}

	boolean instanceOf(OWLClassExpression type, OWLNamedIndividual ind) {

		return createOpsHandler(type).subsumes(createNodeOpsHandler(ind));
	}

	boolean sameIndividuals(OWLNamedIndividual i1, OWLNamedIndividual i2) {

		return createNodeOpsHandler(i1).equivalentTo(createNodeOpsHandler(i2));
	}

	private DynamicOpsHandler createOpsHandler(OWLClassExpression expr) {

		if (expr instanceof OWLClass) {

			return createNodeOpsHandler((OWLClass)expr);
		}

		return patternOps.createHandler(expr);
	}

	private DynamicNodeOpsHandler createNodeOpsHandler(OWLClass cls) {

		return new DynamicNodeOpsHandler(mappedNames.get(cls));
	}

	private DynamicNodeOpsHandler createNodeOpsHandler(OWLNamedIndividual ind) {

		return new DynamicNodeOpsHandler(mappedNames.get(ind));
	}
}
