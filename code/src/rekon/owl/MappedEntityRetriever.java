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
class MappedEntityRetriever {

	private Node<OWLClass> owlThingAsEntityGroup;
	private Node<OWLClass> owlNothingAsEntityGroup;

	private ClassesRetriever classesRetriever = new ClassesRetriever();
	private SupersRetriever supersRetriever = new SupersRetriever();
	private SubsRetriever subsRetriever = new SubsRetriever();
	private IndividualsRetriever individualsRetriever = new IndividualsRetriever();

	private abstract class TypeEntitiesRetriever<E extends OWLEntity> {

		private Class<E> entityType;

		TypeEntitiesRetriever(Class<E> entityType) {

			this.entityType = entityType;
		}

		Node<E> toSingleEntityGroup(Names names) {

			return createGroupNode(toEntityGroup(names));
		}

		NodeSet<E> toEntityGroups(Collection<Names> nameGroups) {

			return createGroupsNode(toEntityGroupsSet(nameGroups));
		}

		Set<Node<E>> toEntityGroupsSet(Collection<Names> nameGroups) {

			Set<Node<E>> entityGroups = new HashSet<Node<E>>();

			for (Names nameGroup : nameGroups) {

				Set<E> entities = toEntityGroup(nameGroup);

				if (!entities.isEmpty()) {

					entityGroups.add(createGroupNode(entities));
				}
			}

			return entityGroups;
		}

		boolean insertedEntity(E entity) {

			return false;
		}

		abstract Node<E> createGroupNode(Set<E> entities);

		abstract NodeSet<E> createGroupsNode(Set<Node<E>> entityGroups);

		private Set<E> toEntityGroup(Names names) {

			Set<E> entities = new HashSet<E>();

			for (Name n : names) {

				E e = toMappedEntity(n);

				if (!insertedEntity(e)) {

					entities.add(e);
				}
			}

			return entities;
		}

		private E toMappedEntity(Name name) {

			return MappedNames.toMappedEntity(name, entityType);
		}
	}

	private class ClassesRetriever extends TypeEntitiesRetriever<OWLClass> {

		ClassesRetriever() {

			super(OWLClass.class);
		}

		boolean insertedEntity(OWLClass entity) {

			return OwlRestrictionResolver.isNoValueClass(entity);
		}

		Node<OWLClass> createGroupNode(Set<OWLClass> entities) {

			return new OWLClassNode(entities);
		}

		NodeSet<OWLClass> createGroupsNode(Set<Node<OWLClass>> entityGroups) {

			return new OWLClassNodeSet(entityGroups);
		}
	}

	private abstract class LinkedClassesRetriever extends ClassesRetriever {

		NodeSet<OWLClass> toEntityGroups(Collection<Names> nameGroups, boolean direct) {

			Set<Node<OWLClass>> entityGroups = toEntityGroupsSet(nameGroups);

			if (!direct || entityGroups.isEmpty()) {

				entityGroups.add(getDefaultEntityGroup());
			}

			return new OWLClassNodeSet(entityGroups);
		}

		abstract Node<OWLClass> getDefaultEntityGroup();
	}

	private class SupersRetriever extends LinkedClassesRetriever {

		Node<OWLClass> getDefaultEntityGroup() {

			return owlThingAsEntityGroup;
		}
	}

	private class SubsRetriever extends LinkedClassesRetriever {

		Node<OWLClass> getDefaultEntityGroup() {

			return owlNothingAsEntityGroup;
		}
	}

	private class IndividualsRetriever extends TypeEntitiesRetriever<OWLNamedIndividual> {

		IndividualsRetriever() {

			super(OWLNamedIndividual.class);
		}

		Node<OWLNamedIndividual> createGroupNode(Set<OWLNamedIndividual> entities) {

			return new OWLNamedIndividualNode(entities);
		}

		NodeSet<OWLNamedIndividual> createGroupsNode(Set<Node<OWLNamedIndividual>> entityGroups) {

			return new OWLNamedIndividualNodeSet(entityGroups);
		}
	}

	MappedEntityRetriever(OWLClass owlThing, OWLClass owlNothing) {

		owlThingAsEntityGroup = new OWLClassNode(owlThing);
		owlNothingAsEntityGroup = new OWLClassNode(owlNothing);
	}

	Node<OWLClass> retrieveEquivs(Names equivs, OWLClassExpression sourceExpr) {

		return classesRetriever.toSingleEntityGroup(equivs);
	}

	NodeSet<OWLClass> retrieveClasses(Collection<Names> equivGroups) {

		return classesRetriever.toEntityGroups(equivGroups);
	}

	NodeSet<OWLClass> retrieveSupers(Collection<Names> equivGroups, boolean direct) {

		return supersRetriever.toEntityGroups(equivGroups, direct);
	}

	NodeSet<OWLClass> retrieveSubs(Collection<Names> equivGroups, boolean direct) {

		return subsRetriever.toEntityGroups(equivGroups, direct);
	}

	NodeSet<OWLNamedIndividual> retrieveIndividuals(Collection<Names> equivGroups) {

		return individualsRetriever.toEntityGroups(equivGroups);
	}
}
