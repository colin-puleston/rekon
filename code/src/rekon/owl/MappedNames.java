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
import rekon.build.*;

/**
 * @author Colin Puleston
 */
class MappedNames extends OntologyNames {

	static private interface MappedName {

		public OWLEntity toMappedEntity();
	}

	static private class MappedClassNode extends ClassNode implements MappedName {

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

	static private class MappedIndividualNode extends IndividualNode implements MappedName {

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

	static private class MappedNodeProperty extends NodeProperty implements MappedName {

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

	static private class MappedDataProperty extends DataProperty implements MappedName {

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

	static <E extends OWLEntity>E toMappedEntity(Name name, Class<E> entityType) {

		return entityType.cast(((MappedName)name).toMappedEntity());
	}

	private ClassNodes classes;
	private IndividualNodes individuals;
	private NodeProperties nodeProperties;
	private DataProperties dataProperties;

	private abstract class TypeNames<E extends OWLEntity, N extends Name> {

		private Map<E, N> names = new HashMap<E, N>();

		void initialise(OWLOntologyManager manager) {

			for (OWLOntology o : manager.getOntologies()) {

				for (E e : getEntitiesInSignature(o)) {

					if (mappingSource(e)) {

						addName(e);
					}
				}
			}
		}

		N resolveName(E entity) {

			N n = names.get(entity);

			return n != null ? n : addName(entity);
		}

		boolean mappingSource(E entity) {

			return true;
		}

		N addName(E entity) {

			N n = createName(entity);

			names.put(entity, n);

			return n;
		}

		abstract N createName(E entity);

		abstract Iterable<E> getEntitiesInSignature(OWLOntology o);

		Iterable<N> getAllNames() {

			return names.values();
		}
	}

	private abstract class HierarchyNames
								<E extends OWLEntity, N extends Name>
								extends TypeNames<E, N> {

		final N rootName = createRootName();

		private E topEntity;
		private E bottomEntity;

		HierarchyNames(OWLOntologyManager manager) {

			OWLDataFactory factory = manager.getOWLDataFactory();

			topEntity = getTopEntity(factory);
			bottomEntity = getBottomEntity(factory);

			initialise(manager);
		}

		N resolveName(E entity) {

			if (entity.equals(bottomEntity)) {

				throw new Error("Unexpected bottom-entity: " + entity);
			}

			return entity.equals(topEntity) ? rootName : super.resolveName(entity);
		}

		boolean mappingSource(E entity) {

			return !entity.equals(topEntity) && !entity.equals(bottomEntity);
		}

		abstract N createRootName();

		abstract E getTopEntity(OWLDataFactory factory);

		abstract E getBottomEntity(OWLDataFactory factory);
	}

	private class ClassNodes extends HierarchyNames<OWLClass, ClassNode> {

		ClassNodes(OWLOntologyManager manager) {

			super(manager);

			addName(getNoValueClass(manager.getOWLDataFactory()));
		}

		ClassNode createName(OWLClass entity) {

			return new MappedClassNode(entity);
		}

		ClassNode createRootName() {

			return createRootClassNode(getAllNames());
		}

		Iterable<OWLClass> getEntitiesInSignature(OWLOntology o) {

			return o.getClassesInSignature();
		}

		OWLClass getTopEntity(OWLDataFactory factory) {

			return factory.getOWLThing();
		}

		OWLClass getBottomEntity(OWLDataFactory factory) {

			return factory.getOWLNothing();
		}

		private OWLClass getNoValueClass(OWLDataFactory factory) {

			return OwlRestrictionResolver.getNoValueClass(factory);
		}
	}

	private class IndividualNodes extends TypeNames<OWLNamedIndividual, IndividualNode> {

		IndividualNodes(OWLOntologyManager manager) {

			initialise(manager);
		}

		IndividualNode createName(OWLNamedIndividual entity) {

			return new MappedIndividualNode(entity);
		}

		Iterable<OWLNamedIndividual> getEntitiesInSignature(OWLOntology o) {

			return o.getIndividualsInSignature();
		}
	}

	private class NodeProperties extends HierarchyNames<OWLObjectProperty, NodeProperty> {

		NodeProperties(OWLOntologyManager manager) {

			super(manager);
		}

		NodeProperty createName(OWLObjectProperty entity) {

			return new MappedNodeProperty(entity);
		}

		NodeProperty createRootName() {

			return createRootNodeProperty(getAllNames());
		}

		Iterable<OWLObjectProperty> getEntitiesInSignature(OWLOntology o) {

			return o.getObjectPropertiesInSignature();
		}

		OWLObjectProperty getTopEntity(OWLDataFactory factory) {

			return factory.getOWLTopObjectProperty();
		}

		OWLObjectProperty getBottomEntity(OWLDataFactory factory) {

			return factory.getOWLBottomObjectProperty();
		}
	}

	private class DataProperties extends HierarchyNames<OWLDataProperty, DataProperty> {

		DataProperties(OWLOntologyManager manager) {

			super(manager);
		}

		DataProperty createName(OWLDataProperty entity) {

			return new MappedDataProperty(entity);
		}

		DataProperty createRootName() {

			return createRootDataProperty(getAllNames());
		}

		Iterable<OWLDataProperty> getEntitiesInSignature(OWLOntology o) {

			return o.getDataPropertiesInSignature();
		}

		OWLDataProperty getTopEntity(OWLDataFactory factory) {

			return factory.getOWLTopDataProperty();
		}

		OWLDataProperty getBottomEntity(OWLDataFactory factory) {

			return factory.getOWLBottomDataProperty();
		}
	}

	public ClassNode getRootClassNode() {

		return classes.rootName;
	}

	public Iterable<ClassNode> getClassNodes() {

		return classes.getAllNames();
	}

	public Iterable<IndividualNode> getIndividualNodes() {

		return individuals.getAllNames();
	}

	public Iterable<NodeProperty> getNodeProperties() {

		return nodeProperties.getAllNames();
	}

	public Iterable<DataProperty> getDataProperties() {

		return dataProperties.getAllNames();
	}

	MappedNames(OWLOntologyManager manager) {

		classes = new ClassNodes(manager);
		individuals = new IndividualNodes(manager);
		nodeProperties = new NodeProperties(manager);
		dataProperties = new DataProperties(manager);
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
}
