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
class DynamicNodeOpsHandler extends ValidInputDynamicOpsHandler {

	private NodeX node;

	private abstract class MatcherCollector {

		Collection<NodeMatcher> getAll() {

			List<NodeMatcher> matchers = new ArrayList<NodeMatcher>();

			addMatchers(matchers, node);

			for (Name en : node.getEquivalents()) {

				addMatchers(matchers, (NodeX)en);
			}

			return matchers;
		}

		abstract Collection<NodeMatcher> getCandidateMatchers(NodeX n);

		private void addMatchers(Collection<NodeMatcher> matchers, NodeX n) {

			matchers.addAll(getCandidateMatchers(n));
		}
	}

	private class ProfileMatcherCollector extends MatcherCollector {

		Collection<NodeMatcher> getCandidateMatchers(NodeX n) {

			return n.getAllProfileMatchers();
		}
	}

	private class DefinitionMatcherCollector extends MatcherCollector {

		Collection<NodeMatcher> getCandidateMatchers(NodeX n) {

			return n.getAllDefinitionMatchers();
		}
	}

	public Names getEquivalents() {

		NameList equivs = new NameList(node);

		equivs.addAll(node.getEquivalents());

		return equivs;
	}

	public Names getSupers(boolean direct) {

		return node.getSupers(direct);
	}

	public Names getSubs(boolean direct) {

		return node.getSubs(ClassNode.class, direct);
	}

	public Names getIndividuals(boolean direct) {

		return node.getSubs(IndividualNode.class, direct);
	}

	DynamicNodeOpsHandler(NodeX node) {

		this.node = node;
	}

	boolean equivalentTo(ValidInputDynamicOpsHandler other) {

		if (other instanceof DynamicNodeOpsHandler) {

			DynamicNodeOpsHandler o = (DynamicNodeOpsHandler)other;

			return node.getEquivalents().getNames().contains(o.node);
		}

		return super.equivalentTo(other);
	}

	boolean subsumes(ValidInputDynamicOpsHandler other) {

		for (Name ps : other.getPotentialSubNodes().getNames()) {

			if (node.subsumes((NodeX)ps)) {

				return true;
			}
		}

		return super.subsumes(other);
	}

	NodeX getNode() {

		return node;
	}

	Collection<NodeMatcher> getAllDefinitionMatchers() {

		return new DefinitionMatcherCollector().getAll();
	}

	Collection<NodeMatcher> getAllProfileMatchers(Collection<NodeMatcher> defns) {

		return new ProfileMatcherCollector().getAll();
	}

	Names getPotentialSubNodes() {

		return new NameList(node);
	}
}
