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
public class NodeValue extends ObjectValue {

	private NodeName node;

	public NodeValue(NodeName node) {

		this.node = node;
	}

	NodeValue asNodeValue() {

		return this;
	}

	NodeName getValueNode() {

		return node;
	}

	void collectNames(NameCollector collector) {

		collector.collectForValueNode(node);
	}

	boolean subsumesOther(Value v) {

		NodeValue nv = v.asNodeValue();

		return nv != null && subsumesOtherNode(nv.node);
	}

	boolean anyNewSubsumers(NodeSelector selector) {

		return node.anyNewSubsumers(selector);
	}

	void render(PatternRenderer r) {

		r.addLine(node.getLabel());
	}

	private boolean subsumesOtherNode(NodeName on) {

		return node.dynamic() ? anyMatchableSubsumes(on) : node.subsumes(on);
	}

	private boolean anyMatchableSubsumes(NodeName on) {

		for (MatchableNode<?> m : node.getMatchableNodes()) {

			if (m.subsumesNode(on) || subsumesAnyMatchable(m, on)) {

				return true;
			}
		}

		return false;
	}

	private boolean subsumesAnyMatchable(MatchableNode<?> m, NodeName on) {

		for (MatchableNode<?> om : on.getMatchableNodes()) {

			if (m.subsumesMatchable(om)) {

				return true;
			}
		}

		return false;
	}
}
