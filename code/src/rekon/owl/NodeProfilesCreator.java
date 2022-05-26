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
class NodeProfilesCreator {

	private Assertions assertions;

	private MappedNames mappedNames;
	private MatchComponents matchComponents;
	private MatchStructures matchStructures;

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

			return matchComponents.toRelation(source);
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

			return matchComponents.toObjectValueRelation(source);
		}
	}

	private class DataValuesRelationCreator
						extends
							ValuesRelationCreator<DataValueAssertion> {

		Collection<DataValueAssertion> getRelationSources(OWLNamedIndividual entity) {

			return assertions.getDataValues(entity);
		}

		Relation toRelation(DataValueAssertion source) {

			return matchComponents.toDataValueRelation(source);
		}
	}

	NodeProfilesCreator(
		Assertions assertions,
		MappedNames mappedNames,
		MatchComponents matchComponents,
		MatchStructures matchStructures) {

		this.assertions = assertions;
		this.mappedNames = mappedNames;
		this.matchStructures = matchStructures;
		this.matchComponents = matchComponents;

		for (ClassName n : mappedNames.getClassNames()) {

			createForClass(n);
		}

		for (IndividualName n : mappedNames.getIndividualNames()) {

			createForIndividual(n);
		}
	}

	private void createForClass(ClassName n) {

		OWLClass c = MappedNames.toMappedEntity(n, OWLClass.class);

		matchStructures.setNodeProfile(n, supersRelations.getAssertedRelations(c));
	}

	private void createForIndividual(IndividualName n) {

		OWLNamedIndividual i = MappedNames.toMappedEntity(n, OWLNamedIndividual.class);

		matchStructures.setNodeProfile(n, getAssertedIndividualRelations(i));
	}

	private Set<Relation> getAssertedIndividualRelations(OWLNamedIndividual i) {

		Set<Relation> rels = new HashSet<Relation>();

		rels.addAll(typesRelations.getAssertedRelations(i));
		rels.addAll(objectValuesRelations.getAssertedRelations(i));
		rels.addAll(dataValuesRelations.getAssertedRelations(i));

		return rels;
	}

	private void logOutOfScopeRef(
					String referDesc,
					String refedDesc,
					OWLEntity refer,
					OWLObject refed) {

		Logger logger = Logger.SINGLETON;

		logger.logOutOfScopeWarningLine(referDesc + " " + refedDesc);
		logger.logLine(referDesc + ": " + refer);
		logger.logLine(refedDesc + ": " + refed);
	}
}
