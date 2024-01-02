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

	private Names disjuncts;
	private boolean definition = false;

	private class MatcherSubsumptionTester extends NodeMatcherVisitor {

		boolean subsumption = false;

		MatcherSubsumptionTester(NodeMatcher test) {

			test.acceptVisitor(this);
		}

		void visit(PatternMatcher test) {

			subsumption = subsumesPatternMatcher(test);
		}

		void visit(DisjunctionMatcher test) {

			subsumption = subsumes(test);
		}
	}

	public String toString() {

		return getClass().getSimpleName() + "(" + getDisjunctLabelsList() + ")";
	}

	DisjunctionMatcher(NodeX node, Collection<? extends NodeX> disjuncts) {

		super(node);

		this.disjuncts = new NameList(disjuncts);
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

	Names getDisjuncts() {

		return disjuncts;
	}

	Names getDirectlyImpliedSubNodes() {

		return disjuncts;
	}

	boolean definition() {

		return definition;
	}

	boolean subsumesNode(NodeX n) {

		for (Name d : disjuncts) {

			if (d.subsumes(n)) {

				return true;
			}
		}

		return false;
	}

	boolean subsumes(NodeMatcher test) {

		return new MatcherSubsumptionTester(test).subsumption;
	}

	boolean subsumes(DisjunctionMatcher test) {

		return test.allDisjunctsSubsumedByAny(expandDisjuncts(), new NameSet());
	}

	boolean classifiable(boolean initialPass) {

		for (Name d : disjuncts) {

			if (((NodeX)d).classifiableDisjunct(initialPass)) {

				return true;
			}
		}

		return false;
	}

	void acceptVisitor(NodeMatcherVisitor visitor) {

		visitor.visit(this);
	}

	private void configureAsDefinition(NodeX node) {

		for (Name d : disjuncts) {

			if (!node.local()) {

				d.addSubsumer(node);
			}

			d.registerAsDefinitionRefed(MatchRole.DISJUNCT);
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

	private boolean subsumesPatternMatcher(PatternMatcher test) {

		for (Name d : disjuncts) {

			if (d.subsumes(test.getNode())) {

				return true;
			}

			PatternMatcher dm = ((NodeX)d).getProfilePatternMatcher();

			if (dm != null && dm.subsumes(test)) {

				return true;
			}
		}

		return false;
	}

	private Names expandDisjuncts() {

		Names expansions = new NameSet();

		collectDisjunctExpansions(expansions);

		return expansions;
	}

	private void collectDisjunctExpansions(Names expansions) {

		expansions.addAll(disjuncts);

		for (Name d : disjuncts) {

			for (DisjunctionMatcher m : ((NodeX)d).getAllDisjunctionMatchers()) {

				m.collectDisjunctExpansions(expansions);
			}
		}
	}

	private boolean allDisjunctsSubsumedByAny(Names subsumers, NameSet visited) {

		for (Name d : disjuncts) {

			if (!disjunctSubsumedByAny(subsumers, (NodeX)d, visited)) {

				return false;
			}
		}

		return true;
	}

	private boolean disjunctSubsumedByAny(Names subsumers, NodeX d, NameSet visited) {

		if (subsumers.anySubsumes(d)) {

			return true;
		}

		if (anyNestedDisjunctSubsumptions(subsumers, d, visited)) {

			return true;
		}

		for (Name s : d.getSubsumers()) {

			if (visited.add(getNode())) {

				if (anyNestedDisjunctSubsumptions(subsumers, (NodeX)s, visited)) {

					return true;
				}
			}
		}

		return false;
	}

	private boolean anyNestedDisjunctSubsumptions(
						Names subsumers,
						NodeX d,
						NameSet visited) {

		for (DisjunctionMatcher dm : d.getAllDisjunctionMatchers()) {

			if (dm.allDisjunctsSubsumedByAny(subsumers, visited)) {

				return true;
			}
		}

		return false;
	}

	private boolean anySubsumptions(Names subsumers, NodeX test) {

		for (Name s : subsumers) {

			if (s.subsumes(test)) {

				return true;
			}
		}

		return false;
	}

	private boolean patternMatcherSubsumedByAny(Names subsumers, PatternMatcher pm) {

		for (Name s : subsumers) {

			for (NodeMatcher dm : ((NodeX)s).getAllDefinitionMatchers()) {

				if (dm.subsumes(pm)) {

					return true;
				}
			}
		}

		return false;
	}

	private String getDisjunctLabelsList() {

		String s = new String();

		for (Name d : disjuncts) {

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
