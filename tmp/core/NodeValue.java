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
public class NodeValue extends Value {

	private NodeX node;

	public NodeValue(NodeX node) {

		this.node = node;
	}

	NodeValue asNodeValue() {

		return this;
	}

	NodeX getValueNode() {

		return node;
	}

	void collectNames(NameCollector collector) {

		collector.collectForValueNode(node);
	}

	boolean subsumesOther(Value v) {

		NodeValue nv = v.asNodeValue();

		return nv != null && node.subsumes(nv.node);
	}

	boolean anyNewSubsumers(NodeSelector selector) {

		return node.anyNewSubsumers(selector);
	}

	void render(PatternRenderer r) {

		List<NodeMatcher> nlrMatchers = getNextLevelRenderMatchers(r);

		if (nlrMatchers.isEmpty()) {

			r.addLine(node.getLabel());
		}
		else {

			r.addLine(node.getLabel() + "==>");

			r = r.nextLevel();

			for (NodeMatcher m : nlrMatchers) {

				m.render(r);
			}
		}
	}

	private List<NodeMatcher> getNextLevelRenderMatchers(PatternRenderer r) {

		if (!node.mapped()) {

			PatternRenderType renderType = r.getRenderType();

			if (renderType == PatternRenderType.NESTED_PROFILE) {

				return node.getAllProfileMatchers();
			}

			if (renderType == PatternRenderType.NESTED_DEFINITION) {

				return node.getAllDefinitionMatchers();
			}
		}

		return Collections.emptyList();
	}
}
