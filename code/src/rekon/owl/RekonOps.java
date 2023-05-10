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

package rekon.owl;

import java.util.*;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class RekonOps {

	private OWLClass owlThing;
	private OWLClass owlNothing;

	private MappedNames mappedNames;

	private DynamicOps dynamicOps;
	private InstanceOps instanceOps;

	private EquivalentsGrouper equivalentsGrouper;

	private EquivalenceChecker equivalenceChecker = new EquivalenceChecker();
	private SameIndividualsChecker sameIndividualsChecker = new SameIndividualsChecker();

	private abstract class PairwiseMatchChecker<E> {

		boolean allMatch(Set<E> entities) {

			if (entities.size() < 2) {

				return true;
			}

			Iterator<E> i = entities.iterator();
			E first = i.next();

			do {

				if (!match(first, i.next())) {

					return false;
				}
			}
			while (i.hasNext());

			return true;
		}

		abstract boolean match(E e1, E e2);
	}

	private class EquivalenceChecker extends PairwiseMatchChecker<OWLClassExpression> {

		boolean match(OWLClassExpression e1, OWLClassExpression e2) {

			return toDynamicHandler(e1).equivalentTo(toDynamicHandler(e2));
		}
	}

	private class SameIndividualsChecker extends PairwiseMatchChecker<OWLNamedIndividual> {

		boolean match(OWLNamedIndividual e1, OWLNamedIndividual e2) {

			return toDynamicHandler(e1).equivalentTo(toDynamicHandler(e2));
		}
	}

	private class DynamicExprPatternCreator extends ExpressionPatternCreator {

		DynamicExprPatternCreator(OWLClassExpression expr) {

			super(expr);
		}

		MatchComponents createMatchComponents(MatchStructures matchStructures) {

			return new MatchComponents(mappedNames, matchStructures, true);
		}
	}

	RekonOps(OWLOntologyManager manager) {

		OWLDataFactory factory = manager.getOWLDataFactory();

		owlThing = factory.getOWLThing();
		owlNothing = factory.getOWLNothing();

		Assertions assertions = new Assertions(manager);

		mappedNames = new MappedNames(assertions);

		Ontology ontology = createOntology(assertions);

		dynamicOps = ontology.createDynamicOps();
		instanceOps = ontology.createInstanceOps();

		equivalentsGrouper = new EquivalentsGrouper(factory);
	}

	RekonInstanceBox createInstanceBox() {

		return new RekonInstanceBox(instanceOps, mappedNames);
	}

	Node<OWLClass> getEquivalentClasses(OWLClassExpression expr) {

		if (expr.equals(owlThing)) {

			return equivalentsGrouper.owlThingAsEquivGroup;
		}

		if (expr.equals(owlNothing)) {

			return equivalentsGrouper.owlNothingAsEquivGroup;
		}

		Names equivs = toDynamicHandler(expr).getEquivalents();

		return equivalentsGrouper.equivsToGroup(equivs, expr);
	}

	NodeSet<OWLClass> getSuperClasses(OWLClassExpression expr, boolean direct) {

		if (expr.equals(owlThing)) {

			return equivalentsGrouper.emptyEquivClassGroups;
		}

		return equivalentsGrouper.groupSupers(getSuperNames(expr, direct), direct);
	}

	NodeSet<OWLClass> getSubClasses(OWLClassExpression expr, boolean direct) {

		if (expr.equals(owlNothing)) {

			return equivalentsGrouper.emptyEquivClassGroups;
		}

		return equivalentsGrouper.groupSubs(getSubNames(expr, direct), direct);
	}

	NodeSet<OWLNamedIndividual> getIndividuals(OWLClassExpression expr, boolean direct) {

		Names insts = toDynamicHandler(expr).getIndividuals(direct);

		return equivalentsGrouper.groupIndividuals(insts);
	}

	NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct) {

		Names types = toDynamicHandler(ind).getSupers(direct);

		return equivalentsGrouper.groupClasses(types);
	}

	boolean isEntailed(OWLAxiom axiom) {

		if (axiom instanceof OWLEquivalentClassesAxiom) {

			OWLEquivalentClassesAxiom equAx = (OWLEquivalentClassesAxiom)axiom;

			return equivalents(equAx.getClassExpressions());
		}

		if (axiom instanceof OWLSubClassOfAxiom) {

			OWLSubClassOfAxiom subAx = (OWLSubClassOfAxiom)axiom;

			return subsumption(subAx.getSuperClass(), subAx.getSubClass());
		}

		if (axiom instanceof OWLClassAssertionAxiom) {

			OWLClassAssertionAxiom typeAx = (OWLClassAssertionAxiom)axiom;

			return instanceOf(typeAx.getClassExpression(), typeAx.getIndividual());
		}

		if (axiom instanceof OWLSameIndividualAxiom) {

			OWLSameIndividualAxiom sameAx = (OWLSameIndividualAxiom)axiom;

			return sameIndividuals(sameAx.getIndividuals());
		}

		return false;
	}

	private Ontology createOntology(Assertions assertions) {

		return new Ontology(mappedNames, createStructureBuilder(assertions));
	}

	private StructureBuilder createStructureBuilder(Assertions assertions) {

		return new RekonStructureBuilder(assertions, mappedNames);
	}

	private Names getSuperNames(OWLClassExpression expr, boolean direct) {

		if (expr.equals(owlNothing)) {

			if (direct) {

				return getLeafClassNames();
			}

			return new NameList(mappedNames.getClassNames());
		}

		return toDynamicHandler(expr).getSupers(direct);
	}

	private Names getSubNames(OWLClassExpression expr, boolean direct) {

		return toDynamicHandler(expr).getSubs(direct);
	}

	private boolean equivalents(Set<OWLClassExpression> exprs) {

		return equivalenceChecker.allMatch(exprs);
	}

	private boolean sameIndividuals(Set<OWLIndividual> inds) {

		Set<OWLNamedIndividual> namedInds = toNamedIndividuals(inds);

		return namedInds != null && sameIndividualsChecker.allMatch(namedInds);
	}

	private boolean subsumption(OWLClassExpression sup, OWLClassExpression sub) {

		return toDynamicHandler(sup).subsumes(toDynamicHandler(sub));
	}

	private boolean instanceOf(OWLClassExpression type, OWLIndividual ind) {

		return ind instanceof OWLNamedIndividual
				&& instanceOf(type, (OWLNamedIndividual)ind);
	}

	private boolean instanceOf(OWLClassExpression type, OWLNamedIndividual ind) {

		return toDynamicHandler(type).subsumes(toDynamicHandler(ind));
	}

	private Names getLeafClassNames() {

		NameSet leafs = new NameSet();

		for (Name n : mappedNames.getClassNames()) {

			if (n.getSubs(ClassName.class, true).isEmpty()) {

				leafs.add(n);
			}
		}

		return leafs;
	}

	private Set<OWLNamedIndividual> toNamedIndividuals(Set<OWLIndividual> inds) {

		Set<OWLNamedIndividual> namedInds = new HashSet<OWLNamedIndividual>();

		for (OWLIndividual ind : inds) {

			if (ind instanceof OWLNamedIndividual) {

				namedInds.add((OWLNamedIndividual)ind);
			}
			else {

				return null;
			}
		}

		return namedInds;
	}

	private DynamicOpsHandler toDynamicHandler(OWLClassExpression expr) {

		if (expr instanceof OWLClass) {

			return toDynamicHandler((OWLClass)expr);
		}

		expr = resolveInputExpr(expr);

		return dynamicOps.createHandler(new DynamicExprPatternCreator(expr));
	}

	private DynamicOpsHandler toDynamicHandler(OWLClass cls) {

		return dynamicOps.createHandler(mappedNames.get(cls));
	}

	private DynamicOpsHandler toDynamicHandler(OWLNamedIndividual ind) {

		return dynamicOps.createHandler(mappedNames.get(ind));
	}

	private OWLClassExpression resolveInputExpr(OWLClassExpression expr) {

		if (expr instanceof OWLNaryBooleanClassExpression) {

			OWLNaryBooleanClassExpression bExpr = (OWLNaryBooleanClassExpression)expr;
			Set<OWLClassExpression> ops = bExpr.getOperands();

			if (ops.size() == 1) {

				return ops.iterator().next();
			}
		}

		return expr;
	}
}
