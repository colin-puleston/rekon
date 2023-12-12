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

		return test instanceof DisjunctionMatcher && subsumes((DisjunctionMatcher)test);
	}

	boolean subsumes(DisjunctionMatcher test) {

		for (Name d : test.disjuncts) {

			NodeX dn = (NodeX)d;

			if (!subsumesNode(dn) && !subsumesDisjunctionMatcher(dn)) {

				return false;
			}
		}

		return true;
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

	private boolean subsumesDisjunctionMatcher(NodeX node) {

		for (DisjunctionMatcher m : node.getAllDisjunctionMatchers()) {

			if (subsumes(m)) {

				return true;
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
