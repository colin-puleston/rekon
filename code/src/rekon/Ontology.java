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
class Ontology {

	private MappedNames mappedNames;
	private MatchableNodes matchables = new MatchableNodes();
	private PatternComponents patternComponents;

	private Collection<NodeName> allNodeNames;

	private boolean logging = false;

	private class AssertedNamesInitialiser {

		private Assertions assertions;

		private SupersRelationCreator supersRelations = new SupersRelationCreator();
		private TypesRelationCreator typesRelations = new TypesRelationCreator();
		private ObjectValuesRelationCreator objectValuesRelations = new ObjectValuesRelationCreator();
		private DataValuesRelationCreator dataValuesRelations = new DataValuesRelationCreator();

		private abstract class RelationCreator<E extends OWLEntity, S> {

			Set<Relation> getAssertedRelations(E entity) {

				Set<Relation> rels = new HashSet<Relation>();

				for (S s : getRelationSources(entity)) {

					if (excludedRelationSource(s)) {

						continue;
					}

					Relation r = toRelation(s);

					if (r != null) {

						rels.add(r);
					}
					else {

						logOutOfScope(entity, s);
					}
				}

				return rels;
			}

			abstract Collection<S> getRelationSources(E entity);

			abstract boolean excludedRelationSource(S source);

			abstract Relation toRelation(S source);

			abstract void logOutOfScope(E entity, S source);
		}

		private abstract class ClassesRelationCreator
									<E extends OWLEntity>
									extends RelationCreator<E, OWLClassExpression> {

			boolean excludedRelationSource(OWLClassExpression source) {

				return source instanceof OWLClass;
			}

			Relation toRelation(OWLClassExpression source) {

				return patternComponents.toRelation(source);
			}
		}

		private class SupersRelationCreator extends ClassesRelationCreator<OWLClass> {

			Collection<OWLClassExpression> getRelationSources(OWLClass entity) {

				return assertions.getSuperExprs(entity);
			}

			void logOutOfScope(OWLClass entity, OWLClassExpression source) {

				logOutOfScopeRef("CLASS", "SUPER", entity, source);
			}
		}

		private class TypesRelationCreator extends ClassesRelationCreator<OWLNamedIndividual> {

			Collection<OWLClassExpression> getRelationSources(OWLNamedIndividual entity) {

				return assertions.getTypeExprs(entity);
			}

			void logOutOfScope(OWLNamedIndividual entity, OWLClassExpression source) {

				logOutOfScopeRef("INDIVIDUAL", "TYPE", entity, source);
			}
		}

		private abstract class ValuesRelationCreator
									<S extends ValueAssertion<?, ?>>
									extends RelationCreator<OWLNamedIndividual, S> {

			boolean excludedRelationSource(S source) {

				return false;
			}

			void logOutOfScope(OWLNamedIndividual entity, S source) {

				logOutOfScopeRef("INDIVIDUAL", "VALUE", entity, source.getValue());
			}
		}

		private class ObjectValuesRelationCreator
						extends
							ValuesRelationCreator<ObjectValueAssertion> {

			Collection<ObjectValueAssertion> getRelationSources(OWLNamedIndividual entity) {

				return assertions.getObjectValues(entity);
			}

			Relation toRelation(ObjectValueAssertion source) {

				return patternComponents.toObjectValueRelation(source);
			}
		}

		private class DataValuesRelationCreator
							extends
								ValuesRelationCreator<DataValueAssertion> {

			Collection<DataValueAssertion> getRelationSources(OWLNamedIndividual entity) {

				return assertions.getDataValues(entity);
			}

			Relation toRelation(DataValueAssertion source) {

				return patternComponents.toDataValueRelation(source);
			}
		}

		AssertedNamesInitialiser(Assertions assertions) {

			this.assertions = assertions;

			for (ClassName name : mappedNames.getAllClassNames()) {

				initialiseForClass(name);
			}

			for (IndividualName name : mappedNames.getAllIndividualNames()) {

				initialiseForIndividual(name);
			}
		}

		private void initialiseForClass(ClassName name) {

			OWLClass cls = name.getOWLClass();
			Set<NodePattern> defns = getDefinitions(cls);
			Set<Relation> rels = getAssertedRelations(cls);

			matchables.checkAddForClass(name, defns, rels);
		}

		private void initialiseForIndividual(IndividualName name) {

			OWLNamedIndividual ind = name.getOWLIndividual();
			Set<Relation> rels = getAssertedRelations(ind);

			matchables.checkAddForIndividual(name, rels);
		}

		private Set<NodePattern> getDefinitions(OWLClass cls) {

			Set<NodePattern> defns = new HashSet<NodePattern>();

			for (OWLClassExpression e : assertions.getEquivalentExprs(cls)) {

				if (e instanceof OWLClass) {

					continue;
				}

				NodePattern d = patternComponents.toNodePattern(e);

				if (d != null) {

					defns.add(d);
				}
				else {

					logOutOfScopeRef("CLASS", "DEFINITION", cls, e);
				}
			}

			return defns;
		}

		private Set<Relation> getAssertedRelations(OWLClass cls) {

			return supersRelations.getAssertedRelations(cls);
		}

		private Set<Relation> getAssertedRelations(OWLNamedIndividual ind) {

			Set<Relation> rels = new HashSet<Relation>();

			rels.addAll(typesRelations.getAssertedRelations(ind));
			rels.addAll(objectValuesRelations.getAssertedRelations(ind));
			rels.addAll(dataValuesRelations.getAssertedRelations(ind));

			return rels;
		}
	}

	private class GeneralClassAxiomsInitialiser {

		private Assertions assertions;
		private FreeClasses gciClasses = FreeClasses.forGCIs(matchables, allNodeNames);

		GeneralClassAxiomsInitialiser(Assertions assertions) {

			this.assertions = assertions;

			for (OWLEquivalentClassesAxiom ax : getEquivalenceAxioms()) {

				if (!createForEquivalents(ax)) {

					logOutOfScopeAxiom("EQUIVALENT-CLASSES", ax);
				}
			}

			for (OWLSubClassOfAxiom ax : getSubClassAxioms()) {

				if (!createForSubClass(ax)) {

					logOutOfScopeAxiom("SUB-CLASS", ax);
				}
			}
		}

		private boolean createForEquivalents(OWLEquivalentClassesAxiom ax) {

			Set<NodePattern> patterns = new HashSet<NodePattern>();

			for (OWLClassExpression e : ax.getClassExpressions()) {

				if (e instanceof OWLClass) {

					return true;
				}

				NodePattern p = patternComponents.toNodePattern(e);

				if (p == null) {

					return false;
				}

				patterns.add(p);
			}

			gciClasses.create(patterns);

			return true;
		}

		private boolean createForSubClass(OWLSubClassOfAxiom ax) {

			OWLClassExpression sub = ax.getSubClass();

			if (sub instanceof OWLClass) {

				return true;
			}

			NodePattern pSub = patternComponents.toNodePattern(sub);

			if (pSub == null) {

				return false;
			}

			OWLClassExpression sup = ax.getSuperClass();

			if (sup instanceof OWLClass) {

				gciClasses.create(mappedNames.get((OWLClass)sup), pSub);
			}
			else {

				NodePattern pSup = patternComponents.toNodePattern(sup);

				if (pSup == null) {

					return false;
				}

				gciClasses.create(gciClasses.create(pSup), pSub);
			}

			return true;
		}

		private Collection<OWLEquivalentClassesAxiom> getEquivalenceAxioms() {

			return assertions.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		}

		private Collection<OWLSubClassOfAxiom> getSubClassAxioms() {

			return assertions.getAxioms(AxiomType.SUBCLASS_OF);
		}
	}

	Ontology(Assertions assertions) {

		mappedNames = new MappedNames(assertions);
		allNodeNames = mappedNames.getAllNodeNames();
		patternComponents = createPatternComponents();

		processMappedNamesPostAdditions();

		new AssertedNamesInitialiser(assertions);
		new GeneralClassAxiomsInitialiser(assertions);

		new OntologyClassifier(this);

		processNamesPostClassification();
	}

	MappedNames getMappedNames() {

		return mappedNames;
	}

	MatchableNodes getMatchables() {

		return matchables;
	}

	Collection<NodeName> getAllNodeNames() {

		return allNodeNames;
	}

	private PatternComponents createPatternComponents() {

		FreeClasses exprClasses = FreeClasses.forOntologyExprs(matchables, allNodeNames);

		return new PatternComponents(mappedNames, exprClasses, false);
	}

	private void processMappedNamesPostAdditions() {

		for (Name n : getAllNames()) {

			n.getClassifier().onPostAssertionAdditions();
		}
	}

	private void processNamesPostClassification() {

		for (Name n : getAllNames()) {

			n.completeClassification();
		}

		for (int i = 0 ; i < NameClassification.INITIALISATION_OPS ; i++) {

			for (Name n : getAllNames()) {

				n.getClassification().performInitialisationOp(i);
			}
		}
	}

	private Collection<Name> getAllNames() {

		List<Name> names = new ArrayList<Name>();

		names.addAll(allNodeNames);
		names.addAll(mappedNames.getAllPropertyNames());

		return names;
	}

	private void logOutOfScopeAxiom(String axiomDesc, OWLAxiom axiom) {

		logOutOfScopeWarningLine(axiomDesc);
		logLine("AXIOM: " + axiom);
	}

	private void logOutOfScopeRef(
					String referDesc,
					String refedDesc,
					OWLEntity refer,
					OWLObject refed) {

		logOutOfScopeWarningLine(referDesc + " " + refedDesc);
		logLine(referDesc + ": " + refer);
		logLine(refedDesc + ": " + refed);
	}

	private void logOutOfScopeWarningLine(String entityDesc) {

		logLine("\nWARNING: " + entityDesc + " out of scope...");
	}

	private void logLine(String line) {

		if (logging) {

			System.out.println(line);
		}
	}
}
