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
public abstract class NodeValue extends Value {

	static public NodeValue create(Collection<? extends NodeX> disjunctNodes) {

		NameSet resolvedDjns = new NameSet();

		resolvedDjns.retainMostGeneral(new NameList(disjunctNodes));

		if (resolvedDjns.size() == 1) {

			return new SingleNodeValue(resolvedDjns.getFirstNode());
		}

		return new DisjunctionNodeValue(resolvedDjns);
	}

	NodeValue asNodeValue() {

		return this;
	}

	abstract boolean singleValueNode();

	abstract NodeX getSingleValueNode();

	abstract Names getDisjunctNodes();

	abstract void collectNames(NameCollector collector);

	boolean subsumesOther(Value v) {

		NodeValue nv = v.asNodeValue();

		if (nv == null) {

			return false;
		}

		if (nv.singleValueNode()) {

			return subsumesNode(nv.getSingleValueNode());
		}

		return subsumesAllNodes(nv.getDisjunctNodes());
	}

	abstract boolean subsumesNode(NodeX n);

	abstract void registerAsDefinitionRefed();

	abstract void render(PatternRenderer r);

	void renderNode(PatternRenderer r, NodeX n) {

		List<PatternMatcher> nlrMatchers = getNextLevelRenderMatchers(r, n);

		if (nlrMatchers.isEmpty()) {

			r.addLine(n.getLabel());
		}
		else {

			r.addLine(n.getLabel() + "==>");

			r = r.nextLevel();

			for (PatternMatcher m : nlrMatchers) {

				m.render(r);
			}
		}
	}

	private boolean subsumesAllNodes(Names ns) {

		for (NodeX n : ns.asNodes()) {

			if (!subsumesNode(n)) {

				return false;
			}
		}

		return true;
	}

	private List<PatternMatcher> getNextLevelRenderMatchers(PatternRenderer r, NodeX n) {

		if (!n.mapped()) {

			PatternRenderType renderType = r.getRenderType();

			if (renderType == PatternRenderType.NESTED_PROFILE) {

				return n.getProfileMatcherAsList();
			}

			if (renderType == PatternRenderType.NESTED_DEFINITION) {

				return n.getDefinitionMatchers();
			}
		}

		return Collections.emptyList();
	}
}
