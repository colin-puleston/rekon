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
class MappedNames {

	static <E extends OWLEntity>E toMappedEntity(Name name, Class<E> entityType) {

		return entityType.cast(((MappedName)name).toMappedEntity());
	}

	static private interface MappedName {

		public OWLEntity toMappedEntity();
	}

	static private class MappedClassName extends ClassName implements MappedName {

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

	static private class MappedIndividualName extends IndividualName implements MappedName {

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

	static private class MappedObjectPropertyName extends ObjectPropertyName implements MappedName {

		private OWLObjectProperty entity;

		public OWLEntity toMappedEntity() {

			return entity;
		}

		public String getLabel() {

			return NameRenderer.SINGLETON.render(entity);
		}

		MappedObjectPropertyName(OWLObjectProperty entity) {

			this.entity = entity;
		}
	}

	static private class MappedDataPropertyName extends DataPropertyName implements MappedName {

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

	private ClassNames classes;
	private IndividualNames individuals;
	private ObjectPropertyNames objectProperties;
	private DataPropertyNames dataProperties;

	private abstract class TypeNames
								<E extends OWLEntity,
								AE extends AssertedEntity<E>,
								N extends Name> {

		private Map<E, N> names = new HashMap<E, N>();

		abstract class TypeNamesInitialiser {

			final Assertions assertions;

			TypeNamesInitialiser(Assertions assertions, Collection<AE> entityities) {

				this.assertions = assertions;

				for (AE ae : entityities) {

					addName(ae.getEntity());
				}

				for (AE ae : entityities) {

					configureName(ae);
				}
			}

			abstract N createName(E entity);

			N addName(E entity) {

				N n = createName(entity);

				names.put(entity, n);

				return n;
			}

			N configureName(AE entity) {

				N n = names.get(entity.getEntity());

				addAssertedEquivalents(entity, n);
				addAssertedSupers(entity, n);

				return n;
			}

			abstract void addAssertedSupers(AE entity, N name);

			private void addAssertedEquivalents(AE entity, N name) {

				for (E e : entity.getEquivs()) {

					name.addEquivalent(names.get(e));
				}
			}
		}

		N getName(E entity) {

			return names.get(entity);
		}

		Collection<N> getAllNames() {

			return names.values();
		}
	}

	private abstract class HierarchyNames
								<E extends OWLEntity,
								AE extends AssertedHierarchyEntity<E>,
								N extends Name>
								extends TypeNames<E, AE, N> {

		private N rootName;

		abstract class HierarchyNamesInitialiser extends TypeNamesInitialiser {

			HierarchyNamesInitialiser(Assertions assertions, Collection<AE> entityities) {

				super(assertions, entityities);
			}

			N addName(E entity) {

				N n = super.addName(entity);

				if (rootEntity(entity)) {

					rootName = n;
				}

				return n;
			}

			void addAssertedSupers(AE entity, N name) {

				for (E s : entity.getSupers()) {

					name.addSubsumer(getName(s));
				}

				if (name != rootName && name.rootName()) {

					name.addSubsumer(rootName);
				}
			}

			abstract boolean rootEntity(E entity);
		}

		N getRootName() {

			if (rootName == null) {

				throw new Error("Root-name has not been set!");
			}

			return rootName;
		}
	}

	private class ClassNames extends HierarchyNames<OWLClass, AssertedClass, ClassName> {

		private class Initialiser extends HierarchyNamesInitialiser {

			Initialiser(Assertions assertions) {

				super(assertions, assertions.getClasses());
			}

			boolean rootEntity(OWLClass entity) {

				return entity.equals(assertions.owlThing);
			}

			ClassName createName(OWLClass entity) {

				return new MappedClassName(entity);
			}
		}

		ClassNames(Assertions assertions) {

			new Initialiser(assertions);
		}
	}

	private class IndividualNames
					extends
						TypeNames<OWLNamedIndividual, AssertedIndividual, IndividualName> {

		private class Initialiser extends TypeNamesInitialiser {

			Initialiser(Assertions assertions) {

				super(assertions, assertions.getIndividuals());
			}

			IndividualName createName(OWLNamedIndividual entity) {

				return new MappedIndividualName(entity);
			}

			void addAssertedSupers(AssertedIndividual entity, IndividualName name) {

				for (OWLClass c : entity.getTypes()) {

					name.addSubsumer(classes.getName(c));
				}

				if (name.rootName()) {

					name.addSubsumer(classes.getRootName());
				}
			}
		}

		IndividualNames(Assertions assertions, ClassNames classes) {

			new Initialiser(assertions);
		}
	}

	private class ObjectPropertyNames
						extends
							HierarchyNames
								<OWLObjectProperty,
								AssertedObjectProperty,
								ObjectPropertyName> {

		private class Initialiser extends HierarchyNamesInitialiser {

			Initialiser(Assertions assertions) {

				super(assertions, assertions.getObjectProperties());
			}

			ObjectPropertyName configureName(AssertedObjectProperty entity) {

				ObjectPropertyName n = super.configureName(entity);

				addInverses(entity, n);
				addChains(entity, n);

				if (entity.transitive()) {

					n.setTransitive();
				}

				if (entity.symmetric()) {

					n.setSymmetric();
				}

				return n;
			}

			boolean rootEntity(OWLObjectProperty entity) {

				return entity.equals(assertions.owlTopObjectProperty);
			}

			ObjectPropertyName createName(OWLObjectProperty entity) {

				return new MappedObjectPropertyName(entity);
			}

			private void addInverses(AssertedObjectProperty entity, ObjectPropertyName name) {

				name.addInverses(toNames(entity.getInverses()));
			}

			private void addChains(AssertedObjectProperty entity, ObjectPropertyName name) {

				for (List<OWLObjectProperty> chain : entity.getChains()) {

					new PropertyChain(name, toNames(chain));
				}
			}

			private List<ObjectPropertyName> toNames(Collection<OWLObjectProperty> props) {

				List<ObjectPropertyName> names = new ArrayList<ObjectPropertyName>();

				for (OWLObjectProperty p : props) {

					names.add(getName(p));
				}

				return names;
			}
		}

		ObjectPropertyNames(Assertions assertions) {

			new Initialiser(assertions);
		}
	}

	private class DataPropertyNames
						extends
							HierarchyNames
								<OWLDataProperty,
								AssertedDataProperty,
								DataPropertyName> {

		private class Initialiser extends HierarchyNamesInitialiser {

			Initialiser(Assertions assertions) {

				super(assertions, assertions.getDataProperties());
			}

			boolean rootEntity(OWLDataProperty entity) {

				return entity.equals(assertions.owlTopDataProperty);
			}

			DataPropertyName createName(OWLDataProperty entity) {

				return new MappedDataPropertyName(entity);
			}
		}

		DataPropertyNames(Assertions assertions) {

			new Initialiser(assertions);
		}
	}

	MappedNames(Assertions assertions) {

		classes = new ClassNames(assertions);
		individuals = new IndividualNames(assertions, classes);
		objectProperties = new ObjectPropertyNames(assertions);
		dataProperties = new DataPropertyNames(assertions);
	}

	Collection<Name> getAllNames() {

		List<Name> names = new ArrayList<Name>();

		names.addAll(classes.getAllNames());
		names.addAll(individuals.getAllNames());
		names.addAll(objectProperties.getAllNames());
		names.addAll(dataProperties.getAllNames());

		return names;
	}

	Collection<NodeName> getAllNodeNames() {

		List<NodeName> names = new ArrayList<NodeName>();

		names.addAll(classes.getAllNames());
		names.addAll(individuals.getAllNames());

		return names;
	}

	Collection<ClassName> getClassNames() {

		return classes.getAllNames();
	}

	Collection<IndividualName> getIndividualNames() {

		return individuals.getAllNames();
	}

	Collection<ObjectPropertyName> getObjectPropertyNames() {

		return objectProperties.getAllNames();
	}

	Collection<DataPropertyName> getDataPropertyNames() {

		return dataProperties.getAllNames();
	}

	ClassName getRootClassName() {

		return classes.getRootName();
	}

	ClassName get(OWLClass entity) {

		return classes.getName(entity);
	}

	IndividualName get(OWLNamedIndividual entity) {

		return individuals.getName(entity);
	}

	ObjectPropertyName get(OWLObjectProperty entity) {

		return objectProperties.getName(entity);
	}

	DataPropertyName get(OWLDataProperty entity) {

		return dataProperties.getName(entity);
	}
}
