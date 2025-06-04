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
class SimpleProfilesBuilder {

	private MatchStructures matchStructures;
	private OntologyNames names;
	private InputAxioms axioms;
	private ComponentBuilder components;

	private class ProfilesBuilder {

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

				matchStructures.checkAddProfile(n, getRelations(n));
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

		final ProfilesBuilder ppBuilder = new ProfilesBuilder();

		TypeNodesProfileCreator() {

			processSourceAxioms();

			ppBuilder.createAllProfiles(getTypeNodes());
		}

		void processSourceAxioms() {

			for (A ax : getSourceAxioms()) {

				NodeX sub = getSubOrNull(ax);

				if (sub != null) {

					checkAddRelations(sub, getSuper(ax));
				}
			}
		}

		abstract Iterable<A> getSourceAxioms();

		abstract NodeX getSubOrNull(A axiom);

		abstract InputNode getSuper(A axiom);

		abstract Iterable<? extends NodeX> getTypeNodes();

		private void checkAddRelations(NodeX sub, InputNode sup) {

			switch (sup.getNodeType()) {

				case CONJUNCTION:

					for (InputNode supConj : sup.asConjuncts()) {

						checkAddRelations(sub, supConj);
					}
					break;

				case RELATION:

					ppBuilder.checkAddRelation(sub, sup.asRelation());
					break;
			}
		}
	}

	private class ClassNodesProfileCreator
						extends
							TypeNodesProfileCreator<InputClassSubSuper> {

		void processSourceAxioms() {

			super.processSourceAxioms();

			ClassNode root = names.getRootClassNode();

			for (InputNodePropertyRange ax : axioms.getNodePropertyRanges()) {

				ppBuilder.addRelation(root, toAllRelation(ax));
			}
		}

		Iterable<InputClassSubSuper> getSourceAxioms() {

			return axioms.getClassSubSupers();
		}

		NodeX getSubOrNull(InputClassSubSuper axiom) {

			InputNode s = axiom.getSub();

			return s.getNodeType() == InputNodeType.CLASS ? s.asClassNode() : null;
		}

		InputNode getSuper(InputClassSubSuper axiom) {

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
							TypeNodesProfileCreator<InputIndividualType> {

		void processSourceAxioms() {

			super.processSourceAxioms();

			for (InputIndividualRelation ax : axioms.getIndividualRelations()) {

				ppBuilder.checkAddRelation(ax.getIndividual(), ax.getRelation());
			}
		}

		Iterable<InputIndividualType> getSourceAxioms() {

			return axioms.getIndividualTypes();
		}

		NodeX getSubOrNull(InputIndividualType axiom) {

			return axiom.getIndividual();
		}

		InputNode getSuper(InputIndividualType axiom) {

			return axiom.getType();
		}

		Iterable<? extends NodeX> getTypeNodes() {

			return names.getIndividualNodes();
		}
	}

	SimpleProfilesBuilder(
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

