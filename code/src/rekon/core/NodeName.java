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
public abstract class NodeName extends Name {

	static private final List<NodeMatcher> NO_MATCHERS = Collections.emptyList();

	static private class ProfilePatternMatcher extends PatternMatcher {

		ProfilePatternMatcher(NodeName node, Pattern pattern) {

			super(node, pattern);
		}
	}

	static private class DefinitionPatternMatcher extends PatternMatcher {

		DefinitionPatternMatcher(NodeName node, Pattern pattern) {

			super(node, pattern);
		}
	}

	private List<NodeMatcher> matchers = NO_MATCHERS;

	PatternMatcher addProfilePatternMatcher() {

		return addProfilePatternMatcher(new Pattern(this));
	}

	PatternMatcher addProfilePatternMatcher(Pattern pattern) {

		if (getProfilePatternMatcher() != null) {

			throw new Error("Attempting to add second profile-pattern for node: " + this);
		}

		return addMatcher(new ProfilePatternMatcher(this, pattern));
	}

	PatternMatcher addDefinitionPatternMatcher(Pattern pattern) {

		return addMatcher(new DefinitionPatternMatcher(this, pattern));
	}

	DisjunctionMatcher addDisjunctionMatcher(Collection<? extends NodeName> disjuncts) {

		return addMatcher(new DisjunctionMatcher(this, disjuncts));
	}

	boolean matchable() {

		return !matchers.isEmpty();
	}

	PatternMatcher getProfilePatternMatcher() {

		List<PatternMatcher> ps = selectPatternMatchers(ProfilePatternMatcher.class);

		return ps.isEmpty() ? null : ps.get(0);
	}

	List<PatternMatcher> getDefinitionPatternMatchers() {

		return selectPatternMatchers(DefinitionPatternMatcher.class);
	}

	List<DisjunctionMatcher> getDisjunctionMatchers() {

		return selectMatchers(DisjunctionMatcher.class, DisjunctionMatcher.class);
	}

	boolean subsumes(Name name) {

		if (name == this) {

			return true;
		}

		if (name instanceof NodeName) {

			return local() ? subsumesViaMatcher((NodeName)name) : super.subsumes(name);
		}

		return false;
	}

	boolean classifyTargetPatternRoot(boolean initialPass) {

		return classifyTarget(initialPass, NodeSelector.CLASSIFY_TARGET_PATTERN_ROOT);
	}

	boolean classifyTargetPatternValue(boolean initialPass) {

		return classifyTarget(initialPass, NodeSelector.CLASSIFY_TARGET_PATTERN_VALUE);
	}

	boolean classifyTargetDisjunct(boolean initialPass) {

		return classifyTarget(initialPass, NodeSelector.CLASSIFY_TARGET_DISJUNCT);
	}

	NodeClassifier getNodeClassifier() {

		return (NodeClassifier)getClassifier();
	}

	NameClassifier createClassifier() {

		return new NodeClassifier(this);
	}

	private <M extends NodeMatcher>M addMatcher(M matcher) {

		if (matchers == NO_MATCHERS) {

			matchers = new ArrayList<NodeMatcher>();

			getNodeClassifier().setMatchableNode();
		}

		matchers.add(matcher);

		return matcher;
	}

	private <S extends PatternMatcher>List<PatternMatcher> selectPatternMatchers(Class<S> type) {

		return selectMatchers(PatternMatcher.class, type);
	}

	private <L extends NodeMatcher, S extends L>List<L> selectMatchers(
															Class<L> listType,
															Class<S> selectType) {

		List<L> selecteds = new ArrayList<L>();

		for (NodeMatcher m : matchers) {

			if (selectType.isAssignableFrom(m.getClass())) {

				selecteds.add(selectType.cast(m));
			}
		}

		return selecteds;
	}

	private boolean subsumesViaMatcher(NodeName node) {

		return subsumesViaPattern(node) || subsumesViaDisjunction(node);
	}

	private boolean subsumesViaPattern(NodeName node) {

		PatternMatcher p = node.getProfilePatternMatcher();

		if (p != null) {

			for (PatternMatcher d : getDefinitionPatternMatchers()) {

				if (d.subsumesPattern(p)) {

					return true;
				}
			}
		}

		return false;
	}

	private boolean subsumesViaDisjunction(NodeName node) {

		for (DisjunctionMatcher d : getDisjunctionMatchers()) {

			if (d.subsumesNode(node)) {

				return true;
			}

			for (DisjunctionMatcher p : node.getDisjunctionMatchers()) {

				if (d.subsumesDisjunction(p)) {

					return true;
				}
			}
		}

		return false;
	}

	private boolean classifyTarget(boolean initialPass, NodeSelector selector) {

		return initialPass ? anyMatches(selector) : anyNewSubsumers(selector);
	}

	private boolean anyMatches(NodeSelector selector) {

		return selector.select(this) || selector.anyMatches(getSubsumers());
	}
}
