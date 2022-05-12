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

	static private class MappedClassName
							extends ClassName
							implements MappedName {

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

	static private class MappedIndividualName
							extends IndividualName
							implements MappedName {

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

	static private class MappedObjectPropertyName
							extends ObjectPropertyName
							implements MappedName {

		private OWLObjectProperty entity;

		public OWLEntity toMappedEntity() {

			return entity;
		}

		public String getLabel() {

			return NameRenderer.SINGLETON.render(entity);
		}

		MappedObjectPropertyName(OWLObjectProperty entity, boolean transitive) {

			super(transitive);

			this.entity = entity;
		}
	}

	static private class MappedDataPropertyName
							extends DataPropertyName
							implements MappedName {

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

	private abstract class TypeNames<E extends OWLEntity, N extends Name> {

		private Map<E, N> names = new HashMap<E, N>();

		abstract class TypeNamesInitialiser {

			final Assertions assertions;

			TypeNamesInitialiser(Assertions assertions) {

				this.assertions = assertions;

				addNames();
				addAssertedLinks();
			}

			abstract Collection<E> getAssertedEntities();

			abstract Collection<E> getAssertedEquivalents(E entity);

			abstract N createName(E entity);

			void onNameAdded(N name, E entity) {
			}

			abstract void addAssertedSupers(N name, E entity);

			private void addNames() {

				for (E e : getAssertedEntities()) {

					N n = createName(e);

					names.put(e, n);
					onNameAdded(n, e);
				}
			}

			private void addAssertedLinks() {

				for (E e : getAssertedEntities()) {

					N n = names.get(e);

					addAssertedEquivalents(n, e);
					addAssertedSupers(n, e);
				}
			}

			private void addAssertedEquivalents(N n, E e) {

				for (E eq : getAssertedEquivalents(e)) {

					n.addEquivalent(names.get(eq));
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

	private abstract class HierarchyNames<E extends OWLEntity, N extends Name> extends TypeNames<E, N> {

		private N rootName;

		abstract class HierarchyNamesInitialiser extends TypeNamesInitialiser {

			HierarchyNamesInitialiser(Assertions assertions) {

				super(assertions);
			}

			void onNameAdded(N name, E entity) {

				if (rootEntity(entity)) {

					rootName = name;
				}
			}

			void addAssertedSupers(N name, E entity) {

				Collection<E> sups = getAssertedSupers(entity);

				for (E s : sups) {

					name.addSubsumer(getName(s));
				}

				if (name != rootName && name.rootName()) {

					name.addSubsumer(rootName);
				}
			}

			abstract Collection<E> getAssertedSupers(E entity);

			abstract boolean rootEntity(E entity);
		}

		N getRootName() {

			if (rootName == null) {

				throw new Error("Root-name has not been set!");
			}

			return rootName;
		}
	}

	private class ClassNames extends HierarchyNames<OWLClass, ClassName> {

		private class Initialiser extends HierarchyNamesInitialiser {

			Initialiser(Assertions assertions) {

				super(assertions);
			}

			Collection<OWLClass> getAssertedEntities() {

				return assertions.getAllClasses();
			}

			Collection<OWLClass> getAssertedEquivalents(OWLClass entity) {

				return assertions.getEquivalentClasses(entity);
			}

			Collection<OWLClass> getAssertedSupers(OWLClass entity) {

				return assertions.getSuperClasses(entity);
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

	private class IndividualNames extends TypeNames<OWLNamedIndividual, IndividualName> {

		private class Initialiser extends TypeNamesInitialiser {

			Initialiser(Assertions assertions) {

				super(assertions);
			}

			Collection<OWLNamedIndividual> getAssertedEntities() {

				return assertions.getAllIndividuals();
			}

			Collection<OWLNamedIndividual> getAssertedEquivalents(OWLNamedIndividual entity) {

				return assertions.getSameIndividuals(entity);
			}

			IndividualName createName(OWLNamedIndividual entity) {

				return new MappedIndividualName(entity);
			}

			void addAssertedSupers(IndividualName name, OWLNamedIndividual entity) {

				for (OWLClass c : assertions.getTypes(entity)) {

					name.addSubsumer(classes.getName(c));
				}
			}
		}

		IndividualNames(Assertions assertions, ClassNames classes) {

			new Initialiser(assertions);
		}
	}

	private class ObjectPropertyNames extends HierarchyNames<OWLObjectProperty, ObjectPropertyName> {

		private class Initialiser extends HierarchyNamesInitialiser {

			Initialiser(Assertions assertions) {

				super(assertions);

				addChains();
			}

			Collection<OWLObjectProperty> getAssertedEntities() {

				return assertions.getAllObjectProperties();
			}

			Collection<OWLObjectProperty> getAssertedEquivalents(OWLObjectProperty entity) {

				return assertions.getEquivalentProperties(entity);
			}

			Collection<OWLObjectProperty> getAssertedSupers(OWLObjectProperty entity) {

				return assertions.getSuperProperties(entity);
			}

			boolean rootEntity(OWLObjectProperty entity) {

				return entity.equals(assertions.owlTopObjectProperty);
			}

			ObjectPropertyName createName(OWLObjectProperty entity) {

				return new MappedObjectPropertyName(entity, assertions.isTransitive(entity));
			}

			private void addChains() {

				for (OWLSubPropertyChainOfAxiom ax : getChainAxioms()) {

					checkAddChain(ax);
				}
			}

			private void checkAddChain(OWLSubPropertyChainOfAxiom ax) {

				ObjectPropertyName sup = toName(ax.getSuperProperty());

				if (sup != null) {

					List<ObjectPropertyName> subs = toNames(ax.getPropertyChain());

					if (subs != null) {

						new PropertyChain(sup, subs);
					}
				}
			}

			private List<ObjectPropertyName> toNames(List<OWLObjectPropertyExpression> exprs) {

				List<ObjectPropertyName> names = new ArrayList<ObjectPropertyName>();

				for (OWLObjectPropertyExpression e : exprs) {

					ObjectPropertyName n = toName(e);

					if (n == null) {

						return null;
					}

					names.add(n);
				}

				return names;
			}

			private ObjectPropertyName toName(OWLObjectPropertyExpression e) {

				return e instanceof OWLObjectProperty ? getName((OWLObjectProperty)e) : null;
			}

			private Collection<OWLSubPropertyChainOfAxiom> getChainAxioms() {

				return assertions.getAxioms(AxiomType.SUB_PROPERTY_CHAIN_OF);
			}
		}

		ObjectPropertyNames(Assertions assertions) {

			new Initialiser(assertions);
		}
	}

	private class DataPropertyNames extends HierarchyNames<OWLDataProperty, DataPropertyName> {

		private class Initialiser extends HierarchyNamesInitialiser {

			Initialiser(Assertions assertions) {

				super(assertions);
			}

			Collection<OWLDataProperty> getAssertedEntities() {

				return assertions.getAllDataProperties();
			}

			Collection<OWLDataProperty> getAssertedEquivalents(OWLDataProperty entity) {

				return assertions.getEquivalentProperties(entity);
			}

			Collection<OWLDataProperty> getAssertedSupers(OWLDataProperty entity) {

				return assertions.getSuperProperties(entity);
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
