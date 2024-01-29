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

package rekon.build;

import java.util.*;

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class NodeProfilesBuilder {

	private MatchComponents matchComponents;
	private MatchStructures matchStructures;

	private BuildLogger logger;

	private ClassSupersBasedRelator classSupersBasedRelator = new ClassSupersBasedRelator();
	private IndividualTypesBasedRelator individualTypesBasedRelator = new IndividualTypesBasedRelator();
	private IndividualValuesBasedRelator individualValuesBasedRelator = new IndividualValuesBasedRelator();

	private abstract class RelationBuilder<IN extends InputName<?>, RS> {

		Set<Relation> getRelations(IN inputName) {

			Set<Relation> rels = new HashSet<Relation>();

			for (RS rs : getRelationSources(inputName)) {

				Relation r = toRelation(rs);

				if (r != null) {

					rels.add(r);
				}
				else {

					logOutOfScope(inputName, rs);
				}
			}

			return rels;
		}

		abstract Collection<RS> getRelationSources(IN inputName);

		abstract Relation toRelation(RS source);

		abstract void logOutOfScope(IN inputName, RS relSource);
	}

	private abstract class NodeValueRelationBuilder
								<IN extends InputName<?>>
								extends RelationBuilder<IN, InputExpression> {

		Relation toRelation(InputExpression relSource) {

			return matchComponents.toRelation(relSource);
		}
	}

	private class ClassSupersBasedRelator extends NodeValueRelationBuilder<InputClass> {

		Collection<InputExpression> getRelationSources(InputClass inputName) {

			return inputName.getSuperAtomicExprs();
		}

		void logOutOfScope(InputClass inputName, InputExpression relSource) {

			logger.logOutOfScopeSuper(inputName, relSource);
		}
	}

	private class IndividualTypesBasedRelator extends NodeValueRelationBuilder<InputIndividual> {

		Collection<InputExpression> getRelationSources(InputIndividual inputName) {

			return inputName.getTypeExprs();
		}

		void logOutOfScope(InputIndividual inputName, InputExpression relSource) {

			logger.logOutOfScopeType(inputName, relSource);
		}
	}

	private class IndividualValuesBasedRelator extends RelationBuilder<InputIndividual, InputRelation> {

		Collection<InputRelation> getRelationSources(InputIndividual inputName) {

			return inputName.getRelations();
		}

		Relation toRelation(InputRelation relSource) {

			return matchComponents.toRelation(relSource);
		}

		void logOutOfScope(InputIndividual inputName, InputRelation relSource) {

			logger.logOutOfScopeValue(inputName, relSource);
		}
	}

	NodeProfilesBuilder(
		InputAssertions assertions,
		MatchComponents matchComponents,
		MatchStructures matchStructures,
		BuildLogger logger) {

		this.matchComponents = matchComponents;
		this.matchStructures = matchStructures;
		this.logger = logger;

		for (InputClass c : assertions.getClasses()) {

			createForNode(c.getClassNode(), classSupersBasedRelator.getRelations(c));
		}

		for (InputIndividual i : assertions.getIndividuals()) {

			createForNode(i.getIndividualNode(), getInputIndividualRelations(i));
		}
	}

	private void createForNode(NodeX n, Collection<Relation> relations) {

		matchStructures.checkAddProfilePattern(n, relations);
	}

	private Set<Relation> getInputIndividualRelations(InputIndividual i) {

		Set<Relation> rels = new HashSet<Relation>();

		rels.addAll(individualTypesBasedRelator.getRelations(i));
		rels.addAll(individualValuesBasedRelator.getRelations(i));

		return rels;
	}
}
