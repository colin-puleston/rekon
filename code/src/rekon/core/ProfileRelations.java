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
	private Collection<Relation> profileRelations;

	private boolean expanded = false;
	private boolean newlyExpanded = false;
	private boolean expansionCheckRequired = false;

	private ExpansionStatus expansionStatus = ExpansionStatus.UNEXPANDED;

	private enum ExpansionStatus {UNEXPANDED, EXPANDED, CHECK}

	private class Collector extends RelationCollector {

		Collector() {

			super(profileRelations);
		}

		void onAdditions() {

			profileRelations = getCollected();
		}
	}

	ProfileRelations(Pattern pattern) {

		this.pattern = pattern;

		profileRelations = getDirectRelations();
	}

	void initExpansion(boolean firstPass) {

		expansionCheckRequired = true;

		if (firstPass) {

			expanded = false;
		}

		newlyExpanded = false;
	}

	void checkExpansion(ProfileRelationsExpander expander, boolean topLevel) {

		if (expansionCheckRequired) {

			if (expander.checkExpand(this)) {

				expanded = true;
				newlyExpanded = true;

				if (topLevel || !expander.incompleteTraversal()) {

					expansionCheckRequired = false;
				}
			}
			else {

				expansionCheckRequired = false;
			}
		}
	}

	boolean anyRelations() {

		return !profileRelations.isEmpty();
	}

	boolean expanded() {

		return expanded;
	}

	boolean newlyExpanded() {

		return newlyExpanded;
	}

	NodeX getNode() {

		return pattern.getSingleNode();
	}

	Collection<Relation> getDirectRelations() {

		return pattern.getDirectRelations();
	}

	Collection<Relation> getAll() {

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

	RelationCollector createCollector() {

		return new Collector();
	}
}
