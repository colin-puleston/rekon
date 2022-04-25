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

/**
 * @author Colin Puleston
 */
class MappedNames {

	static private abstract class TypeNames<E extends OWLEntity, N extends Name> {

		final Assertions assertions;

		private Map<E, N> names = new HashMap<E, N>();

		TypeNames(Assertions assertions) {

			this.assertions = assertions;
		}

		void initialise() {

			addNames();
			addAssertedLinks();
		}

		N getName(E entity) {

			return names.get(entity);
		}

		Collection<N> getAllNames() {

			return names.values();
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

				n.getClassifier().addAssertedEquivalent(names.get(eq));
			}
		}
	}

	static private abstract class HierarchyNames<E extends OWLEntity, N extends Name> extends TypeNames<E, N> {

		private N rootName;

		HierarchyNames(Assertions assertions) {

			super(assertions);
		}

		N getRootName() {

			if (rootName == null) {

				throw new Error("Root-name has not been set!");
			}

			return rootName;
		}

		void onNameAdded(N name, E entity) {

			if (rootEntity(entity)) {

				rootName = name;
			}
		}

		void addAssertedSupers(N name, E entity) {

			Collection<E> sups = getAssertedSupers(entity);
			NameClassifier classifier = name.getClassifier();

			for (E s : sups) {

				classifier.addAssertedSubsumer(getName(s));
			}

			if (name != rootName && classifier.noSubsumers()) {

				classifier.addAssertedSubsumer(rootName);
			}
		}

		abstract Collection<E> getAssertedSupers(E entity);

		abstract boolean rootEntity(E entity);
	}

	static private class ClassNames extends HierarchyNames<OWLClass, ClassName> {

		ClassNames(Assertions assertions) {

			super(assertions);

			initialise();
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

	static private class IndividualNames extends TypeNames<OWLNamedIndividual, IndividualName> {

		private ClassNames classes;

		IndividualNames(Assertions assertions, ClassNames classes) {

			super(assertions);

			this.classes = classes;

			initialise();
		}

		Collection<OWLNamedIndividual> getAssertedEntities() {

			return assertions.getAllIndividuals();
		}

		Collection<OWLNamedIndividual> getAssertedEquivalents(OWLNamedIndividual entity) {

			return assertions.getSameIndividuals(entity);
		}

		IndividualName createName(OWLNamedIndividual entity) {

			return new IndividualName(entity);
		}

		void addAssertedSupers(IndividualName name, OWLNamedIndividual entity) {

			for (OWLClass c : assertions.getTypes(entity)) {

				name.getClassifier().addAssertedSubsumer(classes.getName(c));
			}
		}
	}

	static private class ObjectPropertyNames extends HierarchyNames<OWLObjectProperty, ObjectPropertyName> {

		ObjectPropertyNames(Assertions assertions) {

			super(assertions);

			initialise();

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

			return new ObjectPropertyName(entity, assertions.isTransitive(entity));
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

	static private class DataPropertyNames extends HierarchyNames<OWLDataProperty, DataPropertyName> {

		DataPropertyNames(Assertions assertions) {

			super(assertions);

			initialise();
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

			return new DataPropertyName(entity);
		}
	}

	private ClassNames classes;
	private IndividualNames individuals;
	private ObjectPropertyNames objectProperties;
	private DataPropertyNames dataProperties;

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

	Collection<ClassName> getAllClassNames() {

		return classes.getAllNames();
	}

	Collection<IndividualName> getAllIndividualNames() {

		return individuals.getAllNames();
	}

	Collection<PropertyName> getAllPropertyNames() {

		List<PropertyName> names = new ArrayList<PropertyName>();

		names.addAll(objectProperties.getAllNames());
		names.addAll(dataProperties.getAllNames());

		return names;
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
