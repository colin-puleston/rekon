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

	public String toString() {

		return getClass().getSimpleName() + "(" + getDisjunctLabelsList() + ")";
	}

	DisjunctionMatcher(NodeName node, Collection<? extends NodeName> disjuncts) {

		super(node);

		this.disjuncts = new NameList(disjuncts);

		for (Name d : disjuncts) {

			if (!node.local()) {

				d.addSubsumer(node);
			}

			d.registerAsDefinitionRefed(MatchRole.DISJUNCT);
		}
	}

	void setPreInferredCommonDisjunctSubsumers() {

		getClassifier().addAndExpandPreInferredSubsumers(findCommonDisjunctSubsumers());
	}

	void setNewInferredCommonDisjunctSubsumers() {

		getClassifier().checkAddInferredSubsumers(findCommonDisjunctSubsumers());
	}

	Names getDisjuncts() {

		return disjuncts;
	}

	boolean subsumesNode(NodeName n) {

		for (Name d : disjuncts.getNames()) {

			if (d.subsumes(n)) {

				return true;
			}
		}

		return false;
	}

	boolean subsumesDisjunction(DisjunctionMatcher other) {

		for (Name d : other.disjuncts.getNames()) {

			if (!subsumesNode((NodeName)d)) {

				return false;
			}
		}

		return true;
	}

	boolean classifiable(boolean initialPass) {

		for (Name d : disjuncts.getNames()) {

			if (((NodeName)d).classifyTargetDisjunct(initialPass)) {

				return true;
			}
		}

		return false;
	}

	void acceptVisitor(NodeMatcherVisitor visitor) {

		visitor.visit(this);
	}

	private Names findCommonDisjunctSubsumers() {

		SetIntersector<Name> subsSets = new SetIntersector<Name>();

		for (Name d : disjuncts.getNames()) {

			subsSets.addSet(d.getSubsumers().getNames());
		}

		return new NameList(subsSets.intersectAll());
	}

	private String getDisjunctLabelsList() {

		String s = new String();

		for (Name d : disjuncts.getNames()) {

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
