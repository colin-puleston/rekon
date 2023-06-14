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

	private MatchComponents matchComponents;
	private MatchStructures matchStructures;

	private SupersRelationCreator supersRelations = new SupersRelationCreator();
	private TypesRelationCreator typesRelations = new TypesRelationCreator();
	private NodeValuesRelationCreator nodeValuesRelations = new NodeValuesRelationCreator();
	private DataValuesRelationCreator dataValuesRelations = new DataValuesRelationCreator();

	private abstract class RelationCreator<E extends AssertedEntity<?>, S> {

		Set<Relation> getAssertedRelations(E entity) {

			Set<Relation> rels = new HashSet<Relation>();

			for (S s : getRelationSources(entity)) {

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

		abstract Relation toRelation(S source);

		abstract void logOutOfScope(E entity, S source);
	}

	private abstract class ClassesRelationCreator
								<E extends AssertedEntity<?>>
								extends RelationCreator<E, OWLClassExpression> {

		Relation toRelation(OWLClassExpression source) {

			return matchComponents.toRelation(source);
		}
	}

	private class SupersRelationCreator extends ClassesRelationCreator<AssertedClass> {

		Collection<OWLClassExpression> getRelationSources(AssertedClass entity) {

			return entity.getSuperExprs();
		}

		void logOutOfScope(AssertedClass entity, OWLClassExpression source) {

			logOutOfScopeRef("CLASS", "SUPER", entity, source);
		}
	}

	private class TypesRelationCreator extends ClassesRelationCreator<AssertedIndividual> {

		Collection<OWLClassExpression> getRelationSources(AssertedIndividual entity) {

			return entity.getTypeExprs();
		}

		void logOutOfScope(AssertedIndividual entity, OWLClassExpression source) {

			logOutOfScopeRef("INDIVIDUAL", "TYPE", entity, source);
		}
	}

	private abstract class ValuesRelationCreator
								<S extends AssertedValue<?, ?>>
								extends RelationCreator<AssertedIndividual, S> {

		void logOutOfScope(AssertedIndividual entity, S source) {

			logOutOfScopeRef("INDIVIDUAL", "VALUE", entity, source.getValue());
		}
	}

	private class NodeValuesRelationCreator
					extends
						ValuesRelationCreator<AssertedObjectValue> {

		Collection<AssertedObjectValue> getRelationSources(AssertedIndividual entity) {

			return entity.getObjectValues();
		}

		Relation toRelation(AssertedObjectValue source) {

			return matchComponents.toNodeValueRelation(source);
		}
	}

	private class DataValuesRelationCreator
						extends
							ValuesRelationCreator<AssertedDataValue> {

		Collection<AssertedDataValue> getRelationSources(AssertedIndividual entity) {

			return entity.getDataValues();
		}

		Relation toRelation(AssertedDataValue source) {

			return matchComponents.toDataValueRelation(source);
		}
	}

	NodeProfilesCreator(
		Assertions assertions,
		MappedNames mappedNames,
		MatchComponents matchComponents,
		MatchStructures matchStructures) {

		this.matchStructures = matchStructures;
		this.matchComponents = matchComponents;

		for (AssertedClass c : assertions.getClasses()) {

			createForClass(c, mappedNames.get(c.getEntity()));
		}

		for (AssertedIndividual i : assertions.getIndividuals()) {

			createForIndividual(i, mappedNames.get(i.getEntity()));
		}
	}

	private void createForClass(AssertedClass c, ClassName n) {

		matchStructures.checkAddPatternNode(n, supersRelations.getAssertedRelations(c));
	}

	private void createForIndividual(AssertedIndividual i, IndividualName n) {

		matchStructures.checkAddPatternNode(n, getAssertedIndividualRelations(i));
	}

	private Set<Relation> getAssertedIndividualRelations(AssertedIndividual i) {

		Set<Relation> rels = new HashSet<Relation>();

		rels.addAll(typesRelations.getAssertedRelations(i));
		rels.addAll(nodeValuesRelations.getAssertedRelations(i));
		rels.addAll(dataValuesRelations.getAssertedRelations(i));

		return rels;
	}

	private void logOutOfScopeRef(
					String referDesc,
					String refedDesc,
					AssertedEntity<?> refer,
					OWLObject refed) {

		Logger logger = Logger.SINGLETON;

		logger.logOutOfScopeWarningLine(referDesc + " " + refedDesc);
		logger.logLine(referDesc + ": " + refer.getEntity());
		logger.logLine(refedDesc + ": " + refed);
		logger.logSeparatorLine();
	}
}
