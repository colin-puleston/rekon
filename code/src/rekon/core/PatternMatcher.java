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
class PatternMatcher extends NodeMatcher {

	private Pattern pattern;

	private class SubsumedTester extends NodeMatcherVisitor {

		boolean subsumption = false;

		SubsumedTester(NodeMatcher test) {

			test.acceptVisitor(this);
		}

		void visit(PatternMatcher test) {

			subsumption = subsumes(test);
		}

		void visit(DisjunctionMatcher test) {

			subsumption = subsumesDisjunctionMatcher(test);
		}
	}

	public String toString() {

		return getClass().getSimpleName() + "(" + getNode().getLabel() + ")";
	}

	PatternMatcher(NodeX node, Pattern pattern) {

		super(node);

		this.pattern = pattern;
	}

	void absorbDefinitionIntoProfile(Pattern defn) {

		pattern = pattern.combineWith(defn);
	}

	void addRelation(Relation relation) {

		pattern = pattern.extend(relation);
	}

	void checkExpandProfile() {

		pattern.checkExpandProfile();
	}

	void setProfileExpansionCheckRequired() {

		pattern.setProfileExpansionCheckRequired();
	}

	boolean updateForProfileExpansion() {

		return pattern.updateForProfileExpansion();
	}

	Pattern getPattern() {

		return pattern;
	}

	Names getDirectlyImpliedSubNodes() {

		return Names.NO_NAMES;
	}

	boolean subsumes(NodeMatcher test) {

		return new SubsumedTester(test).subsumption;
	}

	boolean subsumes(PatternMatcher test) {

		return pattern.subsumes(test.pattern);
	}

	boolean disjunctSubsumes(NodeX test) {

		return false;
	}

	boolean hasExpandedDisjunct(NodeX test) {

		return false;
	}

	void acceptVisitor(NodeMatcherVisitor visitor) {

		visitor.visit(this);
	}

	private boolean subsumesDisjunctionMatcher(DisjunctionMatcher test) {

		for (Name d : test.getExpandedDisjuncts()) {

			if (!getNode().subsumes(d)) {

				return false;
			}
		}

		return true;
	}
}
