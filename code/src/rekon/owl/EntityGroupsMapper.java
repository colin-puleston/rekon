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
class EntityGroupsMapper {

	private Node<OWLClass> owlThingAsEntityGroup;
	private Node<OWLClass> owlNothingAsEntityGroup;

	private ClassesMapper classesMapper = new ClassesMapper();
	private SupersMapper supersMapper = new SupersMapper();
	private SubsMapper subsMapper = new SubsMapper();
	private IndividualsMapper individualsMapper = new IndividualsMapper();

	private abstract class EntitiesMapper<E extends OWLEntity> {

		private Class<E> entityType;

		EntitiesMapper(Class<E> entityType) {

			this.entityType = entityType;
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

		abstract Node<E> createGroupNode(Set<E> entities);

		abstract NodeSet<E> createGroupsNode(Set<Node<E>> entityGroups);

		void addEntity(Set<E> entities, Name name) {

			entities.add(MappedNames.toMappedEntity(name, entityType));
		}

		private Set<E> toEntityGroup(Names nameGroup) {

			Set<E> entities = new HashSet<E>();

			for (Name n : nameGroup) {

				addEntity(entities, n);
			}

			return entities;
		}
	}

	private class ClassesMapper extends EntitiesMapper<OWLClass> {

		ClassesMapper() {

			super(OWLClass.class);
		}

		Node<OWLClass> toSingleEntityGroup(Names names, OWLClassExpression sourceExpr) {

			Set<OWLClass> entities = new HashSet<OWLClass>();

			for (Name n : names) {

				addEntity(entities, n);
			}

			return new OWLClassNode(entities);
		}

		Node<OWLClass> createGroupNode(Set<OWLClass> entities) {

			return new OWLClassNode(entities);
		}

		NodeSet<OWLClass> createGroupsNode(Set<Node<OWLClass>> entityGroups) {

			return new OWLClassNodeSet(entityGroups);
		}
	}

	private abstract class LinkedClassesMapper extends ClassesMapper {

		NodeSet<OWLClass> toEntityGroups(Collection<Names> nameGroups, boolean direct) {

			Set<Node<OWLClass>> entityGroups = toEntityGroupsSet(nameGroups);

			if (!direct || entityGroups.isEmpty()) {

				entityGroups.add(getDefaultEntityGroup());
			}

			return new OWLClassNodeSet(entityGroups);
		}

		abstract Node<OWLClass> getDefaultEntityGroup();
	}

	private class SupersMapper extends LinkedClassesMapper {

		Node<OWLClass> getDefaultEntityGroup() {

			return owlThingAsEntityGroup;
		}
	}

	private class SubsMapper extends LinkedClassesMapper {

		Node<OWLClass> getDefaultEntityGroup() {

			return owlNothingAsEntityGroup;
		}
	}

	private class IndividualsMapper extends EntitiesMapper<OWLNamedIndividual> {

		IndividualsMapper() {

			super(OWLNamedIndividual.class);
		}

		Node<OWLNamedIndividual> createGroupNode(Set<OWLNamedIndividual> entities) {

			return new OWLNamedIndividualNode(entities);
		}

		NodeSet<OWLNamedIndividual> createGroupsNode(Set<Node<OWLNamedIndividual>> entityGroups) {

			return new OWLNamedIndividualNodeSet(entityGroups);
		}
	}

	EntityGroupsMapper(OWLClass owlThing, OWLClass owlNothing) {

		owlThingAsEntityGroup = new OWLClassNode(owlThing);
		owlNothingAsEntityGroup = new OWLClassNode(owlNothing);
	}

	Node<OWLClass> mapEquivs(Names equivs, OWLClassExpression sourceExpr) {

		return classesMapper.toSingleEntityGroup(equivs, sourceExpr);
	}

	NodeSet<OWLClass> mapClasses(Collection<Names> equivGroups) {

		return classesMapper.toEntityGroups(equivGroups);
	}

	NodeSet<OWLClass> mapSupers(Collection<Names> equivGroups, boolean direct) {

		return supersMapper.toEntityGroups(equivGroups, direct);
	}

	NodeSet<OWLClass> mapSubs(Collection<Names> equivGroups, boolean direct) {

		return subsMapper.toEntityGroups(equivGroups, direct);
	}

	NodeSet<OWLNamedIndividual> mapIndividuals(Collection<Names> equivGroups) {

		return individualsMapper.toEntityGroups(equivGroups);
	}
}
