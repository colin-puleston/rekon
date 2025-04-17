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

	public String toString() {

		return getClass().getSimpleName() + "(" + getNode().getLabel() + ")";
	}

	PatternMatcher(NodeX node) {

		this(node, new Pattern(node));
	}

	PatternMatcher(NodeX node, Pattern pattern) {

		super(node);

		this.pattern = pattern;
	}

	Collection<Relation> absorbDefinitionIntoProfile(Pattern defn) {

		Collection<Relation> preRels = pattern.getDirectRelations();

		pattern = pattern.combineWith(defn);

		Set<Relation> newRels = new HashSet<Relation>(pattern.getDirectRelations());

		newRels.removeAll(preRels);

		return newRels;
	}

	void addRelation(Relation relation) {

		pattern = pattern.extend(relation);
	}

	void checkExpandLocalProfile() {

		ProfilePatternsExpander.checkExpandLocal(pattern);
	}

	Pattern getPattern() {

		return pattern;
	}

	Names getDirectlyImpliedSubNodes() {

		return Names.NO_NAMES;
	}

	boolean subsumes(PatternMatcher test) {

		return pattern.subsumes(test.pattern);
	}

	boolean subsumesNodeDirectly(NodeX test) {

		return false;
	}

	boolean subsumedBy(PatternMatcher test) {

		return test.subsumes(this);
	}

	boolean subsumedBy(DisjunctionMatcher test) {

		for (NodeX d : test.getDisjuncts().asNodes()) {

			if (d.subsumesMatcher(this)) {

				return true;
			}
		}

		return false;
	}

	boolean hasDisjunct(NodeX test) {

		return false;
	}

	void acceptVisitor(NodeMatcherVisitor visitor) {

		visitor.visit(this);
	}

	void render(PatternRenderer r) {

		pattern.render(r);
	}
}
