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
class NodeProfilesBuilder {

	private MatchStructures matchStructures;
	private OntologyNames names;
	private InputAxioms axioms;
	private ComponentBuilder components;

	private class ProfilePatternsBuilder {

		private Map<NodeX, List<Relation>> relationsByNode
							= new HashMap<NodeX, List<Relation>>();

		void checkAddRelation(NodeX node, InputRelation relSource) {

			Relation rel = components.toRelation(relSource);

			if (rel != null) {

				addRelation(node, rel);
			}
		}

		void addRelation(NodeX node, Relation relation) {

			resolveRelations(node).add(relation);
		}

		void createAllProfiles(Iterable<? extends NodeX> nodes) {

			for (NodeX n : nodes) {

				matchStructures.checkAddProfilePattern(n, getRelations(n));
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

			processSourceAxioms(ppBuilder);

			ppBuilder.createAllProfiles(getTypeNodes());
		}

		void processSourceAxioms(ProfilePatternsBuilder ppBuilder) {

			for (A ax : getSourceAxioms()) {

				NodeX sub = getSub(ax);
				InputComplexSuper sup = getSuper(ax);

				switch (sup.getComplexSuperType()) {

					case CONJUNCTION:

						Pattern p = components.toPattern(sup.toNode());

						if (p != null) {

							matchStructures.addProfilePattern(sub, p);
						}
						break;

					case RELATION:

						ppBuilder.checkAddRelation(sub, sup.asRelation());
						break;

					case OUT_OF_SCOPE:

						break;
				}
			}
		}

		abstract Iterable<A> getSourceAxioms();

		abstract NodeX getSub(A axiom);

		abstract InputComplexSuper getSuper(A axiom);

		abstract Iterable<? extends NodeX> getTypeNodes();
	}

	private class ClassNodesProfileCreator
						extends
							TypeNodesProfileCreator<InputClassSubComplexSuper> {

		void processSourceAxioms(ProfilePatternsBuilder ppBuilder) {

			super.processSourceAxioms(ppBuilder);

			ClassNode root = names.getRootClassNode();

			for (InputNodePropertyRange ax : axioms.getNodePropertyRanges()) {

				ppBuilder.addRelation(root, toAllRelation(ax));
			}
		}

		Iterable<InputClassSubComplexSuper> getSourceAxioms() {

			return axioms.getClassSubComplexSupers();
		}

		NodeX getSub(InputClassSubComplexSuper axiom) {

			return axiom.getSub();
		}

		InputComplexSuper getSuper(InputClassSubComplexSuper axiom) {

			return axiom.getSuper();
		}

		Iterable<? extends NodeX> getTypeNodes() {

			return names.getClassNodes();
		}

		private AllRelation toAllRelation(InputNodePropertyRange ax) {

			return new AllRelation(ax.getProperty(), new NodeValue(ax.getRange()));
		}
	}

	private class IndividualNodesProfileCreator
						extends
							TypeNodesProfileCreator<InputIndividualComplexType> {

		void processSourceAxioms(ProfilePatternsBuilder ppBuilder) {

			super.processSourceAxioms(ppBuilder);

			for (InputIndividualRelation ax : axioms.getIndividualRelations()) {

				ppBuilder.checkAddRelation(ax.getIndividual(), ax.getRelation());
			}
		}

		Iterable<InputIndividualComplexType> getSourceAxioms() {

			return axioms.getIndividualComplexTypes();
		}

		NodeX getSub(InputIndividualComplexType axiom) {

			return axiom.getIndividual();
		}

		InputComplexSuper getSuper(InputIndividualComplexType axiom) {

			return axiom.getType();
		}

		Iterable<? extends NodeX> getTypeNodes() {

			return names.getIndividualNodes();
		}
	}

	NodeProfilesBuilder(
		MatchStructures matchStructures,
		OntologyNames names,
		InputAxioms axioms,
		ComponentBuilder components) {

		this.matchStructures = matchStructures;
		this.names = names;
		this.axioms = axioms;
		this.components = components;

		new ClassNodesProfileCreator();
		new IndividualNodesProfileCreator();
	}
}

