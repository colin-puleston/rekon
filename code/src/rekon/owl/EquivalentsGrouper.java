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

import gnu.trove.set.hash.*;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.impl.*;

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class EquivalentsGrouper {

	private Node<OWLClass> owlThingAsEquivGroup;
	private Node<OWLClass> owlNothingAsEquivGroup;

	private ClassesGrouper classesGrouper = new ClassesGrouper();
	private SupersGrouper supersGrouper = new SupersGrouper();
	private SubsGrouper subsGrouper = new SubsGrouper();
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

			Set<Node<E>> groups = new THashSet<Node<E>>();
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

		void checkAddEntity(Set<E> entities, Name name) {

			if (name.mapped()) {

				entities.add(MappedNames.toMappedEntity(name, entityType));
			}
		}

		private void checkAddEquivGroup(Set<Node<E>> groups, Name name, Names equivs) {

			Set<E> entities = new THashSet<E>();

			checkAddEntity(entities, name);

			for (Name e : equivs.getNames()) {

				checkAddEntity(entities, e);
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

			Set<OWLClass> entities = new THashSet<OWLClass>();

			for (Name n : equivs.getNames()) {

				checkAddEntity(entities, n);
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

		abstract Node<OWLClass> getDefaultEquivGroup();

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

	private class SupersGrouper extends LinkedClassesGrouper {

		Node<OWLClass> getDefaultEquivGroup() {

			return owlThingAsEquivGroup;
		}

		Names getLinked(Name name, boolean direct) {

			return name.getSupers(direct);
		}
	}

	private class SubsGrouper extends LinkedClassesGrouper {

		Node<OWLClass> getDefaultEquivGroup() {

			return owlNothingAsEquivGroup;
		}

		Names getLinked(Name name, boolean direct) {

			return name.getSubs(ClassNode.class, direct);
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

	EquivalentsGrouper(OWLClass owlThing, OWLClass owlNothing) {

		owlThingAsEquivGroup = new OWLClassNode(owlThing);
		owlNothingAsEquivGroup = new OWLClassNode(owlNothing);
	}

	Node<OWLClass> equivsToGroup(Names equivs, OWLClassExpression sourceExpr) {

		return classesGrouper.equivsToGroup(equivs, sourceExpr);
	}

	NodeSet<OWLClass> groupClasses(Names classes) {

		return classesGrouper.toEquivGroups(classes);
	}

	NodeSet<OWLClass> groupSupers(Names supers, boolean direct) {

		return supersGrouper.toEquivGroups(supers, direct);
	}

	NodeSet<OWLClass> groupSubs(Names subs, boolean direct) {

		return subsGrouper.toEquivGroups(subs, direct);
	}

	NodeSet<OWLNamedIndividual> groupIndividuals(Names inds) {

		return individualsGrouper.toEquivGroups(inds);
	}
}
