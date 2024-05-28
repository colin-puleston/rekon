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
class ProfileRelations {

	private Pattern pattern;
	private Set<Relation> profileRelations;

	private ExpansionStatus expansionStatus = ExpansionStatus.UNEXPANDED;

	private enum ExpansionStatus {UNEXPANDED, EXPANDED, CHECK}

	private class ExpansionChecker {

		private NodeVisitMonitor visitMonitor;

		private Set<Relation> inputRelations;
		private Set<Relation> disjunctionDeivedRelations = new HashSet<Relation>();

		private class Expander extends ProfileRelationCollector {

			Expander() {

				super(resolveVisitMonitor(), profileRelations);

				collectDisjunctionDerivedRelations(disjunctionDeivedRelations);
			}

			Set<Relation> ensureCollectorSetUpdatable() {

				Set<Relation> dirRels = getDirectRelations();

				if (profileRelations == dirRels) {

					profileRelations = new HashSet<Relation>(dirRels);
				}

				return profileRelations;
			}
		}

		ExpansionChecker() {

			this(null);
		}

		ExpansionChecker(NodeVisitMonitor visitMonitor) {

			this.visitMonitor = visitMonitor;

			findDisjunctionDerivedRelations();

			inputRelations = resolveInputRelations();
		}

		boolean checkExpand() {

			if (inputRelations.isEmpty()) {

				return false;
			}

			boolean newSubsumers = anyLastPhaseInferredSubsumers();
			boolean expandableInputRels = anyExpandableInputRelations();

			if (newSubsumers || expandableInputRels || anyDisjunctionDeivedRelations()) {

				Expander e = new Expander();

				if (newSubsumers) {

					e.collectFromSubsumers(getNode());
				}

				if (expandableInputRels) {

					e.collectFromRelationExpansions(inputRelations);
				}

				return e.anyAdditions();
			}

			return false;
		}

		private void findDisjunctionDerivedRelations() {

			for (DisjunctionMatcher d : getNode().getAllDisjunctionMatchers()) {

				disjunctionDeivedRelations.addAll(deriveDisjunctionRelations(d));
			}
		}

		private Collection<Relation> deriveDisjunctionRelations(DisjunctionMatcher d) {

			return new DisjunctionDerivedRelations(d).getAll();
		}

		private Set<Relation> resolveInputRelations() {

			Set<Relation> dirRels = getDirectRelations();

			if (disjunctionDeivedRelations.isEmpty()) {

				return dirRels;
			}

			Set<Relation> inputRels = new HashSet<Relation>();

			inputRels.addAll(dirRels);
			inputRels.addAll(disjunctionDeivedRelations);

			return inputRels;
		}

		private NodeVisitMonitor resolveVisitMonitor() {

			return visitMonitor != null ? visitMonitor : new NodeVisitMonitor(getNode());
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

		private boolean anyDisjunctionDeivedRelations() {

			return !disjunctionDeivedRelations.isEmpty();
		}
	}

	ProfileRelations(Pattern pattern) {

		this.pattern = pattern;

		profileRelations = getDirectRelations();
	}

	void initExpansion() {

		expansionStatus = ExpansionStatus.CHECK;
	}

	void processExpansion() {

		if (expansionStatus == ExpansionStatus.CHECK) {

			boolean exp = new ExpansionChecker().checkExpand();

			expansionStatus = exp ? ExpansionStatus.EXPANDED : ExpansionStatus.UNEXPANDED;
		}
	}

	void checkExpandLocal() {

		initExpansion();
		processExpansion();
	}

	boolean anyRelations() {

		return !profileRelations.isEmpty();
	}

	boolean expanded() {

		return expansionStatus == ExpansionStatus.EXPANDED;
	}

	Collection<Relation> getAll() {

		return profileRelations;
	}

	Collection<Relation> ensureExpansions(NodeVisitMonitor visitMonitor) {

		if (expansionStatus == ExpansionStatus.CHECK) {

			Set<Relation> preProfileRels = new HashSet<Relation>(profileRelations);

			if (new ExpansionChecker(visitMonitor).checkExpand()) {

				if (visitMonitor.incompleteTraversal()) {

					Set<Relation> postProfileRels = new HashSet<Relation>(profileRelations);

					profileRelations = preProfileRels;

					return postProfileRels;
				}

				expansionStatus = ExpansionStatus.EXPANDED;
			}
		}

		return profileRelations;
	}

	boolean anySubsumedBy(Relation r) {

		for (Relation sr : profileRelations) {

			if (r.subsumes(sr)) {

				return true;
			}
		}

		return false;
	}

	private NodeX getNode() {

		return pattern.getSingleNode();
	}

	private Set<Relation> getDirectRelations() {

		return pattern.getDirectRelations();
	}
}
