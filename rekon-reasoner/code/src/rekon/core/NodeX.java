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
public abstract class NodeX extends Name {

	static private final List<PatternMatcher> NO_MATCHERS = Collections.emptyList();

	private List<PatternMatcher> matchers = NO_MATCHERS;

	public boolean subsumes(Name name) {

		if (name == this) {

			return true;
		}

		if (name instanceof NodeX) {

			if (local() ) {

				return subsumesNodeOrSubsumerViaMatcher((NodeX)name);
			}

			return super.subsumes(name);
		}

		return false;
	}

	PatternMatcher addProfile() {

		return addProfile(new Pattern(this));
	}

	PatternMatcher addProfile(Pattern profile) {

		checkProfileDoesNotExist();

		return addMatcher(profile);
	}

	PatternMatcher addDefinitionMatcher(Pattern defn) {

		checkProfileExists();

		defn.registerDefinitionRefedNames();

		return addMatcher(defn);
	}

	boolean ensurePatternProfiledImpliesPatternDefined() {

		if (matchers.size() == 1) {

			addDefinitionMatcher(getProfileMatcher().getPattern());

			return true;
		}

		return false;
	}

	void clearMatchers() {

		matchers = NO_MATCHERS;
	}

	boolean matchable() {

		return !matchers.isEmpty();
	}

	List<PatternMatcher> getAllMatchers() {

		return matchers;
	}

	PatternMatcher getProfileMatcher() {

		return matchers.size() == 0 ? null : matchers.get(0);
	}

	List<PatternMatcher> getProfileMatcherAsList() {

		PatternMatcher pm = getProfileMatcher();

		return pm != null ? Collections.singletonList(pm) : Collections.emptyList();
	}

	List<PatternMatcher> getDefinitionMatchers() {

		if (matchers.size() > 1) {

			return matchers.subList(1, matchers.size());
		}

		return Collections.emptyList();
	}

	boolean anyMatches(NodeSelector selector) {

		return selector.select(this) || selector.anyMatches(getSubsumers());
	}

	NodeClassifier getNodeClassifier() {

		return (NodeClassifier)getClassifier();
	}

	NameClassifier createClassifier() {

		return new NodeClassifier(this);
	}

	private PatternMatcher addMatcher(Pattern pattern) {

		return addMatcher(pattern, matchers.size());
	}

	private PatternMatcher addMatcher(Pattern pattern, int index) {

		PatternMatcher m = new PatternMatcher(this, pattern);

		if (matchers == NO_MATCHERS) {

			matchers = new ArrayList<PatternMatcher>();
		}

		matchers.add(index, m);

		return m;
	}

	private void checkProfileExists() {

		if (matchers.size() == 0) {

			throw new Error("No profile for node: " + this);
		}
	}

	private void checkProfileDoesNotExist() {

		if (matchers.size() != 0) {

			throw new Error("Profile already exists for node: " + this);
		}
	}

	private boolean subsumesNodeOrSubsumerViaMatcher(NodeX node) {

		if (subsumesViaMatcher(node)) {

			return true;
		}

		for (NodeX s : node.getSubsumers().asNodes()) {

			if (subsumesViaMatcher(s)) {

				return true;
			}
		}

		return false;
	}

	private boolean subsumesViaMatcher(NodeX node) {

		for (PatternMatcher d : getDefinitionMatchers()) {

			for (PatternMatcher p : node.getProfileMatcherAsList()) {

				if (d.subsumes(p)) {

					return true;
				}
			}
		}

		return false;
	}
}
