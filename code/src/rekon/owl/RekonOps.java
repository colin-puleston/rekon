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
import org.semanticweb.owlapi.reasoner.impl.*;

import rekon.core.*;
import rekon.build.*;
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
class RekonOps {

	static private NodeSet<OWLClass> EMPTY_EQUIV_CLASS_GROUPS = new OWLClassNodeSet();

	private OWLDataFactory factory;

	private OWLClass owlThing;
	private OWLClass owlNothing;

	private Node<OWLClass> owlThingAsEquivGroup;
	private Node<OWLClass> owlNothingAsEquivGroup;

	private MappedNames names;
	private CoreBuilder coreBuilder;
	private ExpressionConverter expressionConverter;

	private DynamicOps dynamicOps;
	private InstanceOps instanceOps;

	private EquivalenceChecker equivalenceChecker = new EquivalenceChecker();
	private SameIndividualsChecker sameIndividualsChecker = new SameIndividualsChecker();

	private MappedEntityRetriever mappedEntityRetriever;

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

	RekonOps(OWLOntologyManager manager) {

		factory = manager.getOWLDataFactory();

		owlThing = factory.getOWLThing();
		owlNothing = factory.getOWLNothing();

		owlThingAsEquivGroup = new OWLClassNode(owlThing);
		owlNothingAsEquivGroup = new OWLClassNode(owlNothing);

		names = new MappedNames(manager);
		coreBuilder = new CoreBuilder(names);
		expressionConverter = new ExpressionConverter(factory, names);

		Ontology ontology = new Ontology(names, createStructureBuilder(manager));

		dynamicOps = ontology.createDynamicOps();
		instanceOps = ontology.createInstanceOps();

		mappedEntityRetriever = new MappedEntityRetriever(owlThing, owlNothing);
	}

	RekonInstanceBox createInstanceBox() {

		return new RekonInstanceBox(instanceOps, names, expressionConverter);
	}

	Node<OWLClass> getEquivalentClasses(OWLClassExpression expr) {

		if (expr.equals(owlThing)) {

			return owlThingAsEquivGroup;
		}

		if (expr.equals(owlNothing)) {

			return owlNothingAsEquivGroup;
		}

		Names equivs = toDynamicHandler(expr).getEquivalentsGroup();

		return mappedEntityRetriever.mapEquivs(equivs, expr);
	}

	NodeSet<OWLClass> getSuperClasses(OWLClassExpression expr, boolean direct) {

		if (expr.equals(owlThing)) {

			return EMPTY_EQUIV_CLASS_GROUPS;
		}

		if (expr.equals(owlNothing)) {

			return mappedEntityRetriever.mapClasses(getOwlNothingSuperEquivGroups(direct));
		}

		Collection<Names> sups = toDynamicHandler(expr).getSuperEquivGroups(direct);

		return mappedEntityRetriever.mapSupers(sups, direct);
	}

	NodeSet<OWLClass> getSubClasses(OWLClassExpression expr, boolean direct) {

		if (expr.equals(owlNothing)) {

			return EMPTY_EQUIV_CLASS_GROUPS;
		}

		Collection<Names> subs = toDynamicHandler(expr).getSubEquivGroups(direct);

		return mappedEntityRetriever.mapSubs(subs, direct);
	}

	NodeSet<OWLNamedIndividual> getIndividuals(OWLClassExpression expr, boolean direct) {

		Collection<Names> inds = toDynamicHandler(expr).getIndividualEquivGroups(direct);

		return mappedEntityRetriever.mapIndividuals(inds);
	}

	NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct) {

		Collection<Names> types = toDynamicHandler(ind).getSuperEquivGroups(direct);

		return mappedEntityRetriever.mapClasses(types);
	}

	boolean isEntailed(OWLAxiom axiom) {

		if (axiom instanceof OWLEquivalentClassesAxiom) {

			OWLEquivalentClassesAxiom equAx = (OWLEquivalentClassesAxiom)axiom;

			return equivalenceChecker.allMatch(equAx.getClassExpressions());
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

	public StructureBuilder createStructureBuilder(OWLOntologyManager manager) {

		return coreBuilder.createStructureBuilder(createAxiomConverter(manager));
	}

	private AxiomConverter createAxiomConverter(OWLOntologyManager manager) {

		return new AxiomConverter(manager, names, expressionConverter);
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

	private Collection<Names> getOwlNothingSuperEquivGroups(boolean direct) {

		return dynamicOps.namesToEquivGroups(getOwlNothingSupers(direct));
	}

	private Names getOwlNothingSupers(boolean direct) {

		return direct ? getLeafClassNodes() : new NameList(names.getClassNodes());
	}

	private Names getLeafClassNodes() {

		NameSet leafs = new NameSet();

		for (Name n : names.getClassNodes()) {

			if (n.getSubs(ClassNode.class, true).isEmpty()) {

				leafs.add(n);
			}
		}

		return leafs;
	}

	private DynamicOpsHandler toDynamicHandler(OWLClassExpression expr) {

		if (expr instanceof OWLClass) {

			return toDynamicHandler((OWLClass)expr);
		}

		return dynamicOps.createHandler(toDynamicPatternBuilder(expr));
	}

	private DynamicOpsHandler toDynamicHandler(OWLClass cls) {

		return dynamicOps.createHandler(names.get(cls));
	}

	private DynamicOpsHandler toDynamicHandler(OWLNamedIndividual ind) {

		return dynamicOps.createHandler(names.get(ind));
	}

	private MultiPatternBuilder toDynamicPatternBuilder(OWLClassExpression expr) {

		InputComplexNode n = expressionConverter.toComplexNode(expr);

		return coreBuilder.createMultiPatternBuilder(n);
	}
}
