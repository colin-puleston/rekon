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

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class MappedNames extends OntologyNames {

	static <E extends OWLEntity>E toMappedEntity(Name name, Class<E> entityType) {

		return entityType.cast(((MappedName)name).toMappedEntity());
	}

	private ClassNames classes;
	private IndividualNames individuals;
	private NodePropertyNames nodeProperties;
	private DataPropertyNames dataProperties;

	private interface MappedName {

		public OWLEntity toMappedEntity();
	}

	private class MappedClassName extends ClassName implements MappedName {

		private OWLClass entity;

		public OWLEntity toMappedEntity() {

			return entity;
		}

		public String getLabel() {

			return NameRenderer.SINGLETON.render(entity);
		}

		MappedClassName(OWLClass entity) {

			this.entity = entity;
		}
	}

	private class MappedIndividualName extends IndividualName implements MappedName {

		private OWLNamedIndividual entity;

		public OWLEntity toMappedEntity() {

			return entity;
		}

		public String getLabel() {

			return NameRenderer.SINGLETON.render(entity);
		}

		MappedIndividualName(OWLNamedIndividual entity) {

			this.entity = entity;
		}
	}

	private class MappedNodePropertyName extends NodePropertyName implements MappedName {

		private OWLObjectProperty entity;

		public OWLEntity toMappedEntity() {

			return entity;
		}

		public String getLabel() {

			return NameRenderer.SINGLETON.render(entity);
		}

		MappedNodePropertyName(OWLObjectProperty entity) {

			this.entity = entity;
		}
	}

	private class MappedDataPropertyName extends DataPropertyName implements MappedName {

		private OWLDataProperty entity;

		public OWLEntity toMappedEntity() {

			return entity;
		}

		public String getLabel() {

			return NameRenderer.SINGLETON.render(entity);
		}

		MappedDataPropertyName(OWLDataProperty entity) {

			this.entity = entity;
		}
	}

	private abstract class TypeNames
								<E extends OWLEntity,
								AE extends AssertedEntity<E>,
								N extends Name> {

		private Map<E, N> names = new HashMap<E, N>();

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

	private class ClassNames extends HierarchyNames<OWLClass, AssertedClass, ClassName> {

		ClassNames(Assertions assertions) {

			super(assertions.owlThing, assertions.getClasses());
		}

		ClassName createName(OWLClass entity) {

			return new MappedClassName(entity);
		}

		ClassName createRootName() {

			return createRootClassName(getAllNames());
		}
	}

	private class IndividualNames
					extends
						TypeNames<OWLNamedIndividual, AssertedIndividual, IndividualName> {

		IndividualNames(Assertions assertions) {

			initialise(assertions.getIndividuals());
		}

		IndividualName createName(OWLNamedIndividual entity) {

			return new MappedIndividualName(entity);
		}

		void addAssertedSupers(AssertedIndividual entity, IndividualName name) {

			for (OWLClass c : entity.getTypes()) {

				name.addSubsumer(classes.resolveName(c));
			}
		}
	}

	private class NodePropertyNames
						extends
							HierarchyNames
								<OWLObjectProperty,
								AssertedObjectProperty,
								NodePropertyName> {

		NodePropertyNames(Assertions assertions) {

			super(assertions.owlTopObjectProperty, assertions.getObjectProperties());
		}

		NodePropertyName configureName(AssertedObjectProperty entity) {

			NodePropertyName n = super.configureName(entity);

			addInverses(entity, n);
			addChains(entity, n);

			if (entity.symmetric()) {

				n.setSymmetric();
			}

			return n;
		}

		NodePropertyName createName(OWLObjectProperty entity) {

			return new MappedNodePropertyName(entity);
		}

		NodePropertyName createRootName() {

			return createRootNodePropertyName(getAllNames());
		}

		private void addInverses(AssertedObjectProperty entity, NodePropertyName name) {

			name.addInverses(toNames(entity.getInverses()));
		}

		private void addChains(AssertedObjectProperty entity, NodePropertyName name) {

			if (entity.transitive()) {

				new PropertyChain(name);
			}

			for (List<OWLObjectProperty> chain : entity.getChains()) {

				new PropertyChain(name, toNames(chain));
			}
		}

		private List<NodePropertyName> toNames(Collection<OWLObjectProperty> props) {

			List<NodePropertyName> names = new ArrayList<NodePropertyName>();

			for (OWLObjectProperty p : props) {

				names.add(resolveName(p));
			}

			return names;
		}
	}

	private class DataPropertyNames
						extends
							HierarchyNames
								<OWLDataProperty,
								AssertedDataProperty,
								DataPropertyName> {

		DataPropertyNames(Assertions assertions) {

			super(assertions.owlTopDataProperty, assertions.getDataProperties());
		}

		DataPropertyName createName(OWLDataProperty entity) {

			return new MappedDataPropertyName(entity);
		}

		DataPropertyName createRootName() {

			return createRootDataPropertyName(getAllNames());
		}
	}

	protected Collection<ClassName> getClassNames() {

		return classes.getAllNames();
	}

	protected Collection<IndividualName> getIndividualNames() {

		return individuals.getAllNames();
	}

	protected Collection<NodePropertyName> getNodePropertyNames() {

		return nodeProperties.getAllNames();
	}

	protected Collection<DataPropertyName> getDataPropertyNames() {

		return dataProperties.getAllNames();
	}

	MappedNames(Assertions assertions) {

		classes = new ClassNames(assertions);
		individuals = new IndividualNames(assertions);
		nodeProperties = new NodePropertyNames(assertions);
		dataProperties = new DataPropertyNames(assertions);
	}

	ClassName get(OWLClass entity) {

		return classes.resolveName(entity);
	}

	IndividualName get(OWLNamedIndividual entity) {

		return individuals.resolveName(entity);
	}

	NodePropertyName get(OWLObjectProperty entity) {

		return nodeProperties.resolveName(entity);
	}

	DataPropertyName get(OWLDataProperty entity) {

		return dataProperties.resolveName(entity);
	}

	ClassName getRootClassName() {

		return classes.rootName;
	}
}
