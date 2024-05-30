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
	private boolean incompleteTraversal = false;

	private class DisjunctionBasedDeriver extends DisjunctionBasedProfileRelationDeriver {

		ClassNode addDerivedValueDisjunction(Collection<NodeX> disjuncts) {

			if (localExpansion()) {

				throw new Error("Unexpected disjunction-value derivation!");
			}

			return ontology.addDerivedProfileValueDisjunction(disjuncts);
		}

		Collection<Relation> ensureExpandedProfileRelations(PatternMatcher p) {

			return ensureExpansions(p.getPattern().getProfileRelations());
		}

		private Collection<Relation> ensureExpansions(ProfileRelations profRels) {

			return profRels.ensureExpansions(ProfileRelationsExpander.this);
		}
	}

	private class ChainBasedExpander extends ChainBasedProfileRelationsExpander {

		ChainBasedExpander(SomeRelation relation) {

			super(relation);
		}

		Set<Relation> getAllRelationsFromNode(NodeX node) {

			ProfileRelationCollector c = createCollector();

			c.collectFromName(node);

			return c.getCollectorSet();
		}

		private ProfileRelationCollector createCollector() {

			return new ProfileRelationCollector(ProfileRelationsExpander.this);
		}
	}

	private class ExpansionChecker {

		private ProfileRelations profileRelations;

		private Collection<Relation> inputRelations;
		private Collection<Relation> derivedRelations;

		ExpansionChecker(ProfileRelations profileRelations) {

			this.profileRelations = profileRelations;

			derivedRelations = getDerivedRelations();
			inputRelations = resolveInputRelations();
		}

		boolean checkExpand() {

			boolean newSubsumers = anyLastPhaseInferredSubsumers();
			boolean expandableInputRels = anyExpandableInputRelations();

			if (newSubsumers || expandableInputRels || !derivedRelations.isEmpty()) {

				ProfileRelationCollector c = createExpansionCollector();

				if (!derivedRelations.isEmpty()) {

					c.collectAll(derivedRelations);
				}

				if (newSubsumers) {

					c.collectFromSubsumers(getNode());
				}

				if (expandableInputRels) {

					c.collectFromRelationExpansions(inputRelations);
				}

				return c.anyAdditions();
			}

			return false;
		}

		private Collection<Relation> getDerivedRelations() {

			return localExpansion() ? Collections.emptyList() : deriveRelations();
		}

		private Collection<Relation> deriveRelations() {

			DisjunctionBasedDeriver deriver = new DisjunctionBasedDeriver();

			for (DisjunctionMatcher d : getNode().getAllDisjunctionMatchers()) {

				deriver.deriveFor(d);
			}

			return deriver.getAll();
		}

		private Collection<Relation> resolveInputRelations() {

			Set<Relation> dirRels = getDirectRelations();

			if (derivedRelations.isEmpty()) {

				return dirRels;
			}

			Set<Relation> inputRels = new HashSet<Relation>();

			inputRels.addAll(dirRels);
			inputRels.addAll(derivedRelations);

			return inputRels;
		}

		private boolean anyLastPhaseInferredSubsumers() {

			NodeX n = getNode();

			return !n.classified() && n.getNodeClassifier().anyLastPhaseInferredSubsumers();
		}

		private boolean anyExpandableInputRelations() {

			for (Relation r : inputRelations) {

				if (r.expandableRelation()) {

					return true;
				}
			}

			return false;
		}

		private NodeX getNode() {

			return profileRelations.getNode();
		}

		private Set<Relation> getDirectRelations() {

			return profileRelations.getDirectRelations();
		}

		private ProfileRelationCollector createExpansionCollector() {

			return profileRelations.createExpansionCollector(ProfileRelationsExpander.this);
		}
	}

	boolean incompleteTraversal() {

		return incompleteTraversal;
	}

	ProfileRelationsExpander() {

		this(null);
	}

	ProfileRelationsExpander(Ontology ontology) {

		this.ontology = ontology;
	}

	void processExpansion(ProfileRelations profileRelations) {

		visited.add(profileRelations.getNode());

		profileRelations.processExpansion(this);
	}

	boolean checkExpand(ProfileRelations profileRelations) {

		return new ExpansionChecker(profileRelations).checkExpand();
	}

	Collection<Relation> getAllExpansions(Relation relation) {

		if (relation instanceof SomeRelation) {

			SomeRelation sr = (SomeRelation)relation;

			if (sr.anyChains()) {

				return new ChainBasedExpander(sr).getAllExpansions();
			}
		}

		return Collections.emptySet();
	}

	boolean startVisit(NodeX node) {

		if (visited.add(node)) {

			return true;
		}

		incompleteTraversal = true;

		return false;
	}

	private boolean localExpansion() {

		return ontology == null;
	}
}
