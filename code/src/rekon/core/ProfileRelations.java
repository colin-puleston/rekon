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

	private class Collector extends RelationCollector {

		Collector() {

			super(profileRelations);
		}

		Set<Relation> ensureUpdatableCollectorSet() {

			Set<Relation> dirRels = getDirectRelations();

			if (profileRelations == dirRels) {

				profileRelations = new HashSet<Relation>(dirRels);
			}

			return profileRelations;
		}
	}

	ProfileRelations(Pattern pattern) {

		this.pattern = pattern;

		profileRelations = getDirectRelations();
	}

	void initExpansion() {

		expansionStatus = ExpansionStatus.CHECK;
	}

	void processExpansion(ProfileRelationsExpander expander) {

		if (expansionStatus == ExpansionStatus.CHECK) {

			boolean exp = expander.checkExpand(this);

			expansionStatus = exp ? ExpansionStatus.EXPANDED : ExpansionStatus.UNEXPANDED;
		}
	}

	boolean anyRelations() {

		return !profileRelations.isEmpty();
	}

	boolean expanded() {

		return expansionStatus == ExpansionStatus.EXPANDED;
	}

	NodeX getNode() {

		return pattern.getSingleNode();
	}

	Collection<Relation> getAll() {

		return profileRelations;
	}

	Set<Relation> getDirectRelations() {

		return pattern.getDirectRelations();
	}

	boolean anySubsumedBy(Relation r) {

		for (Relation sr : profileRelations) {

			if (r.subsumes(sr)) {

				return true;
			}
		}

		return false;
	}

	Collection<Relation> ensureExpansions(ProfileRelationsExpander expander) {

		if (expansionStatus == ExpansionStatus.CHECK) {

			Set<Relation> preProfileRels = new HashSet<Relation>(profileRelations);

			if (expander.checkExpand(this)) {

				if (expander.incompleteTraversal()) {

					Set<Relation> postProfileRels = new HashSet<Relation>(profileRelations);

					profileRelations = preProfileRels;

					return postProfileRels;
				}

				expansionStatus = ExpansionStatus.EXPANDED;
			}
		}

		return profileRelations;
	}

	RelationCollector createCollector() {

		return new Collector();
	}
}
