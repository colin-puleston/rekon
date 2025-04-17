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
class DisjunctionMatcher extends NodeMatcher {

	private NameSet disjuncts;
	private boolean definition = false;

	public String toString() {

		return toString(getClass().getSimpleName());
	}

	DisjunctionMatcher(NodeX node, Collection<? extends NodeX> rawDisjuncts) {

		super(node);

		disjuncts = new NameSet();

		disjuncts.retainMostGeneral(new NameList(rawDisjuncts));

		if (disjuncts.size() == 1) {

			node.addSubsumer(disjuncts.getFirstName());
		}
	}

	void ensureDefinition() {

		if (!definition) {

			definition = true;

			configureAsDefinition(getNode());
		}
	}

	void inferNewCommonDisjunctSubsumers() {

		getClassifier().checkAddInferredSubsumers(findCommonDisjunctSubsumers());
	}

	void checkExpandLocalProfile() {
	}

	Names getDisjuncts() {

		return disjuncts;
	}

	Names getDirectlyImpliedSubNodes() {

		return disjuncts;
	}

	boolean definition() {

		return definition;
	}

	boolean subsumes(DisjunctionMatcher test) {

		for (NodeX d : test.disjuncts.asNodes()) {

			if (!disjuncts.anySubsumes(d)) {

				return false;
			}
		}

		return true;
	}

	boolean subsumesNodeDirectly(NodeX test) {

		for (NodeX d : disjuncts.asNodes()) {

			if (d.subsumes(test)) {

				return true;
			}
		}

		return false;
	}

	boolean subsumedBy(PatternMatcher test) {

		if (test.getNode().local()) {

			return false;
		}

		for (NodeX d : disjuncts.asNodes()) {

			if (!d.subsumedByMatcher(test)) {

				return false;
			}
		}

		return true;
	}

	boolean subsumedBy(DisjunctionMatcher test) {

		return test.subsumes(this);
	}

	boolean hasDisjunct(NodeX test) {

		return disjuncts.contains(test);
	}

	boolean matchable(boolean initialPass) {

		return unprocessedSubsumers(initialPass, NodeSelector.MATCHABLE_DISJUNCT);
	}

	boolean unprocessedSubsumers(boolean initialPass) {

		return unprocessedSubsumers(initialPass, NodeSelector.ANY);
	}

	void acceptVisitor(NodeMatcherVisitor visitor) {

		visitor.visit(this);
	}

	void render(PatternRenderer r) {

		r.addLine(toString("OR"));
	}

	private void configureAsDefinition(NodeX node) {

		for (Name d : disjuncts) {

			if (!node.local()) {

				d.addSubsumer(node);
				d.registerAsDefinitionRefed(MatchRole.DISJUNCT);
			}
		}
	}

	private Names findCommonDisjunctSubsumers() {

		List<Collection<Name>> subsSets = new ArrayList<Collection<Name>>();

		for (Name d : disjuncts) {

			subsSets.add(d.getSubsumers().getNames());
		}

		NameSet ss = new NameSet(NameSetIntersector.intersect(subsSets));

		ss.remove(getNode());

		return ss;
	}

	private boolean unprocessedSubsumers(boolean initialPass, NodeSelector selector) {

		for (NodeX d : disjuncts.asNodes()) {

			if (d.unprocessedSubsumers(initialPass, selector)) {

				return true;
			}
		}

		return false;
	}

	private String toString(String prefix) {

		return prefix + "(" + getDisjunctLabelsList() + ")";
	}

	private String getDisjunctLabelsList() {

		StringBuilder s = new StringBuilder();
		boolean first = true;

		for (Name d : disjuncts) {

			if (first) {

				first = false;
			}
			else {

				s.append(',');
			}

			s.append(d.getLabel());
		}

		return s.toString();
	}

	private NodeClassifier getClassifier() {

		return getNode().getNodeClassifier();
	}
}
