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

/**
 * @author Colin Puleston
 */
class RekonOps {

	private OWLClass owlThing;
	private OWLClass owlNothing;

	private OWLClassNode owlThingAsEquivGroup;
	private OWLClassNode owlNothingAsEquivGroup;

	private OWLClassNodeSet emptyEquivClassGroups = new OWLClassNodeSet();

	private Names mappedClassNames;
	private DynamicOpsInvoker dynamicOps;

	private EquivalenceChecker equivalenceChecker = new EquivalenceChecker();
	private SameIndividualsChecker sameIndividualsChecker = new SameIndividualsChecker();

	private ClassesGrouper classesGrouper = new ClassesGrouper();
	private SuperClassesGrouper superClassesGrouper = new SuperClassesGrouper();
	private SubClassesGrouper subClassesGrouper = new SubClassesGrouper();
	private IndividualsGrouper individualsGrouper = new IndividualsGrouper();

	private abstract class EntitiesGrouper<E extends OWLEntity> {

		private Class<E> entityType;

		EntitiesGrouper(Class<E> entityType) {

			this.entityType = entityType;
		}

		NodeSet<E> toEquivGroups(Names names) {

			return createGroupsNode(toEquivGroupsSet(names));
		}

		Set<Node<E>> toEquivGroupsSet(Names names) {

			Set<Node<E>> groups = new HashSet<Node<E>>();
			NameSet initialGroupElements = new NameSet();

			for (Name n : names.getNames()) {

				Names equivs = n.getEquivalents();

				if (equivs.isEmpty()) {

					checkAddEquivGroup(groups, n, Names.NO_NAMES);
				}
				else {

					if (!initialGroupElements.containsAny(equivs)) {

						checkAddEquivGroup(groups, n, equivs);

						initialGroupElements.add(n);
					}
				}
			}

			return groups;
		}

		abstract Node<E> createGroupNode(Set<E> entities);

		abstract NodeSet<E> createGroupsNode(Set<Node<E>> groups);

		void checkAddEntities(Set<E> entities, Name name) {

			if (name.mapped()) {

				entities.add(MappedNames.toMappedEntity(name, entityType));
			}
		}

		private void checkAddEquivGroup(Set<Node<E>> groups, Name name, Names equivs) {

			Set<E> entities = new HashSet<E>();

			checkAddEntities(entities, name);

			for (Name e : equivs.getNames()) {

				checkAddEntities(entities, e);
			}

			if (!entities.isEmpty()) {

				groups.add(createGroupNode(entities));
			}
		}
	}

	private class ClassesGrouper extends EntitiesGrouper<OWLClass> {

		ClassesGrouper() {

			super(OWLClass.class);
		}

		Node<OWLClass> equivsToGroup(Names equivs, OWLClassExpression sourceExpr) {

			Set<OWLClass> entities = new HashSet<OWLClass>();

			for (Name n : equivs.getNames()) {

				checkAddEntities(entities, n);
			}

			return new OWLClassNode(entities);
		}

		Node<OWLClass> createGroupNode(Set<OWLClass> entities) {

			return new OWLClassNode(entities);
		}

		NodeSet<OWLClass> createGroupsNode(Set<Node<OWLClass>> groups) {

			return new OWLClassNodeSet(groups);
		}
	}

	private abstract class LinkedClassesGrouper extends ClassesGrouper {

		NodeSet<OWLClass> toEquivGroups(Names names, boolean direct) {

			if (direct && anyFreeNames(names)) {

				names = resolveForFreeDirects(names);
			}

			Set<Node<OWLClass>> groups = toEquivGroupsSet(names);

			if (!direct || groups.isEmpty()) {

				groups.add(getDefaultEquivGroup());
			}

			return new OWLClassNodeSet(groups);
		}

		abstract OWLClassNode getDefaultEquivGroup();

		abstract Names getLinked(Name name, boolean direct);

		private Names resolveForFreeDirects(Names rawDirects) {

			NameSet resDirects = new NameSet();

			for (Name d : rawDirects.getNames()) {

				collectLinkedMappeds(resDirects, d);
			}

			for (Name d : resDirects.copyNames()) {

				resDirects.removeAll(getLinked(d, false));
			}

			return resDirects;
		}

		private void collectLinkedMappeds(NameSet collected, Name n) {

			if (!n.mapped() || collected.add(n)) {

				for (Name l : getLinked(n, true).getNames()) {

					collectLinkedMappeds(collected, l);
				}
			}
		}

		private boolean anyFreeNames(Names names) {

			for (Name n : names.getNames()) {

				if (!n.mapped()) {

					return true;
				}
			}

			return false;
		}
	}

	private class SuperClassesGrouper extends LinkedClassesGrouper {

		OWLClassNode getDefaultEquivGroup() {

			return owlThingAsEquivGroup;
		}

		Names getLinked(Name name, boolean direct) {

			return name.getSupers(direct);
		}
	}

	private class SubClassesGrouper extends LinkedClassesGrouper {

		OWLClassNode getDefaultEquivGroup() {

			return owlNothingAsEquivGroup;
		}

		Names getLinked(Name name, boolean direct) {

			return name.getSubs(ClassName.class, direct);
		}
	}

	private class IndividualsGrouper extends EntitiesGrouper<OWLNamedIndividual> {

		IndividualsGrouper() {

			super(OWLNamedIndividual.class);
		}

		Node<OWLNamedIndividual> createGroupNode(Set<OWLNamedIndividual> entities) {

			return new OWLNamedIndividualNode(entities);
		}

		NodeSet<OWLNamedIndividual> createGroupsNode(Set<Node<OWLNamedIndividual>> groups) {

			return new OWLNamedIndividualNodeSet(groups);
		}
	}

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

			return dynamicOps.equivalents(e1, e2);
		}
	}

	private class SameIndividualsChecker extends PairwiseMatchChecker<OWLNamedIndividual> {

		boolean match(OWLNamedIndividual e1, OWLNamedIndividual e2) {

			return dynamicOps.sameIndividuals(e1, e2);
		}
	}

	RekonOps(OWLOntologyManager manager) {

		OWLDataFactory factory = manager.getOWLDataFactory();

		owlThing = factory.getOWLThing();
		owlNothing = factory.getOWLNothing();

		owlThingAsEquivGroup = new OWLClassNode(owlThing);
		owlNothingAsEquivGroup = new OWLClassNode(owlNothing);

		Assertions assertions = new Assertions(manager);
		OntologyInitialiserImpl ontoInit = new OntologyInitialiserImpl(assertions);
		Ontology ontology = new Ontology(ontoInit);

		mappedClassNames = new NameList(ontoInit.getClassNames());
		dynamicOps = ontoInit.createDynamicOpsInvoker(ontology);
	}

	Node<OWLClass> getEquivalentClasses(OWLClassExpression expr) {

		if (expr.equals(owlThing)) {

			return owlThingAsEquivGroup;
		}

		if (expr.equals(owlNothing)) {

			return owlNothingAsEquivGroup;
		}

		expr = resolveInputExpr(expr);

		Names equivs = dynamicOps.getEquivalents(expr);

		return classesGrouper.equivsToGroup(equivs, expr);
	}

	NodeSet<OWLClass> getSuperClasses(OWLClassExpression expr, boolean direct) {

		if (expr.equals(owlThing)) {

			return emptyEquivClassGroups;
		}

		return superClassesGrouper.toEquivGroups(getSuperNames(expr, direct), direct);
	}

	NodeSet<OWLClass> getSubClasses(OWLClassExpression expr, boolean direct) {

		if (expr.equals(owlNothing)) {

			return emptyEquivClassGroups;
		}

		return subClassesGrouper.toEquivGroups(getSubNames(expr, direct), direct);
	}

	NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression expr, boolean direct) {

		Names insts = dynamicOps.getInstances(resolveInputExpr(expr), direct);

		return individualsGrouper.toEquivGroups(insts);
	}

	NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct) {

		Names types = dynamicOps.getTypes(ind, direct);

		return classesGrouper.toEquivGroups(types);
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

	private boolean equivalents(Set<OWLClassExpression> exprs) {

		return equivalenceChecker.allMatch(resolveInputExprs(exprs));
	}

	private boolean sameIndividuals(Set<OWLIndividual> inds) {

		Set<OWLNamedIndividual> namedInds = toNamedIndividuals(inds);

		return namedInds != null && sameIndividualsChecker.allMatch(namedInds);
	}

	private boolean subsumption(OWLClassExpression sup, OWLClassExpression sub) {

		return dynamicOps.subsumption(resolveInputExpr(sup), resolveInputExpr(sub));
	}

	private boolean instanceOf(OWLClassExpression type, OWLIndividual ind) {

		if (ind instanceof OWLNamedIndividual) {

			return dynamicOps.instanceOf(resolveInputExpr(type), (OWLNamedIndividual)ind);
		}

		return false;
	}

	private boolean instanceOf(OWLClassExpression type, OWLNamedIndividual ind) {

		return dynamicOps.instanceOf(resolveInputExpr(type), ind);
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

	private Set<OWLClassExpression> resolveInputExprs(Set<OWLClassExpression> ins) {

		Set<OWLClassExpression> outs = new HashSet<OWLClassExpression>();

		for (OWLClassExpression in : ins) {

			outs.add(resolveInputExpr(in));
		}

		return outs;
	}

	private OWLClassExpression resolveInputExpr(OWLClassExpression expr) {

		if (expr instanceof OWLObjectIntersectionOf) {

			return checkExtractSingleOperand((OWLObjectIntersectionOf)expr);
		}

		if (expr instanceof OWLObjectUnionOf) {

			return checkExtractSingleOperand((OWLObjectUnionOf)expr);
		}

		return expr;
	}

	private OWLClassExpression checkExtractSingleOperand(OWLNaryBooleanClassExpression expr) {

		Set<OWLClassExpression> ops = expr.getOperands();

		return ops.size() == 1 ? ops.iterator().next() : expr;
	}

	private Names getSuperNames(OWLClassExpression expr, boolean direct) {

		if (expr.equals(owlNothing)) {

			return direct ? getLeafClassNames() : mappedClassNames;
		}

		return dynamicOps.getSupers(resolveInputExpr(expr), direct);
	}

	private Names getSubNames(OWLClassExpression expr, boolean direct) {

		return dynamicOps.getSubs(resolveInputExpr(expr), direct);
	}

	private Names getLeafClassNames() {

		NameSet leafs = new NameSet();

		for (Name n : mappedClassNames.getNames()) {

			if (n.getSubs(ClassName.class, true).isEmpty()) {

				leafs.add(n);
			}
		}

		return leafs;
	}
}
