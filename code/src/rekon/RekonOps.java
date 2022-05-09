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
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.impl.*;

/**
 * @author Colin Puleston
 */
class RekonOps {

	private OWLClass owlThing;
	private OWLClass owlNothing;

	private OWLClassNode owlThingAsEquivGroup;
	private OWLClassNode owlNothingAsEquivGroup;

	private OWLClassNodeSet emptyEquivClassGroups = new OWLClassNodeSet();

	private DynamicOps dynamicOps;

	private EquivalenceChecker equivalenceChecker = new EquivalenceChecker();
	private SameIndividualsChecker sameIndividualsChecker = new SameIndividualsChecker();

	private OWLClassesGrouper owlClassesGrouper;
	private OWLSuperClassesGrouper owlSuperClassesGrouper;
	private OWLSubClassesGrouper owlSubClassesGrouper;
	private OWLIndividualsGrouper owlIndividualsGrouper = new OWLIndividualsGrouper();

	private abstract class OWLGrouper<E extends OWLEntity> {

		private Class<E> entityType;

		OWLGrouper(Class<E> entityType) {

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

		void checkAddOWLEntities(Set<E> entities, Name name) {

			if (name.mapped()) {

				addOWLEntity(entities, name);
			}
		}

		void addOWLEntity(Set<E> entities, Name name) {

			entities.add(name.getOWLEntity(entityType));
		}

		private void checkAddEquivGroup(Set<Node<E>> groups, Name name, Names equivs) {

			Set<E> entities = new HashSet<E>();

			checkAddOWLEntities(entities, name);

			for (Name e : equivs.getNames()) {

				checkAddOWLEntities(entities, e);
			}

			if (!entities.isEmpty()) {

				groups.add(createGroupNode(entities));
			}
		}
	}

	private class OWLClassesGrouper extends OWLGrouper<OWLClass> {

		OWLClassesGrouper() {

			super(OWLClass.class);
		}

		Node<OWLClass> equivsToGroup(Names equivs, OWLClassExpression sourceExpr) {

			Set<OWLClass> entities = new HashSet<OWLClass>();

			for (Name n : equivs.getNames()) {

				checkAddOWLEntities(entities, n);
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

	private abstract class OWLLinkedClassesGrouper extends OWLClassesGrouper {

		private NameSet mappedDirectsSubset = null;

		NodeSet<OWLClass> toEquivGroups(Names names, boolean direct) {

			Set<Node<OWLClass>> groups = toEquivGroupsSet(names, direct);

			if (!direct || groups.isEmpty()) {

				groups.add(getDefaultEquivGroup());
			}

			return new OWLClassNodeSet(groups);
		}

		void checkAddOWLEntities(Set<OWLClass> entities, Name name) {

			if (name.mapped()) {

				addOWLEntity(entities, name);
			}
			else {

				if (mappedDirectsSubset != null) {

					addOWLEntitiesForClosestMapped(entities, name);
				}
			}
		}

		abstract OWLClassNode getDefaultEquivGroup();

		abstract Names getDirectlyLinkeds(Name name);

		abstract Names getReverseDirectlyLinkeds(Name name);

		private Set<Node<OWLClass>> toEquivGroupsSet(Names names, boolean direct) {

			if (direct) {

				configureForAnyFreeDirects(names);
			}

			Set<Node<OWLClass>> groups = toEquivGroupsSet(names);

			mappedDirectsSubset = null;

			return groups;
		}

		private void configureForAnyFreeDirects(Names names) {

			List<Name> mappedDirects = new ArrayList<Name>();

			for (Name n : names.getNames()) {

				if (n.mapped()) {

					mappedDirects.add(n);
				}
			}

			if (mappedDirects.size() != names.size()) {

				mappedDirectsSubset = new NameSet(mappedDirects);
			}
		}

		private void addOWLEntitiesForClosestMapped(Set<OWLClass> entities, Name name) {

			for (Name ln : getDirectlyLinkeds(name).getNames()) {

				if (ln.mapped()) {

					if (!mappedPathToAnyMappedDirect(ln)) {

						addOWLEntity(entities, ln);
					}
				}
				else {

					addOWLEntitiesForClosestMapped(entities, ln);
				}
			}
		}

		private boolean mappedPathToAnyMappedDirect(Name name) {

			for (Name ln : getReverseDirectlyLinkeds(name).getNames()) {

				if (ln.mapped()) {

					if (mappedDirectsSubset.contains(ln)
						|| mappedPathToAnyMappedDirect(ln)) {

						return true;
					}
				}
			}

			return false;
		}
	}

	private class OWLSuperClassesGrouper extends OWLLinkedClassesGrouper {

		OWLClassNode getDefaultEquivGroup() {

			return owlThingAsEquivGroup;
		}

		Names getDirectlyLinkeds(Name name) {

			return name.getSupers(true);
		}

		Names getReverseDirectlyLinkeds(Name name) {

			return name.getSubs(ClassName.class, true);
		}
	}

	private class OWLSubClassesGrouper extends OWLLinkedClassesGrouper {

		OWLClassNode getDefaultEquivGroup() {

			return owlNothingAsEquivGroup;
		}

		Names getDirectlyLinkeds(Name name) {

			return name.getSubs(ClassName.class, true);
		}

		Names getReverseDirectlyLinkeds(Name name) {

			return name.getSupers(true);
		}
	}

	private class OWLIndividualsGrouper extends OWLGrouper<OWLNamedIndividual> {

		OWLIndividualsGrouper() {

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

		dynamicOps = new DynamicOps(new Ontology(new Assertions(manager)));

		OWLDataFactory factory = manager.getOWLDataFactory();

		owlThing = factory.getOWLThing();
		owlNothing = factory.getOWLNothing();

		owlThingAsEquivGroup = new OWLClassNode(owlThing);
		owlNothingAsEquivGroup = new OWLClassNode(owlNothing);

		owlClassesGrouper = new OWLClassesGrouper();
		owlSubClassesGrouper = new OWLSubClassesGrouper();
		owlSuperClassesGrouper = new OWLSuperClassesGrouper();
	}

	Node<OWLClass> getEquivalentClasses(OWLClassExpression expr) {

		if (expr.equals(owlThing)) {

			return owlThingAsEquivGroup;
		}

		expr = resolveInputExpr(expr);

		Names equivs = dynamicOps.getEquivalents(expr);

		return owlClassesGrouper.equivsToGroup(equivs, expr);
	}

	NodeSet<OWLClass> getSuperClasses(OWLClassExpression expr, boolean direct) {

		if (expr.equals(owlThing)) {

			return emptyEquivClassGroups;
		}

		Names sups = dynamicOps.getSupers(resolveInputExpr(expr), direct);

		return owlSuperClassesGrouper.toEquivGroups(sups, direct);
	}

	NodeSet<OWLClass> getSubClasses(OWLClassExpression expr, boolean direct) {

		Names subs = dynamicOps.getSubs(resolveInputExpr(expr), direct);

		return owlSubClassesGrouper.toEquivGroups(subs, direct);
	}

	NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression expr, boolean direct) {

		Names insts = dynamicOps.getInstances(resolveInputExpr(expr), direct);

		return owlIndividualsGrouper.toEquivGroups(insts);
	}

	NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct) {

		Names types = dynamicOps.getTypes(ind, direct);

		return owlClassesGrouper.toEquivGroups(types);
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
}
