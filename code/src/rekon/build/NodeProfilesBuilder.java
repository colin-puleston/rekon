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
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
class NodeProfilesBuilder extends MatchStuctureBuilder {

	private OntologyNames names;
	private InputAxioms axioms;
	private ComponentBuilder components;

	private class ProfilePatternsBuilder {

		private Map<NodeX, List<Relation>> relationsByNode
							= new HashMap<NodeX, List<Relation>>();

		void checkAddRelation(NodeX node, InputRelation relSource) {

			Relation rel = components.toRelation(relSource);

			if (rel != null) {

				resolveRelations(node).add(rel);
			}
		}

		void createAllProfiles(Collection<? extends NodeX> nodes) {

			for (NodeX n : nodes) {

				checkAddProfilePattern(n, getRelations(n));
			}
		}

		private List<Relation> resolveRelations(NodeX node) {

			List<Relation> rels = relationsByNode.get(node);

			if (rels == null) {

				rels = new ArrayList<Relation>();

				relationsByNode.put(node, rels);
			}

			return rels;
		}

		private List<Relation> getRelations(NodeX node) {

			List<Relation> rels = relationsByNode.get(node);

			return rels != null ? rels : Collections.emptyList();
		}
	}

	private abstract class TypeNodesProfileCreator<A extends InputAxiom> {

		TypeNodesProfileCreator() {

			ProfilePatternsBuilder ppBuilder = new ProfilePatternsBuilder();

			addRelations(ppBuilder);

			ppBuilder.createAllProfiles(getTypeNodes());
		}

		void addRelations(ProfilePatternsBuilder ppBuilder) {

			for (A ax : getAxioms()) {

				NodeX sub = getSub(ax);
				InputComplexSuper sup = getSuper(ax);

				switch (sup.getComplexSuperType()) {

					case DISJUNCTION:

						checkCreateDisjunctionProfile(sub, sup.asDisjuncts());
						break;

					case RELATION:

						ppBuilder.checkAddRelation(sub, sup.asRelation());
						break;

					case OUT_OF_SCOPE:

						break;
				}
			}
		}

		abstract Collection<A> getAxioms();

		abstract NodeX getSub(A axiom);

		abstract InputComplexSuper getSuper(A axiom);

		abstract Collection<? extends NodeX> getTypeNodes();

		private void checkCreateDisjunctionProfile(
						NodeX sub,
						Collection<InputNode> supDisjuncts) {

			List<Pattern> djs = components.toPatternDisjunction(supDisjuncts);

			if (djs != null) {

				addProfileDisjunction(sub, djs);
			}
		}
	}

	private class ClassNodesProfileCreator
						extends
							TypeNodesProfileCreator<InputClassSubComplexSuper> {

		Collection<InputClassSubComplexSuper> getAxioms() {

			return axioms.getClassSubComplexSupers();
		}

		NodeX getSub(InputClassSubComplexSuper axiom) {

			return axiom.getSub();
		}

		InputComplexSuper getSuper(InputClassSubComplexSuper axiom) {

			return axiom.getSuper();
		}

		Collection<? extends NodeX> getTypeNodes() {

			return names.getClassNodes();
		}
	}

	private class IndividualNodesProfileCreator
						extends
							TypeNodesProfileCreator<InputIndividualComplexType> {

		void addRelations(ProfilePatternsBuilder ppBuilder) {

			super.addRelations(ppBuilder);

			for (InputIndividualRelation ax : axioms.getIndividualRelations()) {

				ppBuilder.checkAddRelation(ax.getIndividual(), ax.getRelation());
			}
		}

		Collection<InputIndividualComplexType> getAxioms() {

			return axioms.getIndividualComplexTypes();
		}

		NodeX getSub(InputIndividualComplexType axiom) {

			return axiom.getIndividual();
		}

		InputComplexSuper getSuper(InputIndividualComplexType axiom) {

			return axiom.getType();
		}

		Collection<? extends NodeX> getTypeNodes() {

			return names.getIndividualNodes();
		}
	}

	NodeProfilesBuilder(
		MatchStructures matchStructures,
		OntologyNames names,
		InputAxioms axioms,
		ComponentBuilder components) {

		super(matchStructures);

		this.names = names;
		this.axioms = axioms;
		this.components = components;

		new ClassNodesProfileCreator();
		new IndividualNodesProfileCreator();
	}
}
