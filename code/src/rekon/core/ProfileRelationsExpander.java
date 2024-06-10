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

package rekon.core;

import java.util.*;

/**
 * @author Colin Puleston
 */
class ProfileRelationsExpander {

	private Ontology ontology;

	private NameSet visited = new NameSet();
	private NameSet revisited = new NameSet();

	private class DisjunctionBasedDeriver extends DisjunctionBasedProfileRelationDeriver {

		ClassNode addDerivedValueDisjunction(Collection<NodeX> disjuncts) {

			if (localExpansion()) {

				throw new Error("Unexpected disjunction-value derivation!");
			}

			return ontology.addDerivedProfileValueDisjunction(disjuncts);
		}

		Collection<Relation> resolveRelationExpansions(NodeX node) {

			return resolveGeneralExpansions(node);
		}
	}

	private class ChainBasedDeriver extends ChainBasedProfileRelationsDeriver {

		ChainBasedDeriver(SomeRelation relation) {

			super(relation);
		}

		Collection<Relation> resolveRelationExpansions(NodeX node) {

			return resolveGeneralExpansions(node);
		}
	}

	private class ExpansionChecker {

		private NodeX node;
		private RelationCollector relationCollector;

		private Collection<Relation> inputRelations;
		private Collection<Relation> disjunctionBasedRelations;

		ExpansionChecker(ProfileRelations profileRelations) {

			this(
				profileRelations.getNode(),
				profileRelations.getDirectRelations(),
				profileRelations.createCollector());
		}

		ExpansionChecker(NodeX node) {

			this(node, Collections.emptySet(), new RelationCollector());
		}

		boolean checkExpand() {

			if (!disjunctionBasedRelations.isEmpty()) {

				relationCollector.checkAddAll(disjunctionBasedRelations);
			}

			if (anyLastPhaseInferredSubsumers()) {

				collectFromSubsumers(node);
			}

			if (anyChainExpandableInputRelations()) {

				collectFromChainBasedExpansions();
			}

			return relationCollector.anyAdditions();
		}

		Collection<Relation> getExpanded() {

			checkExpand();

			return relationCollector.getCollected();
		}

		private ExpansionChecker(
					NodeX node,
					Collection<Relation> directRelations,
					RelationCollector relationCollector) {

			this.node = node;
			this.relationCollector = relationCollector;

			disjunctionBasedRelations = deriveDisjunctionBasedRelations();
			inputRelations = resolveInputRelations(directRelations);
		}

		private void collectFromSubsumers(NodeX node) {

			for (NodeX s : node.getSubsumers().asNodes()) {

				PatternMatcher p = s.getProfilePatternMatcher();

				if (p != null) {

					for (Relation r : resolveExpansions(s, p)) {

						relationCollector.checkAdd(r);
					}
				}
			}
		}

		private void collectFromChainBasedExpansions() {

			for (Relation r : inputRelations) {

				for (Relation sr : getAllChainBasedExpansions(r)) {

					relationCollector.checkAdd(sr);
				}
			}
		}

		private Collection<Relation> deriveDisjunctionBasedRelations() {

			return localExpansion() ? Collections.emptyList() : deriveRelations();
		}

		private Collection<Relation> deriveRelations() {

			DisjunctionBasedDeriver deriver = new DisjunctionBasedDeriver();

			for (DisjunctionMatcher d : node.getAllDisjunctionMatchers()) {

				deriver.deriveFor(d);
			}

			return deriver.getAll();
		}

		private Collection<Relation> resolveInputRelations(
										Collection<Relation> directRelations) {

			if (disjunctionBasedRelations.isEmpty()) {

				return directRelations;
			}

			if (directRelations.isEmpty()) {

				return disjunctionBasedRelations;
			}

			Set<Relation> inputRels = new HashSet<Relation>();

			inputRels.addAll(directRelations);
			inputRels.addAll(disjunctionBasedRelations);

			return inputRels;
		}

		private boolean anyLastPhaseInferredSubsumers() {

			if (node.classified()) {

				return false;
			}

			return node.getNodeClassifier().anyLastPhaseInferredSubsumers();
		}

		private boolean anyChainExpandableInputRelations() {

			for (Relation r : inputRelations) {

				if (r.chainExpandable()) {

					return true;
				}
			}

			return false;
		}
	}

	ProfileRelationsExpander() {

		this(null);
	}

	ProfileRelationsExpander(Ontology ontology) {

		this.ontology = ontology;
	}

	boolean checkExpand(ProfileRelations profileRelations) {

		NodeX n = profileRelations.getNode();

		startVisit(n);

		if (revisiting(n)) {

			return false;
		}

		return new ExpansionChecker(profileRelations).checkExpand();
	}

	boolean incompleteExpansion() {

		return !revisited.isEmpty();
	}

	private Collection<Relation> getAllChainBasedExpansions(Relation relation) {

		if (relation instanceof SomeRelation) {

			SomeRelation sr = (SomeRelation)relation;

			if (sr.anyChains()) {

				return new ChainBasedDeriver(sr).getAllExpansions();
			}
		}

		return Collections.emptySet();
	}

	private Collection<Relation> resolveGeneralExpansions(NodeX node) {

		PatternMatcher p = node.getProfilePatternMatcher();

		if (p != null) {

			return resolveExpansions(node, p);
		}

		return new ExpansionChecker(node).getExpanded();
	}

	private Collection<Relation> resolveExpansions(NodeX node, PatternMatcher p) {

		ProfileRelations rels = p.getPattern().getProfileRelations();

		return revisiting(node) ? rels.getAll() : rels.ensureExpansions(this);
	}

	private void startVisit(NodeX node) {

		if (!visited.add(node)) {

			revisited.add(node);
		}
	}

	private boolean revisiting(NodeX node) {

		return revisited.contains(node);
	}

	private boolean localExpansion() {

		return ontology == null;
	}
}
