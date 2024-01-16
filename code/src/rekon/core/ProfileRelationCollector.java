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

	private NodeVisitMonitor visitMonitor;

	private Set<Relation> collectorSet;
	private boolean anyAdditions = false;

	ProfileRelationCollector(NodeVisitMonitor visitMonitor) {

		this(visitMonitor, new HashSet<Relation>());
	}

	ProfileRelationCollector(
		NodeVisitMonitor visitMonitor,
		Set<Relation> initialCollectorSet) {

		this.visitMonitor = visitMonitor;

		collectorSet = initialCollectorSet;
	}

	Set<Relation> collectFromName(NodeX node) {

		if (collectFromRelations(node)) {

			collectFromSubsumers(node);
		}

		return collectorSet;
	}

	void collectFromSubsumers(Names nodes) {

		for (NodeX n : nodes.asNodes()) {

			collectFromSubsumers(n);
		}
	}

	void collectFromRelationExpansions(Set<Relation> relations) {

		for (Relation r : relations) {

			for (Relation sr : r.getExpansions(visitMonitor)) {

				checkAdd(sr);
			}
		}
	}

	boolean anyAdditions() {

		return anyAdditions;
	}

	Set<Relation> getCollectorSet() {

		return collectorSet;
	}

	Set<Relation> ensureCollectorSetUpdatable() {

		return collectorSet;
	}

	private void collectFromSubsumers(NodeX node) {

		for (NodeX s : node.getSubsumers().asNodes()) {

			collectFromRelations(s);
		}
	}

	private boolean collectFromRelations(NodeX node) {

		if (visitMonitor.startVisit(node)) {

			PatternMatcher pp = node.getProfilePatternMatcher();

			if (pp != null) {

				Pattern p = pp.getPattern();

				for (Relation r : p.getProfileRelations().ensureExpansions(visitMonitor)) {

					checkAdd(r);
				}
			}

			return true;
		}

		return false;
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

		collectorSet = ensureCollectorSetUpdatable();

		return collectorSet;
	}
}
