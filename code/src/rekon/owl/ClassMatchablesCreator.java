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
class ClassMatchablesCreator {

	private Assertions assertions;
	private PatternComponents patternComponents;

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

	ClassMatchablesCreator(Assertions assertions, PatternComponents patternComponents) {

		this.assertions = assertions;
		this.patternComponents = patternComponents;
	}

	void createForClass(ClassName name, MatchableNodes matchables) {

		OWLClass cls = MappedNames.toMappedEntity(name, OWLClass.class);
		Set<NodePattern> defns = getDefinitions(cls);
		Set<Relation> rels = getAssertedRelations(cls);

		matchables.checkAddForClass(name, defns, rels);
	}

	void createForIndividual(IndividualName name, MatchableNodes matchables) {

		OWLNamedIndividual ind = MappedNames.toMappedEntity(name, OWLNamedIndividual.class);
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
