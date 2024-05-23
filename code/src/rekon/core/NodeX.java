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

	PatternMatcher addProfilePatternMatcher() {

		return addProfilePatternMatcher(new Pattern(this));
	}

	PatternMatcher addProfilePatternMatcher(Pattern profile) {

		int pmCount = patternMatcherCount();

		if (pmCount != 0) {

			throw new Error("Profile pattern-matcher already exists for node: " + this);
		}

		return addPatternMatcher(profile, pmCount);
	}

	void removeProfilePatternMatcher() {

		if (patternMatcherCount() == 0) {

			throw new Error("No profile pattern-matcher for node: " + this);
		}

		matchers.remove(0);
	}

	PatternMatcher addDefinitionPatternMatcher(Pattern defn) {

		defn.registerDefinitionRefedNames();

		int pmCount = patternMatcherCount();

		if (pmCount == 0) {

			throw new Error("No profile pattern-matcher for node: " + this);
		}

		return addPatternMatcher(defn, pmCount);
	}

	DisjunctionMatcher addDisjunctionMatcher(Collection<? extends NodeX> disjuncts) {

		int pmCount = patternMatcherCount();

		return addMatcher(new DisjunctionMatcher(this, disjuncts), pmCount);
	}

	boolean ensurePatternProfiledImpliesPatternDefined() {

		int pmCount = patternMatcherCount();

		if (pmCount == 1) {

			PatternMatcher prof = getProfilePatternMatcher();

			addMatcher(prof, 1);

			prof.getPattern().registerDefinitionRefedNames();

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

	List<PatternMatcher> getAllPatternMatchers() {

		int pmCount = patternMatcherCount();

		return pmCount != 0 ? getPatternMatchers(0, pmCount) : Collections.emptyList();
	}

	PatternMatcher getProfilePatternMatcher() {

		int pmCount = patternMatcherCount();

		return pmCount == 0 ? null : (PatternMatcher)matchers.get(0);
	}

	List<PatternMatcher> getProfilePatternMatcherAsList() {

		PatternMatcher pm = getProfilePatternMatcher();

		return pm != null ? Collections.singletonList(pm) : Collections.emptyList();
	}

	List<PatternMatcher> getDefinitionPatternMatchers() {

		int pmCount = patternMatcherCount();

		return pmCount > 1 ? getPatternMatchers(1, pmCount) : Collections.emptyList();
	}

	List<DisjunctionMatcher> getAllDisjunctionMatchers() {

		int mCount = matchers.size();
		int pmCount = patternMatcherCount();

		return pmCount != mCount
				? getDisjunctionMatchers(pmCount, mCount)
				: Collections.emptyList();
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

	boolean subsumesDirectly(NodeX node) {

		return super.subsumes(node);
	}

	boolean subsumesMatcher(NodeMatcher test) {

		for (NodeMatcher d : getAllDefinitionMatchers()) {

			if (d.subsumes(test)) {

				return true;
			}
		}

		return false;
	}

	boolean subsumedByMatcher(NodeMatcher test) {

		for (NodeMatcher d : getAllProfileMatchers()) {

			if (test.subsumes(d)) {

				return true;
			}
		}

		return false;
	}

	boolean matchablePatternRoot(boolean initialPass) {

		return unprocessedSubsumers(initialPass, NodeSelector.MATCHABLE_PATTERN_ROOT);
	}

	boolean matchablePatternValue(boolean initialPass) {

		return unprocessedSubsumers(initialPass, NodeSelector.MATCHABLE_PATTERN_VALUE);
	}

	boolean unprocessedSubsumers(boolean initialPass, NodeSelector selector) {

		return initialPass ? anyMatches(selector) : anyNewSubsumers(selector);
	}

	NodeClassifier getNodeClassifier() {

		return (NodeClassifier)getClassifier();
	}

	NameClassifier createClassifier() {

		return new NodeClassifier(this);
	}

	private PatternMatcher addPatternMatcher(Pattern pattern, int index) {

		return addMatcher(new PatternMatcher(this, pattern), index);
	}

	private <M extends NodeMatcher>M addMatcher(M matcher, int index) {

		if (matchers == NO_MATCHERS) {

			matchers = new ArrayList<NodeMatcher>();

			getNodeClassifier().setClassifiableNode();
		}

		matchers.add(index, matcher);

		return matcher;
	}

	private int patternMatcherCount() {

		int i = 0;

		for (NodeMatcher m : matchers) {

			if (m instanceof DisjunctionMatcher) {

				break;
			}

			i++;
		}

		return i;
	}

	private List<PatternMatcher> getPatternMatchers(int start, int end) {

		return getMatchers(PatternMatcher.class, start, end);
	}

	private List<DisjunctionMatcher> getDisjunctionMatchers(int start, int end) {

		return getMatchers(DisjunctionMatcher.class, start, end);
	}

	private <T extends NodeMatcher>List<T> getMatchers(Class<T> type, int start, int end) {

		List<T> selecteds = new ArrayList<T>();

		for (int i = start ; i < end ; i++) {

			selecteds.add(type.cast(matchers.get(i)));
		}

		return selecteds;
	}

	private boolean subsumesNodeOrSubsumerViaMatcher(NodeX baseTarget) {

		if (subsumesViaMatcher(baseTarget, baseTarget)) {

			return true;
		}

		for (NodeX s : baseTarget.getSubsumers().asNodes()) {

			if (subsumesViaMatcher(baseTarget, s)) {

				return true;
			}
		}

		return false;
	}

	private boolean subsumesViaMatcher(NodeX baseTarget, NodeX currentTarget) {

		for (NodeMatcher d : getAllDefinitionMatchers()) {

			if (d.subsumesNodeDirectly(currentTarget)) {

				return true;
			}

			for (NodeMatcher p : currentTarget.getAllProfileMatchers()) {

				if (currentTarget == baseTarget || !p.hasExpandedDisjunct(baseTarget)) {

					if (d.subsumes(p)) {

						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean anyMatches(NodeSelector selector) {

		return selector.select(this) || selector.anyMatches(getSubsumers());
	}
}
