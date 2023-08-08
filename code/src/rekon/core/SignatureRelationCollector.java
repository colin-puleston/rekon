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
class SignatureRelationCollector {

	private NodeVisitMonitor visitMonitor;

	private Set<Relation> collected;
	private boolean additions = false;

	SignatureRelationCollector(NodeVisitMonitor visitMonitor) {

		this.visitMonitor = visitMonitor;

		collected = getInitialCollected();
	}

	Set<Relation> collectFromName(GNode node) {

		if (collectFromRelations(node)) {

			collectFromSubsumers(node);
		}

		return collected;
	}

	void collectFromSubsumers(GNode node) {

		for (Name n : node.getSubsumers().getNames()) {

			collectFromRelations((GNode)n);
		}
	}

	void collectFromRelationExpansions(Set<Relation> relations) {

		for (Relation r : relations) {

			for (Relation sr : r.getSignatureExpansions(visitMonitor)) {

				checkAdd(sr);
			}
		}
	}

	boolean additions() {

		return additions;
	}

	Set<Relation> getInitialCollected() {

		return new HashSet<Relation>();
	}

	Set<Relation> getCollected() {

		return collected;
	}

	Set<Relation> ensureUpdatable(Set<Relation> collected) {

		return collected;
	}

	private boolean collectFromRelations(GNode node) {

		if (visitMonitor.startVisit(node)) {

			PatternMatcher pp = node.getProfilePatternMatcher();

			if (pp != null) {

				Pattern p = pp.getPattern();

				for (Relation r : p.resolveSignatureRelations(visitMonitor)) {

					checkAdd(r);
				}
			}

			return true;
		}

		return false;
	}

	private void checkAdd(Relation r) {

		for (Relation cr : new ArrayList<Relation>(collected)) {

			if (r.subsumes(cr)) {

				return;
			}

			if (cr.subsumes(r)) {

				ensureUpdatable().remove(cr);
			}
		}

		ensureUpdatable().add(r);
		additions |= true;
	}

	private Set<Relation> ensureUpdatable() {

		collected = ensureUpdatable(collected);

		return collected;
	}
}
