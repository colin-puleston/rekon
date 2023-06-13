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

/**
 * @author Colin Puleston
 */
class Assertions {

	final OWLClass owlThing;
	final OWLObjectProperty owlTopObjectProperty;
	final OWLDataProperty owlTopDataProperty;

	private Map<OWLClass, AssertedClass> classes = new HashMap<OWLClass, AssertedClass>();
	private Map<OWLNamedIndividual, AssertedIndividual> individuals = new HashMap<OWLNamedIndividual, AssertedIndividual>();
	private Map<OWLObjectProperty, AssertedObjectProperty> objectProperties = new HashMap<OWLObjectProperty, AssertedObjectProperty>();
	private Map<OWLDataProperty, AssertedDataProperty> dataProperties = new HashMap<OWLDataProperty, AssertedDataProperty>();

	private Set<OWLEquivalentClassesAxiom> equivGCIs = new HashSet<OWLEquivalentClassesAxiom>();
	private Set<OWLSubClassOfAxiom> superGCIs = new HashSet<OWLSubClassOfAxiom>();

	private class AxiomProcessingInitialiser {

		private OWLDataFactory factory;

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

						equivGCIs.add(axiom);

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

					superGCIs.add(axiom);

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
									AE extends AssertedEntity<?>>
									extends ProcessorGroup {

			abstract class AspectExtractor<A extends OWLAxiom> extends Processor<A> {

				boolean checkTypeProcess(A axiom) {

					E e = getEntityOrNull(axiom);

					if (e != null && !e.equals(getRootEntityOrNull())) {

						process(axiom, ensureAssertedEntity(e));

						return true;
					}

					return false;
				}

				abstract E getEntityOrNull(A axiom);

				abstract void process(A axiom, AE ae);
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

				void process(OWLDeclarationAxiom axiom, AE ae) {
				}
			}

			EntityAspectExtractors() {

				new DeclarationExtractor();
			}

			abstract Class<E> getProcessEntityType();

			abstract E getRootEntityOrNull();

			abstract Map<E, AE> getEntitiesMap();

			abstract AE createAssertedEntity(E entity);

			private AE ensureAssertedEntity(E entity) {

				Map<E, AE> map = getEntitiesMap();
				AE ae = map.get(entity);

				if (ae == null) {

					ae = createAssertedEntity(entity);

					map.put(entity, ae);
				}

				return ae;
			}
		}

		private class ClassAspectExtractors
							extends
								EntityAspectExtractors<OWLClass, AssertedClass> {

			private class EquivsExtractor extends AspectExtractor<OWLEquivalentClassesAxiom> {

				Class<OWLEquivalentClassesAxiom> getProcessAxiomType() {

					return OWLEquivalentClassesAxiom.class;
				}

				OWLClass getEntityOrNull(OWLEquivalentClassesAxiom axiom) {

					return getAnyOfTypeOrNull(axiom.getClassExpressions(), OWLClass.class);
				}

				void process(OWLEquivalentClassesAxiom axiom, AssertedClass ae) {

					for (OWLClassExpression e : axiom.getClassExpressionsMinus(ae.getEntity())) {

						ae.addEquivExpr(e);
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

				void process(OWLSubClassOfAxiom axiom, AssertedClass ae) {

					OWLClassExpression sup = axiom.getSuperClass();

					if (!sup.equals(owlThing)) {

						ae.addSuperExpr(sup);
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

			Map<OWLClass, AssertedClass> getEntitiesMap() {

				return classes;
			}

			AssertedClass createAssertedEntity(OWLClass entity) {

				return new AssertedClass(entity, factory);
			}
		}

		private class IndividualAspectExtractors
							extends
								EntityAspectExtractors<OWLNamedIndividual, AssertedIndividual> {

			private class EquivsExtractor extends AspectExtractor<OWLSameIndividualAxiom> {

				Class<OWLSameIndividualAxiom> getProcessAxiomType() {

					return OWLSameIndividualAxiom.class;
				}

				OWLNamedIndividual getEntityOrNull(OWLSameIndividualAxiom axiom) {

					return getAnyOfTypeOrNull(axiom.getIndividuals(), OWLNamedIndividual.class);
				}

				void process(OWLSameIndividualAxiom axiom, AssertedIndividual ae) {

					for (OWLIndividual i : axiom.getIndividuals()) {

						OWLNamedIndividual ni = toTypeOrNull(i, OWLNamedIndividual.class);

						if (ni != null && ni != ae.getEntity()) {

							ae.addEquiv(ni);
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

				void process(OWLClassAssertionAxiom axiom, AssertedIndividual ae) {

					ae.addTypeExpr(axiom.getClassExpression());
				}
			}

			private class ObjectValueExtractor extends AspectExtractor<OWLObjectPropertyAssertionAxiom> {

				Class<OWLObjectPropertyAssertionAxiom> getProcessAxiomType() {

					return OWLObjectPropertyAssertionAxiom.class;
				}

				OWLNamedIndividual getEntityOrNull(OWLObjectPropertyAssertionAxiom axiom) {

					return toTypeOrNull(axiom.getSubject(), OWLNamedIndividual.class);
				}

				void process(OWLObjectPropertyAssertionAxiom axiom, AssertedIndividual ae) {

					OWLObjectProperty p = toTypeOrNull(axiom.getProperty(), OWLObjectProperty.class);

					if (p != null) {

						ae.addObjectValue(new AssertedObjectValue(p, axiom.getObject()));
					}
				}
			}

			private class DataValueExtractor extends AspectExtractor<OWLDataPropertyAssertionAxiom> {

				Class<OWLDataPropertyAssertionAxiom> getProcessAxiomType() {

					return OWLDataPropertyAssertionAxiom.class;
				}

				OWLNamedIndividual getEntityOrNull(OWLDataPropertyAssertionAxiom axiom) {

					return toTypeOrNull(axiom.getSubject(), OWLNamedIndividual.class);
				}

				void process(OWLDataPropertyAssertionAxiom axiom, AssertedIndividual ae) {

					OWLDataProperty p = toTypeOrNull(axiom.getProperty(), OWLDataProperty.class);

					if (p != null) {

						ae.addDataValue(new AssertedDataValue(p, axiom.getObject()));
					}
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

			Map<OWLNamedIndividual, AssertedIndividual> getEntitiesMap() {

				return individuals;
			}

			AssertedIndividual createAssertedEntity(OWLNamedIndividual entity) {

				return new AssertedIndividual(entity);
			}
		}

		private class ObjectPropertyAspectExtractors
							extends
								EntityAspectExtractors<OWLObjectProperty, AssertedObjectProperty> {

			private class EquivsExtractor extends AspectExtractor<OWLEquivalentObjectPropertiesAxiom> {

				Class<OWLEquivalentObjectPropertiesAxiom> getProcessAxiomType() {

					return OWLEquivalentObjectPropertiesAxiom.class;
				}

				OWLObjectProperty getEntityOrNull(OWLEquivalentObjectPropertiesAxiom axiom) {

					return getAnyOfTypeOrNull(axiom.getProperties(), OWLObjectProperty.class);
				}

				void process(OWLEquivalentObjectPropertiesAxiom axiom, AssertedObjectProperty ae) {

					for (OWLObjectPropertyExpression e : axiom.getPropertiesMinus(ae.getEntity())) {

						ae.checkAddEquiv(e);
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

				void process(OWLSubObjectPropertyOfAxiom axiom, AssertedObjectProperty ae) {

					OWLObjectPropertyExpression sup = axiom.getSuperProperty();

					if (!sup.equals(owlTopObjectProperty)) {

						ae.checkAddSuper(sup);
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

				void process(OWLInverseObjectPropertiesAxiom axiom, AssertedObjectProperty ae) {

					Set<OWLObjectPropertyExpression> expInvs = axiom.getPropertiesMinus(ae.getEntity());
					List<OWLObjectProperty> invs = allToTypeOrNull(expInvs, OWLObjectProperty.class);

					if (invs != null) {

						ae.addToInverses(invs);
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

				void process(OWLSubPropertyChainOfAxiom axiom, AssertedObjectProperty ae) {

					List<OWLObjectPropertyExpression> expChain = axiom.getPropertyChain();
					List<OWLObjectProperty> chain = allToTypeOrNull(expChain, OWLObjectProperty.class);

					if (chain != null) {

						ae.addChain(chain);
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

				void process(OWLTransitiveObjectPropertyAxiom axiom, AssertedObjectProperty ae) {

					ae.setTransitive();
				}
			}

			private class SymmetryExtractor extends AspectExtractor<OWLSymmetricObjectPropertyAxiom> {

				Class<OWLSymmetricObjectPropertyAxiom> getProcessAxiomType() {

					return OWLSymmetricObjectPropertyAxiom.class;
				}

				OWLObjectProperty getEntityOrNull(OWLSymmetricObjectPropertyAxiom axiom) {

					return toTypeOrNull(axiom.getProperty(), OWLObjectProperty.class);
				}

				void process(OWLSymmetricObjectPropertyAxiom axiom, AssertedObjectProperty ae) {

					ae.setSymmetric();
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

			Map<OWLObjectProperty, AssertedObjectProperty> getEntitiesMap() {

				return objectProperties;
			}

			AssertedObjectProperty createAssertedEntity(OWLObjectProperty entity) {

				return new AssertedObjectProperty(entity);
			}
		}

		private class DataPropertyAspectExtractors
							extends
								EntityAspectExtractors<OWLDataProperty, AssertedDataProperty> {

			private class EquivsExtractor extends AspectExtractor<OWLEquivalentDataPropertiesAxiom> {

				Class<OWLEquivalentDataPropertiesAxiom> getProcessAxiomType() {

					return OWLEquivalentDataPropertiesAxiom.class;
				}

				OWLDataProperty getEntityOrNull(OWLEquivalentDataPropertiesAxiom axiom) {

					return getAnyOfTypeOrNull(axiom.getProperties(), OWLDataProperty.class);
				}

				void process(OWLEquivalentDataPropertiesAxiom axiom, AssertedDataProperty ae) {

					for (OWLDataPropertyExpression e : axiom.getPropertiesMinus(ae.getEntity())) {

						ae.checkAddEquiv(e);
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

				void process(OWLSubDataPropertyOfAxiom axiom, AssertedDataProperty ae) {

					OWLDataPropertyExpression sup = axiom.getSuperProperty();

					if (!sup.equals(owlTopDataProperty)) {

						ae.checkAddSuper(sup);
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

			Map<OWLDataProperty, AssertedDataProperty> getEntitiesMap() {

				return dataProperties;
			}

			AssertedDataProperty createAssertedEntity(OWLDataProperty entity) {

				return new AssertedDataProperty(entity);
			}
		}

		AxiomProcessingInitialiser(OWLOntologyManager manager) {

			factory = manager.getOWLDataFactory();

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

	Assertions(OWLOntologyManager manager) {

		OWLDataFactory factory = manager.getOWLDataFactory();

		owlThing = factory.getOWLThing();
		owlTopObjectProperty = factory.getOWLTopObjectProperty();
		owlTopDataProperty = factory.getOWLTopDataProperty();

		new AxiomProcessingInitialiser(manager);
	}

	Collection<AssertedClass> getClasses() {

		return classes.values();
	}

	Collection<AssertedIndividual> getIndividuals() {

		return individuals.values();
	}

	Collection<AssertedObjectProperty> getObjectProperties() {

		return objectProperties.values();
	}

	Collection<AssertedDataProperty> getDataProperties() {

		return dataProperties.values();
	}

	Collection<OWLEquivalentClassesAxiom> getEquivGCIs() {

		return equivGCIs;
	}

	Collection<OWLSubClassOfAxiom> getSuperGCIs() {

		return superGCIs;
	}
}
