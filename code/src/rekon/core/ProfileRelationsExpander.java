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

	static private class AllExpander {

		private Ontology ontology;
		private List<PatternMatcher> potentialProfiles = new ArrayList<PatternMatcher>();

		AllExpander(Ontology ontology) {

			this.ontology = ontology;

			initCurrentProfiles();
			initPotentialProfiles();

			processProfiles(ontology.getProfilePatterns());
			processProfiles(potentialProfiles);

			resolvePotentialProfiles();
		}

		private void initCurrentProfiles() {

			for (PatternMatcher p : ontology.getProfilePatterns()) {

				getProfileRelations(p).initExpansion();
			}
		}

		private void initPotentialProfiles() {

			for (DisjunctionMatcher d : ontology.getAllDisjunctions()) {

				NodeX n = d.getNode();

				if (n.getProfilePatternMatcher() == null) {

					PatternMatcher p = n.addProfilePatternMatcher();

					potentialProfiles.add(p);
					getProfileRelations(p).initExpansion();
				}
			}
		}

		private void processProfiles(List<PatternMatcher> patterns) {

			for (PatternMatcher p : patterns) {

				getProfileRelations(p).processExpansion(ontology);
			}
		}

		private void resolvePotentialProfiles() {

			for (PatternMatcher p : potentialProfiles) {

				ProfileRelations prs = getProfileRelations(p);

				if (prs.anyRelations()) {

					ontology.addDerivedProfilePattern(p);
				}
				else {

					p.getNode().removeProfilePatternMatcher();
				}
			}

			potentialProfiles.clear();
		}

		private ProfileRelations getProfileRelations(PatternMatcher p) {

			return p.getPattern().getProfileRelations();
		}
	}

	static void expandAll(Ontology ontology) {

		new AllExpander(ontology);
	}

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

	ProfileRelationsExpander(NodeX initialNode) {

		this(null, initialNode);
	}

	ProfileRelationsExpander(Ontology ontology, NodeX initialNode) {

		this.ontology = ontology;

		visited.add(initialNode);
	}

	boolean checkExpand(ProfileRelations profileRelations) {

		return new ExpansionChecker(profileRelations).checkExpand();
	}

	boolean startVisit(NodeX node) {

		if (visited.add(node)) {

			return true;
		}

		incompleteTraversal = true;

		return false;
	}

	boolean incompleteTraversal() {

		return incompleteTraversal;
	}

	private boolean localExpansion() {

		return ontology == null;
	}
}

