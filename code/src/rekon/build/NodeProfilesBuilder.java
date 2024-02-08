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

		void checkAddRelation(InputAxiom axiom, NodeX node, InputComplex relSource) {

			checkAddRelation(axiom, node, components.toRelation(relSource));
		}

		void checkAddRelation(InputAxiom axiom, NodeX node, InputRelation relSource) {

			checkAddRelation(axiom, node, components.toRelation(relSource));
		}

		void createAllProfiles(Collection<? extends NodeX> nodes) {

			for (NodeX n : nodes) {

				checkAddProfilePattern(n, getRelations(n));
			}
		}

		private void checkAddRelation(InputAxiom axiom, NodeX node, Relation rel) {

			if (rel != null) {

				resolveRelations(node).add(rel);
			}
			else {

				axiom.notifyAxiomOutOfScope();
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

			InputComplex sup = ComplexSuperConverter.toComplex(ax.getSuper());

			if (sup.hasComplexType(InputComplexType.DISJUNCTION)) {

				checkCreateDisjunctionProfile(ax, ax.getSub(), sup);
			}
			else {

				ppBuilders.checkAddRelation(ax, ax.getSub(), sup);
			}
		}

		ppBuilders.createAllProfiles(names.getClassNodes());
	}

	private void createIndividualProfiles(OntologyNames names, InputAxioms axioms) {

		ProfilePatternsBuilder ppBuilders = new ProfilePatternsBuilder();

		for (InputIndividualComplexType ax : axioms.getIndividualComplexTypes()) {

			ppBuilders.checkAddRelation(ax, ax.getIndividual(), ax.getComplexType());
		}

		for (InputIndividualRelation ax : axioms.getIndividualRelations()) {

			ppBuilders.checkAddRelation(ax, ax.getIndividual(), ax.getRelation());
		}

		ppBuilders.createAllProfiles(names.getIndividualNodes());
	}

	private void checkCreateDisjunctionProfile(
						InputClassSubComplexSuper ax,
						ClassNode sub,
						InputComplex supDisjunction) {

		List<Pattern> djs = components.toPatternDisjunction(supDisjunction);

		if (djs != null) {

			addProfileDisjunction(sub, djs);
		}
		else {

			ax.notifyAxiomOutOfScope();
		}
	}
}
