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
class ProfileRelationCollector {

	private ProfileRelationsExpander expander;

	private Set<Relation> collectorSet;
	private boolean anyAdditions = false;

	ProfileRelationCollector(ProfileRelationsExpander expander) {

		this(expander, new HashSet<Relation>());
	}

	ProfileRelationCollector(
		ProfileRelationsExpander expander,
		Set<Relation> initialCollectorSet) {

		this.expander = expander;

		collectorSet = initialCollectorSet;
	}

	Set<Relation> collectFromName(NodeX node) {

		if (collectFromRelations(node)) {

			collectFromSubsumers(node);
		}

		return collectorSet;
	}

	void collectFromSubsumers(NodeX node) {

		for (NodeX s : node.getSubsumers().asNodes()) {

			collectFromRelations(s);
		}
	}

	void collectFromRelationExpansions(Collection<Relation> relations) {

		for (Relation r : relations) {

			for (Relation sr : r.getExpansions(expander)) {

				checkAdd(sr);
			}
		}
	}

	void collectAll(Collection<Relation> relations) {

		for (Relation r : relations) {

			checkAdd(r);
		}
	}

	boolean anyAdditions() {

		return anyAdditions;
	}

	Set<Relation> getCollectorSet() {

		return collectorSet;
	}

	Set<Relation> ensureUpdatableCollectorSet() {

		return collectorSet;
	}

	private boolean collectFromRelations(NodeX node) {

		PatternMatcher p = node.getProfilePatternMatcher();

		if (p != null) {

			if (!expander.startVisit(node)) {

				return false;
			}

			for (Relation r : eusureExpandedProfileRelations(p)) {

				checkAdd(r);
			}
		}

		return true;
	}

	private void checkAdd(Relation r) {

		if (collectorSet.contains(r)) {

			return;
		}

		for (Relation cr : new ArrayList<Relation>(collectorSet)) {

			if (r.subsumes(cr)) {

				return;
			}

			if (cr.subsumes(r)) {

				resolveCollectorSet().remove(cr);
			}
		}

		resolveCollectorSet().add(r);
		anyAdditions |= true;
	}

	private Set<Relation> resolveCollectorSet() {

		collectorSet = ensureUpdatableCollectorSet();

		return collectorSet;
	}

	private Collection<Relation> eusureExpandedProfileRelations(PatternMatcher p) {

		return p.getPattern().getProfileRelations().ensureExpansions(expander);
	}
}
