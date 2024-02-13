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

	private ComponentBuilder components;

	private class ProfilePatternsBuilder {

		private Map<NodeX, List<Relation>> relationsByNode
							= new HashMap<NodeX, List<Relation>>();

		void checkAddRelation(InputAxiom axiom, NodeX node, InputRelation relSource) {

			Relation rel = components.toRelation(relSource);

			if (rel != null) {

				resolveRelations(node).add(rel);
			}
			else {

				axiom.notifyAxiomOutOfScope();
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

	NodeProfilesBuilder(
		MatchStructures matchStructures,
		OntologyNames names,
		InputAxioms axioms,
		ComponentBuilder components) {

		super(matchStructures);

		this.components = components;

		createClassProfiles(names, axioms);
		createIndividualProfiles(names, axioms);
	}

	private void createClassProfiles(OntologyNames names, InputAxioms axioms) {

		ProfilePatternsBuilder ppBuilders = new ProfilePatternsBuilder();

		for (InputClassSubComplexSuper ax : axioms.getClassSubComplexSupers()) {

			ClassNode sub = ax.getSub();
			InputComplexSuper sup = ax.getSuper();

			switch (sup.getComplexSuperType()) {

				case DISJUNCTION:

					checkCreateDisjunctionProfile(ax, sub, sup.asDisjuncts());
					break;

				case RELATION:

					ppBuilders.checkAddRelation(ax, sub, sup.asRelation());
					break;

				case OUT_OF_SCOPE:

					sup.notifyComponentOutOfScope();
					ax.notifyAxiomOutOfScope();
					break;

				default:

					throw new Error("Unexpected complex-type: " + sup.getComplexSuperType());
			}
		}

		ppBuilders.createAllProfiles(names.getClassNodes());
	}

	private void createIndividualProfiles(OntologyNames names, InputAxioms axioms) {

		ProfilePatternsBuilder ppBuilders = new ProfilePatternsBuilder();

		for (InputIndividualRelation ax : axioms.getIndividualRelations()) {

			ppBuilders.checkAddRelation(ax, ax.getIndividual(), ax.getRelation());
		}

		ppBuilders.createAllProfiles(names.getIndividualNodes());
	}

	private void checkCreateDisjunctionProfile(
						InputClassSubComplexSuper ax,
						ClassNode sub,
						Collection<InputNode> supDisjuncts) {

		List<Pattern> djs = components.toPatternDisjunction(supDisjuncts);

		if (djs != null) {

			addProfileDisjunction(sub, djs);
		}
		else {

			ax.notifyAxiomOutOfScope();
		}
	}
}
