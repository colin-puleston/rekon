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

	static private final List<MatchableNode<?>> NO_MATCHABLE_NODES = Collections.emptyList();

	private List<MatchableNode<?>> matchableNodes = NO_MATCHABLE_NODES;

	void addMatchableNode(MatchableNode<?> matchable) {

		checkNewMatchableNode(matchable);

		if (matchableNodes == NO_MATCHABLE_NODES) {

			matchableNodes = new ArrayList<MatchableNode<?>>();

			getNodeClassifier().setMatchableNode();
		}

		matchableNodes.add(matchable);
	}

	boolean matchable() {

		return !matchableNodes.isEmpty();
	}

	List<MatchableNode<?>> getMatchableNodes() {

		return matchableNodes;
	}

	PatternNode getPatternNode() {

		List<PatternNode> pns = selectMatchableNodes(PatternNode.class);

		return pns.isEmpty() ? null : pns.get(0);
	}

	List<DisjunctionNode> getDisjunctionNodes() {

		return selectMatchableNodes(DisjunctionNode.class);
	}

	NodePattern getProfilePattern() {

		PatternNode pn = getPatternNode();

		return pn != null ? pn.getProfile() : null;
	}

	Collection<NodePattern> getDefinitionPatterns() {

		PatternNode pn = getPatternNode();

		return pn != null ? pn.getDefinitions() : Collections.emptySet();
	}

	boolean subsumes(Name name) {

		if (name == this) {

			return true;
		}

		if (name instanceof NodeName) {

			return local() ? anyMatchableSubsumes((NodeName)name) : super.subsumes(name);
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

	NodeNameClassifier getNodeClassifier() {

		return (NodeNameClassifier)getClassifier();
	}

	NameClassifier createClassifier() {

		return new NodeNameClassifier(this);
	}

	private void checkNewMatchableNode(MatchableNode<?> m) {

		if (m instanceof PatternNode && getPatternNode() != null) {

			throw new Error("Attempting to add second pattern-node for: " + this);
		}
	}

	private <T extends MatchableNode<?>>List<T> selectMatchableNodes(Class<T> type) {

		List<T> selecteds = new ArrayList<T>();

		for (MatchableNode<?> m : matchableNodes) {

			if (type.isAssignableFrom(m.getClass())) {

				selecteds.add(type.cast(m));
			}
		}

		return selecteds;
	}

	private boolean anyMatchableSubsumes(NodeName name) {

		for (MatchableNode<?> m : matchableNodes) {

			if (m.subsumesNode(name) || name.anyMatchableSubsumedBy(m)) {

				return true;
			}
		}

		return false;
	}

	private boolean anyMatchableSubsumedBy(MatchableNode<?> matchable) {

		for (MatchableNode<?> m : matchableNodes) {

			if (matchable.subsumesMatchable(m)) {

				return true;
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
