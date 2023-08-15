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

	private ExpansionStatus expansionStatus = ExpansionStatus.NONE;

	private enum ExpansionStatus {NONE, PRE_EXPANDED, CHECK_EXPANSION}

	private class Expander extends ProfileRelationCollector {

		Expander(NodeVisitMonitor visitMonitor) {

			super(visitMonitor, profileRelations);
		}

		Set<Relation> ensureCollectorSetUpdatable() {

			if (profileRelations == getDirectRelations()) {

				profileRelations = new HashSet<Relation>(getDirectRelations());
			}

			return profileRelations;
		}
	}

	ProfileRelations(Pattern pattern) {

		this.pattern = pattern;

		profileRelations = getDirectRelations();
	}

	void setExpansionCheckRequired() {

		expansionStatus = ExpansionStatus.CHECK_EXPANSION;
	}

	boolean updateForExpansion() {

		boolean expanded = false;

		switch (expansionStatus) {

			case CHECK_EXPANSION:
				expanded = checkExpansion(null);
				break;

			case PRE_EXPANDED:
				expanded = true;
				break;

			case NONE:
				throw new Error("Should never happen!");
		}

		expansionStatus = ExpansionStatus.NONE;

		return expanded;
	}

	Collection<Relation> getAll() {

		return profileRelations;
	}

	Collection<Relation> ensureExpansions(NodeVisitMonitor visitMonitor) {

		if (expansionStatus == ExpansionStatus.CHECK_EXPANSION) {

			Set<Relation> preProfileRels = new HashSet<Relation>(profileRelations);

			if (checkExpansion(visitMonitor)) {

				if (visitMonitor.incompleteTraversal()) {

					Set<Relation> postProfileRels = new HashSet<Relation>(profileRelations);

					profileRelations = preProfileRels;

					return postProfileRels;
				}

				expansionStatus = ExpansionStatus.PRE_EXPANDED;
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

	private boolean checkExpansion(NodeVisitMonitor visitMonitor) {

		boolean newSubsumers = anyLastPhaseInferredSubsumers();
		boolean expandableRels = anyExpandableRelations();

		if (newSubsumers || expandableRels) {

			if (visitMonitor == null) {

				visitMonitor = new NodeVisitMonitor(getNodes());
			}

			Expander e = new Expander(visitMonitor);

			if (newSubsumers) {

				e.collectFromSubsumers(getNodes());
			}

			if (expandableRels) {

				e.collectFromRelationExpansions(getDirectRelations());
			}

			return e.anyAdditions();
		}

		return false;
	}

	private boolean anyLastPhaseInferredSubsumers() {

		for (Name n : getNodes().getNames()) {

			if (((NodeX)n).getNodeClassifier().anyLastPhaseInferredSubsumers()) {

				return true;
			}
		}

		return false;
	}

	private boolean anyExpandableRelations() {

		for (Relation r : getDirectRelations()) {

			if (r.expandableRelation()) {

				return true;
			}
		}

		return false;
	}

	private Names getNodes() {

		return pattern.getNodes();
	}

	private Set<Relation> getDirectRelations() {

		return pattern.getDirectRelations();
	}
}

