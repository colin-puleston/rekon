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

	static private final List<NodeMatcher> NO_MATCHERS = Collections.emptyList();

	private List<NodeMatcher> matchers = NO_MATCHERS;

	PatternMatcher addProfilePatternMatcher() {

		return addProfilePatternMatcher(new Pattern(this));
	}

	PatternMatcher addProfilePatternMatcher(Pattern profile) {

		return addPatternMatcher(profile, true);
	}

	PatternMatcher addDefinitionPatternMatcher(Pattern defn) {

		defn.registerDefinitionRefedNames();

		return addPatternMatcher(defn, false);
	}

	DisjunctionMatcher addDisjunctionMatcher(Collection<? extends NodeX> disjuncts) {

		return addMatcher(new DisjunctionMatcher(this, disjuncts));
	}

	boolean ensurePatternProfiledImpliesPatternDefined() {

		PatternMatcher prof = getProfilePatternMatcher();

		if (prof != null && getDefinitionPatternMatchers().isEmpty()) {

			matchers.add(prof);

			prof.getPattern().registerDefinitionRefedNames();

			return true;
		}

		return false;
	}

	boolean matchable() {

		return !matchers.isEmpty();
	}

	List<PatternMatcher> getAllPatternMatchers() {

		return selectMatchers(PatternMatcher.class);
	}

	PatternMatcher getProfilePatternMatcher() {

		List<PatternMatcher> ps = getAllPatternMatchers();

		return ps.isEmpty() ? null : ps.get(0);
	}

	List<PatternMatcher> getProfilePatternMatcherAsList() {

		List<PatternMatcher> ps = getAllPatternMatchers();

		return ps.isEmpty()
				? Collections.emptyList()
				: Collections.singletonList(ps.get(0));
	}

	List<PatternMatcher> getDefinitionPatternMatchers() {

		List<PatternMatcher> ps = getAllPatternMatchers();

		return ps.size() > 1 ? ps.subList(1, ps.size()) : Collections.emptyList();
	}

	List<DisjunctionMatcher> getAllDisjunctionMatchers() {

		return selectMatchers(DisjunctionMatcher.class);
	}

	List<DisjunctionMatcher> getDefinitionDisjunctionMatchers() {

		List<DisjunctionMatcher> defns = new ArrayList<DisjunctionMatcher>();

		for (DisjunctionMatcher d : getAllDisjunctionMatchers()) {

			if (d.definition()) {

				defns.add(d);
			}
		}

		return defns;
	}

	List<NodeMatcher> getAllProfileMatchers() {

		List<NodeMatcher> all = new ArrayList<NodeMatcher>();

		all.addAll(getProfilePatternMatcherAsList());
		all.addAll(getAllDisjunctionMatchers());

		return all;
	}

	List<NodeMatcher> getAllDefinitionMatchers() {

		List<NodeMatcher> all = new ArrayList<NodeMatcher>();

		all.addAll(getDefinitionPatternMatchers());
		all.addAll(getDefinitionDisjunctionMatchers());

		return all;
	}

	boolean subsumes(Name name) {

		if (name == this) {

			return true;
		}

		if (name instanceof NodeX) {

			if (local()) {

				NodeX n = (NodeX)name;

				return subsumesViaMatcher(n) || subsumesSubsumerViaMatcher(n);
			}

			return super.subsumes(name);
		}

		return false;
	}

	boolean classifiable(boolean initialPass, NodeSelector selector) {

		if (initialPass) {

			return anyMatches(selector);
		}

		if (anyNewSubsumers(selector)) {

			return true;
		}

		if (selector.matchableSelector()) {

			return anyNewDisjunctSubsumers();
		}

		return false;
	}

	boolean classifiablePatternRoot(boolean initialPass) {

		return classifiable(initialPass, NodeSelector.MATCHABLE_PATTERN_ROOT);
	}

	boolean classifiablePatternValue(boolean initialPass) {

		return classifiable(initialPass, NodeSelector.MATCHABLE_PATTERN_VALUE);
	}

	NodeClassifier getNodeClassifier() {

		return (NodeClassifier)getClassifier();
	}

	NameClassifier createClassifier() {

		return new NodeClassifier(this);
	}

	private PatternMatcher addPatternMatcher(Pattern pattern, boolean isProfile) {

		if (isProfile != selectMatchers(PatternMatcher.class).isEmpty()) {

			throw new Error("Pattern-matcher order-error for node: " + this);
		}

		return addMatcher(new PatternMatcher(this, pattern));
	}

	private <M extends NodeMatcher>M addMatcher(M matcher) {

		if (matchers == NO_MATCHERS) {

			matchers = new ArrayList<NodeMatcher>();

			getNodeClassifier().setMatchableNode();
		}

		matchers.add(matcher);

		return matcher;
	}

	private <T extends NodeMatcher>List<T> selectMatchers(Class<T> type) {

		List<T> selecteds = new ArrayList<T>();

		for (NodeMatcher m : matchers) {

			if (type.isAssignableFrom(m.getClass())) {

				selecteds.add(type.cast(m));
			}
		}

		return selecteds;
	}

	private boolean subsumesSubsumerViaMatcher(NodeX node) {

		for (Name s : node.getSubsumers()) {

			if (subsumesViaMatcher((NodeX)s)) {

				return true;
			}
		}

		return false;
	}

	private boolean subsumesViaMatcher(NodeX node) {

		return subsumesViaPattern(node) || subsumesViaDisjunction(node);
	}

	private boolean subsumesViaPattern(NodeX node) {

		PatternMatcher p = node.getProfilePatternMatcher();

		if (p != null) {

			for (PatternMatcher d : getDefinitionPatternMatchers()) {

				if (d.subsumes(p)) {

					return true;
				}
			}
		}

		return false;
	}

	private boolean subsumesViaDisjunction(NodeX node) {

		for (DisjunctionMatcher d : getAllDisjunctionMatchers()) {

			if (d.subsumesNode(node)) {

				return true;
			}

			for (DisjunctionMatcher od : node.getAllDisjunctionMatchers()) {

				if (d.subsumes(od)) {

					return true;
				}
			}
		}

		return false;
	}

	private boolean anyMatches(NodeSelector selector) {

		return selector.select(this) || selector.anyMatches(getSubsumers());
	}

	private boolean anyNewDisjunctSubsumers() {

		for (DisjunctionMatcher dm : getAllDisjunctionMatchers()) {

			for (Name d : dm.getDisjuncts()) {

				if (((NodeX)d).anyNewSubsumers(NodeSelector.ANY)) {

					return true;
				}
			}
		}

		return false;
	}
}
