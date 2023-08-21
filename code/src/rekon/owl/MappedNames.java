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

import gnu.trove.map.hash.*;

import org.semanticweb.owlapi.model.*;

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class MappedNames extends OntologyNames {

	static <E extends OWLEntity>E toMappedEntity(Name name, Class<E> entityType) {

		return entityType.cast(((MappedName)name).toMappedEntity());
	}

	private ClassNodes classes;
	private IndividualNodes individuals;
	private NodeProperties nodeProperties;
	private DataProperties dataProperties;

	private interface MappedName {

		public OWLEntity toMappedEntity();
	}

	private class MappedClassNode extends ClassNode implements MappedName {

		private OWLClass entity;

		public OWLEntity toMappedEntity() {

			return entity;
		}

		public String getLabel() {

			return NameRenderer.SINGLETON.render(entity);
		}

		MappedClassNode(OWLClass entity) {

			this.entity = entity;
		}
	}

	private class MappedIndividualNode extends IndividualNode implements MappedName {

		private OWLNamedIndividual entity;

		public OWLEntity toMappedEntity() {

			return entity;
		}

		public String getLabel() {

			return NameRenderer.SINGLETON.render(entity);
		}

		MappedIndividualNode(OWLNamedIndividual entity) {

			this.entity = entity;
		}
	}

	private class MappedNodeProperty extends NodeProperty implements MappedName {

		private OWLObjectProperty entity;

		public OWLEntity toMappedEntity() {

			return entity;
		}

		public String getLabel() {

			return NameRenderer.SINGLETON.render(entity);
		}

		MappedNodeProperty(OWLObjectProperty entity) {

			this.entity = entity;
		}
	}

	private class MappedDataProperty extends DataProperty implements MappedName {

		private OWLDataProperty entity;

		public OWLEntity toMappedEntity() {

			return entity;
		}

		public String getLabel() {

			return NameRenderer.SINGLETON.render(entity);
		}

		MappedDataProperty(OWLDataProperty entity) {

			this.entity = entity;
		}
	}

	private abstract class TypeNames
								<E extends OWLEntity,
								AE extends AssertedEntity<E>,
								N extends Name> {

		private Map<E, N> names = new THashMap<E, N>();

		void initialise(Collection<AE> entities) {

			for (AE ae : entities) {

				addName(ae.getEntity());
			}

			for (AE ae : entities) {

				configureName(ae);
			}
		}

		N addName(E entity) {

			N n = createName(entity);

			names.put(entity, n);

			return n;
		}

		N resolveName(E entity) {

			N n = names.get(entity);

			return n != null ? n : addName(entity);
		}

		N configureName(AE entity) {

			N n = names.get(entity.getEntity());

			addAssertedEquivalents(entity, n);
			addAssertedSupers(entity, n);

			return n;
		}

		abstract N createName(E entity);

		abstract void addAssertedSupers(AE entity, N name);

		Collection<N> getAllNames() {

			return names.values();
		}

		private void addAssertedEquivalents(AE entity, N name) {

			for (E e : entity.getEquivs()) {

				name.addEquivalent(resolveName(e));
			}
		}
	}

	private abstract class HierarchyNames
								<E extends OWLEntity,
								AE extends AssertedHierarchyEntity<E>,
								N extends Name>
								extends TypeNames<E, AE, N> {

		final N rootName = createRootName();

		private E rootEntity;

		HierarchyNames(E rootEntity, Collection<AE> entities) {

			this.rootEntity = rootEntity;

			initialise(entities);
		}

		void addAssertedSupers(AE entity, N name) {

			for (E s : entity.getSupers()) {

				name.addSubsumer(resolveName(s));
			}
		}

		N resolveName(E entity) {

			return entity.equals(rootEntity) ? rootName : super.resolveName(entity);
		}

		abstract N createRootName();
	}

	private class ClassNodes extends HierarchyNames<OWLClass, AssertedClass, ClassNode> {

		ClassNodes(Assertions assertions) {

			super(assertions.owlThing, assertions.getClasses());
		}

		ClassNode createName(OWLClass entity) {

			return new MappedClassNode(entity);
		}

		ClassNode createRootName() {

			return createRootClassNode(getAllNames());
		}
	}

	private class IndividualNodes
					extends
						TypeNames<OWLNamedIndividual, AssertedIndividual, IndividualNode> {

		IndividualNodes(Assertions assertions) {

			initialise(assertions.getIndividuals());
		}

		IndividualNode createName(OWLNamedIndividual entity) {

			return new MappedIndividualNode(entity);
		}

		void addAssertedSupers(AssertedIndividual entity, IndividualNode node) {

			for (OWLClass c : entity.getTypes()) {

				node.addSubsumer(classes.resolveName(c));
			}
		}
	}

	private class NodeProperties
						extends
							HierarchyNames
								<OWLObjectProperty,
								AssertedObjectProperty,
								NodeProperty> {

		NodeProperties(Assertions assertions) {

			super(assertions.owlTopObjectProperty, assertions.getObjectProperties());
		}

		NodeProperty configureName(AssertedObjectProperty entity) {

			NodeProperty n = super.configureName(entity);

			addInverses(entity, n);
			addChains(entity, n);

			if (entity.symmetric()) {

				n.setSymmetric();
			}

			return n;
		}

		NodeProperty createName(OWLObjectProperty entity) {

			return new MappedNodeProperty(entity);
		}

		NodeProperty createRootName() {

			return createRootNodeProperty(getAllNames());
		}

		private void addInverses(AssertedObjectProperty entity, NodeProperty prop) {

			prop.addInverses(toNodeProperties(entity.getInverses()));
		}

		private void addChains(AssertedObjectProperty entity, NodeProperty prop) {

			if (entity.transitive()) {

				new PropertyChain(prop);
			}

			for (List<OWLObjectProperty> chain : entity.getChains()) {

				new PropertyChain(prop, toNodeProperties(chain));
			}
		}

		private List<NodeProperty> toNodeProperties(Collection<OWLObjectProperty> objectProps) {

			List<NodeProperty> props = new ArrayList<NodeProperty>();

			for (OWLObjectProperty p : objectProps) {

				props.add(resolveName(p));
			}

			return props;
		}
	}

	private class DataProperties
						extends
							HierarchyNames
								<OWLDataProperty,
								AssertedDataProperty,
								DataProperty> {

		DataProperties(Assertions assertions) {

			super(assertions.owlTopDataProperty, assertions.getDataProperties());
		}

		DataProperty createName(OWLDataProperty entity) {

			return new MappedDataProperty(entity);
		}

		DataProperty createRootName() {

			return createRootDataProperty(getAllNames());
		}
	}

	protected Collection<ClassNode> getClassNodes() {

		return classes.getAllNames();
	}

	protected Collection<IndividualNode> getIndividualNodes() {

		return individuals.getAllNames();
	}

	protected Collection<NodeProperty> getNodeProperties() {

		return nodeProperties.getAllNames();
	}

	protected Collection<DataProperty> getDataProperties() {

		return dataProperties.getAllNames();
	}

	MappedNames(Assertions assertions) {

		classes = new ClassNodes(assertions);
		individuals = new IndividualNodes(assertions);
		nodeProperties = new NodeProperties(assertions);
		dataProperties = new DataProperties(assertions);
	}

	ClassNode get(OWLClass entity) {

		return classes.resolveName(entity);
	}

	IndividualNode get(OWLNamedIndividual entity) {

		return individuals.resolveName(entity);
	}

	NodeProperty get(OWLObjectProperty entity) {

		return nodeProperties.resolveName(entity);
	}

	DataProperty get(OWLDataProperty entity) {

		return dataProperties.resolveName(entity);
	}

	ClassNode getRootClassNode() {

		return classes.rootName;
	}
}
