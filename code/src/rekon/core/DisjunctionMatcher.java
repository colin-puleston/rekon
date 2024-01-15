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

	private NameSet directDisjuncts;
	private boolean definition = false;

	private class GeneralSubsumedTester extends NodeMatcherVisitor {

		boolean subsumption = false;

		GeneralSubsumedTester(NodeMatcher test) {

			test.acceptVisitor(this);
		}

		void visit(PatternMatcher test) {

			if (getNode().local()) {

				subsumption = subsumesPatternMatcher(test);
			}
		}

		void visit(DisjunctionMatcher test) {

			subsumption = subsumes(test);
		}
	}

	private class SubsumedDisjunctionTester {

		final boolean subsumption;

		private Names testSubsumers = expandDisjuncts();
		private NameSet visited = new NameSet();

		SubsumedDisjunctionTester(DisjunctionMatcher test) {

			subsumption = allDisjunctsSubsumed(test);
		}

		private boolean allDisjunctsSubsumed(DisjunctionMatcher test) {

			for (Name d : test.directDisjuncts) {

				if (!disjunctSubsumed((NodeX)d)) {

					return false;
				}
			}

			return true;
		}

		private boolean disjunctSubsumed(NodeX d) {

			if (testSubsumers.anySubsumes(d)) {

				return true;
			}

			if (anyNestedDisjunctSubsumptions(d)) {

				return true;
			}

			for (Name s : d.getSubsumers()) {

				if (visited.add(getNode())) {

					if (anyNestedDisjunctSubsumptions((NodeX)s)) {

						return true;
					}
				}
			}

			return false;
		}

		private boolean anyNestedDisjunctSubsumptions(NodeX d) {

			for (DisjunctionMatcher dm : d.getAllDisjunctionMatchers()) {

				if (allDisjunctsSubsumed(dm)) {

					return true;
				}
			}

			return false;
		}
	}

	public String toString() {

		return getClass().getSimpleName() + "(" + getDisjunctLabelsList() + ")";
	}

	DisjunctionMatcher(NodeX node, Collection<? extends NodeX> disjuncts) {

		super(node);

		directDisjuncts = new NameSet(disjuncts);
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

	void checkExpandProfile() {
	}

	void setProfileExpansionCheckRequired() {
	}

	boolean updateForProfileExpansion() {

		return false;
	}

	Names getDirectDisjuncts() {

		return directDisjuncts;
	}

	Names getExpandedDisjuncts() {

		return expandDisjuncts();
	}

	Names getDirectlyImpliedSubNodes() {

		return expandDisjuncts();
	}

	boolean definition() {

		return definition;
	}

	boolean subsumes(NodeMatcher test) {

		return new GeneralSubsumedTester(test).subsumption;
	}

	boolean subsumes(DisjunctionMatcher test) {

		return new SubsumedDisjunctionTester(test).subsumption;
	}

	boolean disjunctSubsumes(NodeX test) {

		for (Name d : expandDisjuncts()) {

			if (d.subsumes(test)) {

				return true;
			}
		}

		return false;
	}

	boolean hasExpandedDisjunct(NodeX test) {

		return expandDisjuncts().contains(test);
	}

	boolean matchable(boolean initialPass) {

		return classifiable(initialPass, NodeSelector.MATCHABLE_DISJUNCT);
	}

	boolean classifiable(boolean initialPass) {

		return classifiable(initialPass, NodeSelector.ANY);
	}

	void acceptVisitor(NodeMatcherVisitor visitor) {

		visitor.visit(this);
	}

	private void configureAsDefinition(NodeX node) {

		for (Name d : directDisjuncts) {

			if (!node.local()) {

				d.addSubsumer(node);
			}

			d.registerAsDefinitionRefed(MatchRole.DISJUNCT);
		}
	}

	private Names findCommonDisjunctSubsumers() {

		List<Collection<Name>> subsSets = new ArrayList<Collection<Name>>();

		for (Name d : directDisjuncts) {

			subsSets.add(d.getSubsumers().getNames());
		}

		NameSet ss = new NameSet(NameSetIntersector.intersect(subsSets));

		ss.remove(getNode());

		return ss;
	}

	private boolean subsumesPatternMatcher(PatternMatcher test) {

		for (Name d : directDisjuncts) {

			if (((NodeX)d).subsumesMatcher(test)) {

				return true;
			}
		}

		return false;
	}

	private NameSet expandDisjuncts() {

		NameSet expansions = new NameSet();

		collectDisjunctExpansions(expansions);

		return expansions;
	}

	private void collectDisjunctExpansions(Names expansions) {

		expansions.addAll(directDisjuncts);

		for (Name d : directDisjuncts) {

			for (DisjunctionMatcher m : ((NodeX)d).getAllDisjunctionMatchers()) {

				m.collectDisjunctExpansions(expansions);
			}
		}
	}

	private boolean classifiable(boolean initialPass, NodeSelector selector) {

		for (Name d : expandDisjuncts()) {

			if (((NodeX)d).classifiable(initialPass, selector)) {

				return true;
			}
		}

		return false;
	}

	private String getDisjunctLabelsList() {

		String s = new String();

		for (Name d : directDisjuncts) {

			if (!s.isEmpty()) {

				s += ',';
			}

			s += d.getLabel();
		}

		return s;
	}

	private NodeClassifier getClassifier() {

		return getNode().getNodeClassifier();
	}
}
