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
import org.semanticweb.owlapi.model.parameters.*;

import rekon.build.*;

/**
 * @author Colin Puleston
 */
class OwlInputAssertions implements InputAssertions {

	private Map<OWLClass, InputClass> classes = new HashMap<OWLClass, InputClass>();
	private Map<OWLNamedIndividual, InputIndividual> individuals = new HashMap<OWLNamedIndividual, InputIndividual>();
	private Map<OWLObjectProperty, InputObjectProperty> objectProperties = new HashMap<OWLObjectProperty, InputObjectProperty>();
	private Map<OWLDataProperty, InputDataProperty> dataProperties = new HashMap<OWLDataProperty, InputDataProperty>();

	private Set<InputEquivalence> equivalenceGCIs = new HashSet<InputEquivalence>();
	private Set<InputSubSuper> subSuperGCIs = new HashSet<InputSubSuper>();

	private class AxiomProcessingInitialiser {

		private OWLClass owlThing;
		private OWLObjectProperty owlTopObjectProperty;
		private OWLDataProperty owlTopDataProperty;

		private OwlInputObjects inputObjects;

		private List<ProcessorGroup> processorGroups = new ArrayList<ProcessorGroup>();
		private Set<AxiomType<?>> outOfScopeAxiomTypes = new HashSet<AxiomType<?>>();

		private abstract class ProcessorGroup {

			private List<Processor<?>> processors = new ArrayList<Processor<?>>();

			abstract class Processor<A extends OWLAxiom> {

				Processor() {

					processors.add(this);
				}

				boolean checkProcess(OWLAxiom axiom) {

					Class<? extends OWLAxiom> type = axiom.getClass();
					Class<A> pType = getProcessAxiomType();

					return pType.isAssignableFrom(type) && checkTypeProcess(pType.cast(axiom));
				}

				abstract Class<A> getProcessAxiomType();

				abstract boolean checkTypeProcess(A axiom);
			}

			ProcessorGroup() {

				processorGroups.add(this);
			}

			boolean checkProcess(OWLAxiom axiom) {

				for (Processor<?> ap : processors) {

					if (ap.checkProcess(axiom)) {

						return true;
					}
				}

				return false;
			}
		}

		private class GCIExtractors extends ProcessorGroup {

			private class EquivsExtractor extends Processor<OWLEquivalentClassesAxiom> {

				Class<OWLEquivalentClassesAxiom> getProcessAxiomType() {

					return OWLEquivalentClassesAxiom.class;
				}

				boolean checkTypeProcess(OWLEquivalentClassesAxiom axiom) {

					if (!anyOfType(axiom.getClassExpressions(), OWLClass.class)) {

						equivalenceGCIs.add(inputObjects.createEquivalence(axiom));

						return true;
					}

					return false;
				}
			}

			private class SuperExtractor extends Processor<OWLSubClassOfAxiom> {

				Class<OWLSubClassOfAxiom> getProcessAxiomType() {

					return OWLSubClassOfAxiom.class;
				}

				boolean checkTypeProcess(OWLSubClassOfAxiom axiom) {

					if (axiom.getSubClass() instanceof OWLClass) {

						return false;
					}

					subSuperGCIs.add(inputObjects.createSubSuper(axiom));

					return true;
				}
			}

			GCIExtractors() {

				new EquivsExtractor();
				new SuperExtractor();
			}
		}

		private abstract class EntityAspectExtractors
									<E extends OWLEntity,
									N extends InputName<?>>
									extends ProcessorGroup {

			abstract class AspectExtractor<A extends OWLAxiom> extends Processor<A> {

				boolean checkTypeProcess(A axiom) {

					E e = getEntityOrNull(axiom);

					if (e != null && !e.equals(getRootEntityOrNull())) {

						process(axiom, ensureInputName(e));

						return true;
					}

					return false;
				}

				abstract E getEntityOrNull(A axiom);

				abstract void process(A axiom, N n);
			}

			private class DeclarationExtractor extends AspectExtractor<OWLDeclarationAxiom> {

				Class<OWLDeclarationAxiom> getProcessAxiomType() {

					return OWLDeclarationAxiom.class;
				}

				E getEntityOrNull(OWLDeclarationAxiom axiom) {

					OWLEntity e = axiom.getEntity();
					Class<E> pType = getProcessEntityType();

					return pType.isAssignableFrom(e.getClass()) ? pType.cast(e) : null;
				}

				void process(OWLDeclarationAxiom axiom, N n) {
				}
			}

			EntityAspectExtractors() {

				new DeclarationExtractor();
			}

			abstract Class<E> getProcessEntityType();

			abstract E getRootEntityOrNull();

			abstract Map<E, N> getEntityToNameMap();

			abstract N createInputName(E entity);

			private N ensureInputName(E entity) {

				Map<E, N> map = getEntityToNameMap();
				N n = map.get(entity);

				if (n == null) {

					n = createInputName(entity);

					map.put(entity, n);
				}

				return n;
			}
		}

		private class ClassAspectExtractors
							extends
								EntityAspectExtractors<OWLClass, InputClass> {

			private class EquivsExtractor extends AspectExtractor<OWLEquivalentClassesAxiom> {

				Class<OWLEquivalentClassesAxiom> getProcessAxiomType() {

					return OWLEquivalentClassesAxiom.class;
				}

				OWLClass getEntityOrNull(OWLEquivalentClassesAxiom axiom) {

					return getAnyOfTypeOrNull(axiom.getClassExpressions(), OWLClass.class);
				}

				void process(OWLEquivalentClassesAxiom axiom, InputClass n) {

					for (OWLClassExpression e : axiom.getClassExpressions()) {

						inputObjects.addEquivExpr(n, e);
					}
				}
			}

			private class SuperExtractor extends AspectExtractor<OWLSubClassOfAxiom> {

				Class<OWLSubClassOfAxiom> getProcessAxiomType() {

					return OWLSubClassOfAxiom.class;
				}

				OWLClass getEntityOrNull(OWLSubClassOfAxiom axiom) {

					return toTypeOrNull(axiom.getSubClass(), OWLClass.class);
				}

				void process(OWLSubClassOfAxiom axiom, InputClass n) {

					OWLClassExpression sup = axiom.getSuperClass();

					if (!sup.equals(owlThing)) {

						inputObjects.addSuperExpr(n, sup);
					}
				}
			}

			ClassAspectExtractors() {

				new EquivsExtractor();
				new SuperExtractor();
			}

			Class<OWLClass> getProcessEntityType() {

				return OWLClass.class;
			}

			OWLClass getRootEntityOrNull() {

				return owlThing;
			}

			Map<OWLClass, InputClass> getEntityToNameMap() {

				return classes;
			}

			InputClass createInputName(OWLClass entity) {

				return inputObjects.createClass(entity);
			}
		}

		private class IndividualAspectExtractors
							extends
								EntityAspectExtractors<OWLNamedIndividual, InputIndividual> {

			private class EquivsExtractor extends AspectExtractor<OWLSameIndividualAxiom> {

				Class<OWLSameIndividualAxiom> getProcessAxiomType() {

					return OWLSameIndividualAxiom.class;
				}

				OWLNamedIndividual getEntityOrNull(OWLSameIndividualAxiom axiom) {

					return getAnyOfTypeOrNull(axiom.getIndividuals(), OWLNamedIndividual.class);
				}

				void process(OWLSameIndividualAxiom axiom, InputIndividual n) {

					for (OWLIndividual i : axiom.getIndividuals()) {

						OWLNamedIndividual ni = toTypeOrNull(i, OWLNamedIndividual.class);

						if (ni != null) {

							inputObjects.addEquiv(n, ni);
						}
					}
				}
			}

			private class TypeExtractor extends AspectExtractor<OWLClassAssertionAxiom> {

				Class<OWLClassAssertionAxiom> getProcessAxiomType() {

					return OWLClassAssertionAxiom.class;
				}

				OWLNamedIndividual getEntityOrNull(OWLClassAssertionAxiom axiom) {

					return toTypeOrNull(axiom.getIndividual(), OWLNamedIndividual.class);
				}

				void process(OWLClassAssertionAxiom axiom, InputIndividual n) {

					inputObjects.addTypeExpr(n, axiom.getClassExpression());
				}
			}

			private abstract class ValueExtractor
										<A extends OWLPropertyAssertionAxiom<?, ?>>
										extends AspectExtractor<A> {

				OWLNamedIndividual getEntityOrNull(A axiom) {

					return toTypeOrNull(axiom.getSubject(), OWLNamedIndividual.class);
				}
			}

			private class ObjectValueExtractor extends ValueExtractor<OWLObjectPropertyAssertionAxiom> {

				Class<OWLObjectPropertyAssertionAxiom> getProcessAxiomType() {

					return OWLObjectPropertyAssertionAxiom.class;
				}

				void process(OWLObjectPropertyAssertionAxiom axiom, InputIndividual n) {

					inputObjects.addRelation(n, axiom);
				}
			}

			private class DataValueExtractor extends ValueExtractor<OWLDataPropertyAssertionAxiom> {

				Class<OWLDataPropertyAssertionAxiom> getProcessAxiomType() {

					return OWLDataPropertyAssertionAxiom.class;
				}

				void process(OWLDataPropertyAssertionAxiom axiom, InputIndividual n) {

					inputObjects.addRelation(n, axiom);
				}
			}

			IndividualAspectExtractors() {

				new EquivsExtractor();
				new TypeExtractor();
				new ObjectValueExtractor();
				new DataValueExtractor();
			}

			Class<OWLNamedIndividual> getProcessEntityType() {

				return OWLNamedIndividual.class;
			}

			OWLNamedIndividual getRootEntityOrNull() {

				return null;
			}

			Map<OWLNamedIndividual, InputIndividual> getEntityToNameMap() {

				return individuals;
			}

			InputIndividual createInputName(OWLNamedIndividual entity) {

				return inputObjects.createIndividual(entity);
			}
		}

		private class ObjectPropertyAspectExtractors
							extends
								EntityAspectExtractors<OWLObjectProperty, InputObjectProperty> {

			private class EquivsExtractor extends AspectExtractor<OWLEquivalentObjectPropertiesAxiom> {

				Class<OWLEquivalentObjectPropertiesAxiom> getProcessAxiomType() {

					return OWLEquivalentObjectPropertiesAxiom.class;
				}

				OWLObjectProperty getEntityOrNull(OWLEquivalentObjectPropertiesAxiom axiom) {

					return getAnyOfTypeOrNull(axiom.getProperties(), OWLObjectProperty.class);
				}

				void process(OWLEquivalentObjectPropertiesAxiom axiom, InputObjectProperty n) {

					for (OWLObjectPropertyExpression e : axiom.getProperties()) {

						if (e instanceof OWLObjectProperty) {

							inputObjects.addEquiv(n, (OWLObjectProperty)e);
						}
					}
				}
			}

			private class SuperExtractor extends AspectExtractor<OWLSubObjectPropertyOfAxiom> {

				Class<OWLSubObjectPropertyOfAxiom> getProcessAxiomType() {

					return OWLSubObjectPropertyOfAxiom.class;
				}

				OWLObjectProperty getEntityOrNull(OWLSubObjectPropertyOfAxiom axiom) {

					return toTypeOrNull(axiom.getSubProperty(), OWLObjectProperty.class);
				}

				void process(OWLSubObjectPropertyOfAxiom axiom, InputObjectProperty n) {

					OWLObjectPropertyExpression sup = axiom.getSuperProperty();

					if (sup instanceof OWLObjectProperty && !sup.equals(owlTopObjectProperty)) {

						inputObjects.addSuper(n, (OWLObjectProperty)sup);
					}
				}
			}

			private class InverseExtractor extends AspectExtractor<OWLInverseObjectPropertiesAxiom> {

				Class<OWLInverseObjectPropertiesAxiom> getProcessAxiomType() {

					return OWLInverseObjectPropertiesAxiom.class;
				}

				OWLObjectProperty getEntityOrNull(OWLInverseObjectPropertiesAxiom axiom) {

					return toTypeOrNull(axiom.getFirstProperty(), OWLObjectProperty.class);
				}

				void process(OWLInverseObjectPropertiesAxiom axiom, InputObjectProperty n) {

					Set<OWLObjectPropertyExpression> expInvs = axiom.getProperties();
					List<OWLObjectProperty> invs = allToTypeOrNull(expInvs, OWLObjectProperty.class);

					if (invs != null) {

						inputObjects.addInverses(n, invs);
					}
				}
			}

			private class ChainExtractor extends AspectExtractor<OWLSubPropertyChainOfAxiom> {

				Class<OWLSubPropertyChainOfAxiom> getProcessAxiomType() {

					return OWLSubPropertyChainOfAxiom.class;
				}

				OWLObjectProperty getEntityOrNull(OWLSubPropertyChainOfAxiom axiom) {

					return toTypeOrNull(axiom.getSuperProperty(), OWLObjectProperty.class);
				}

				void process(OWLSubPropertyChainOfAxiom axiom, InputObjectProperty n) {

					List<OWLObjectPropertyExpression> expChain = axiom.getPropertyChain();
					List<OWLObjectProperty> chain = allToTypeOrNull(expChain, OWLObjectProperty.class);

					if (chain != null) {

						inputObjects.addChain(n, chain);
					}
				}
			}

			private class TransitivityExtractor extends AspectExtractor<OWLTransitiveObjectPropertyAxiom> {

				Class<OWLTransitiveObjectPropertyAxiom> getProcessAxiomType() {

					return OWLTransitiveObjectPropertyAxiom.class;
				}

				OWLObjectProperty getEntityOrNull(OWLTransitiveObjectPropertyAxiom axiom) {

					return toTypeOrNull(axiom.getProperty(), OWLObjectProperty.class);
				}

				void process(OWLTransitiveObjectPropertyAxiom axiom, InputObjectProperty n) {

					inputObjects.setTransitive(n);
				}
			}

			private class SymmetryExtractor extends AspectExtractor<OWLSymmetricObjectPropertyAxiom> {

				Class<OWLSymmetricObjectPropertyAxiom> getProcessAxiomType() {

					return OWLSymmetricObjectPropertyAxiom.class;
				}

				OWLObjectProperty getEntityOrNull(OWLSymmetricObjectPropertyAxiom axiom) {

					return toTypeOrNull(axiom.getProperty(), OWLObjectProperty.class);
				}

				void process(OWLSymmetricObjectPropertyAxiom axiom, InputObjectProperty n) {

					inputObjects.setSymmetric(n);
				}
			}

			ObjectPropertyAspectExtractors() {

				new EquivsExtractor();
				new SuperExtractor();
				new InverseExtractor();
				new ChainExtractor();
				new TransitivityExtractor();
				new SymmetryExtractor();
			}

			Class<OWLObjectProperty> getProcessEntityType() {

				return OWLObjectProperty.class;
			}

			OWLObjectProperty getRootEntityOrNull() {

				return owlTopObjectProperty;
			}

			Map<OWLObjectProperty, InputObjectProperty> getEntityToNameMap() {

				return objectProperties;
			}

			InputObjectProperty createInputName(OWLObjectProperty entity) {

				return inputObjects.createObjectProperty(entity);
			}
		}

		private class DataPropertyAspectExtractors
							extends
								EntityAspectExtractors<OWLDataProperty, InputDataProperty> {

			private class EquivsExtractor extends AspectExtractor<OWLEquivalentDataPropertiesAxiom> {

				Class<OWLEquivalentDataPropertiesAxiom> getProcessAxiomType() {

					return OWLEquivalentDataPropertiesAxiom.class;
				}

				OWLDataProperty getEntityOrNull(OWLEquivalentDataPropertiesAxiom axiom) {

					return getAnyOfTypeOrNull(axiom.getProperties(), OWLDataProperty.class);
				}

				void process(OWLEquivalentDataPropertiesAxiom axiom, InputDataProperty n) {

					for (OWLDataPropertyExpression e : axiom.getProperties()) {

						if (e instanceof OWLDataProperty) {

							inputObjects.addEquiv(n, (OWLDataProperty)e);
						}
					}
				}
			}

			private class SuperExtractor extends AspectExtractor<OWLSubDataPropertyOfAxiom> {

				Class<OWLSubDataPropertyOfAxiom> getProcessAxiomType() {

					return OWLSubDataPropertyOfAxiom.class;
				}

				OWLDataProperty getEntityOrNull(OWLSubDataPropertyOfAxiom axiom) {

					return toTypeOrNull(axiom.getSubProperty(), OWLDataProperty.class);
				}

				void process(OWLSubDataPropertyOfAxiom axiom, InputDataProperty n) {

					OWLDataPropertyExpression sup = axiom.getSuperProperty();

					if (sup instanceof OWLDataProperty && !sup.equals(owlTopDataProperty)) {

						inputObjects.addSuper(n, (OWLDataProperty)sup);
					}
				}
			}

			DataPropertyAspectExtractors() {

				new EquivsExtractor();
				new SuperExtractor();
			}

			Class<OWLDataProperty> getProcessEntityType() {

				return OWLDataProperty.class;
			}

			OWLDataProperty getRootEntityOrNull() {

				return owlTopDataProperty;
			}

			Map<OWLDataProperty, InputDataProperty> getEntityToNameMap() {

				return dataProperties;
			}

			InputDataProperty createInputName(OWLDataProperty entity) {

				return inputObjects.createDataProperty(entity);
			}
		}

		AxiomProcessingInitialiser(
			OWLOntologyManager manager,
			MappedNames mappedNames,
			OwlInputObjects inputObjects) {

			this.inputObjects = inputObjects;

			OWLDataFactory factory = manager.getOWLDataFactory();

			owlThing = factory.getOWLThing();
			owlTopObjectProperty = factory.getOWLTopObjectProperty();
			owlTopDataProperty = factory.getOWLTopDataProperty();

			inputObjects = new OwlInputObjects(factory, mappedNames);

			new GCIExtractors();
			new ClassAspectExtractors();
			new IndividualAspectExtractors();
			new ObjectPropertyAspectExtractors();
			new DataPropertyAspectExtractors();

			for (OWLOntology ont : manager.getOntologies()) {

				for (OWLAxiom ax : ont.getAxioms(Imports.EXCLUDED)) {

					if (!annotationRelated(ax)) {

						process(ax);
					}
				}
			}

			if (!outOfScopeAxiomTypes.isEmpty()) {

				logOutOfScopeAxiomTypes();
			}
		}

		private boolean annotationRelated(OWLAxiom ax) {

			if (ax instanceof OWLDeclarationAxiom) {

				OWLDeclarationAxiom dax = (OWLDeclarationAxiom)ax;

				return dax.getEntity() instanceof OWLAnnotationProperty;
			}

			return ax instanceof OWLAnnotationAxiom;
		}

		private void process(OWLAxiom ax) {

			for (ProcessorGroup p : processorGroups) {

				if (p.checkProcess(ax)) {

					return;
				}
			}

			outOfScopeAxiomTypes.add(ax.getAxiomType());
		}

		private void logOutOfScopeAxiomTypes() {

			Logger logger = Logger.SINGLETON;

			logger.logOutOfScopeWarningLine("Axiom-types");

			for (AxiomType<?> axType : outOfScopeAxiomTypes) {

				logger.logLine("AXIOM-TYPE: " + axType);
			}

			logger.logSeparatorLine();
		}

		private <T>T toTypeOrNull(Object obj, Class<T> type) {

			return type.isAssignableFrom(obj.getClass()) ? type.cast(obj) : null;
		}

		private <T>List<T> allToTypeOrNull(Collection<?> objs, Class<T> type) {

			List<T> typeObjs = new ArrayList<T>();

			for (Object obj : objs) {

				T typeObj = toTypeOrNull(obj, type);

				if (typeObj == null) {

					return null;
				}

				typeObjs.add(typeObj);
			}

			return typeObjs;
		}

		private <T>T getAnyOfTypeOrNull(Collection<?> objs, Class<T> type) {

			for (Object obj : objs) {

				T typeObj = toTypeOrNull(obj, type);

				if (typeObj != null) {

					return typeObj;
				}
			}

			return null;
		}

		private <T>boolean anyOfType(Collection<?> objs, Class<T> type) {

			return getAnyOfTypeOrNull(objs, type) != null;
		}
	}

	public Collection<InputClass> getClasses() {

		return classes.values();
	}

	public Collection<InputIndividual> getIndividuals() {

		return individuals.values();
	}

	public Collection<InputObjectProperty> getObjectProperties() {

		return objectProperties.values();
	}

	public Collection<InputDataProperty> getDataProperties() {

		return dataProperties.values();
	}

	public Collection<InputEquivalence> getEquivalenceGCIs() {

		return equivalenceGCIs;
	}

	public Collection<InputSubSuper> getSubSuperGCIs() {

		return subSuperGCIs;
	}

	OwlInputAssertions(
		OWLOntologyManager manager,
		MappedNames mappedNames,
		OwlInputObjects inputObjects) {

		new AxiomProcessingInitialiser(manager, mappedNames, inputObjects);
	}

	Collection<OWLClass> getOwlClasses() {

		return classes.keySet();
	}

	Collection<OWLNamedIndividual> getOwlIndividuals() {

		return individuals.keySet();
	}

	Collection<OWLObjectProperty> getOwlObjectProperties() {

		return objectProperties.keySet();
	}

	Collection<OWLDataProperty> getOwlDataProperties() {

		return dataProperties.keySet();
	}
}
