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

	private NameSet disjunctNodes = new NameSet();

	public NodeValue(NodeX node) {

		disjunctNodes.add(node);
	}

	public NodeValue(Collection<? extends NodeX> disjunctNodes) {

		this.disjunctNodes.retainMostGeneral(new NameList(disjunctNodes));
	}

	NodeValue asNodeValue() {

		return this;
	}

	boolean singleValueNode() {

		return disjunctNodes.size() == 1;
	}

	NodeX getSingleValueNode() {

		if (singleValueNode()) {

			return disjunctNodes.getFirstNode();
		}

		throw new Error("Not a single value node!");
	}

	Names getDisjunctNodes() {

		return disjunctNodes;
	}

	Names getMostSpecificCommonDisjunctSubsumers() {

		List<Collection<Name>> subsumerSets = new ArrayList<Collection<Name>>();

		for (NodeX d : disjunctNodes.asNodes()) {

			subsumerSets.add(d.getSubsumers().getNames());
		}

		NameSet mostSpecifics = new NameSet();

		mostSpecifics.retainMostSpecific(NameSetIntersector.intersect(subsumerSets));

		return mostSpecifics;
	}

	void collectNames(NameCollector collector) {

		if (singleValueNode()) {

			collector.collectForSingleValueNode(getSingleValueNode());
		}
		else {

			collector.collectForDisjunctNodes(this);
		}
	}

	boolean subsumesOther(Value v) {

		NodeValue nv = v.asNodeValue();

		return nv != null && subsumesAllDisjunctNodesOf(nv);
	}

	boolean anyNewSubsumers(NodeSelector selector) {

		for (NodeX d : disjunctNodes.asNodes()) {

			if (d.anyNewSubsumers(selector)) {

				return true;
			}
		}

		return false;
	}

	void registerAsDefinitionRefed() {

		registerAllAsDefinitionRefed(disjunctNodes);

		if (!singleValueNode()) {

			registerAllAsDefinitionRefed(getMostSpecificCommonDisjunctSubsumers());
		}
	}

	void render(PatternRenderer r) {

		if (singleValueNode()) {

			renderNode(r, getSingleValueNode());
		}
		else {

			r.addLine("OR");

			r = r.nextLevel();

			for (NodeX d : disjunctNodes.asNodes()) {

				renderNode(r, d);
			}
		}
	}

	private boolean subsumesAllDisjunctNodesOf(NodeValue nv) {

		for (NodeX d : nv.disjunctNodes.asNodes()) {

			if (!subsumesNode(d)) {

				return false;
			}
		}

		return true;
	}

	private boolean subsumesNode(NodeX n) {

		for (NodeX d : disjunctNodes.asNodes()) {

			if (d.subsumes(n)) {

				return true;
			}
		}

		return false;
	}

	private void registerAllAsDefinitionRefed(Names names) {

		for (NodeX n : names.asNodes()) {

			n.registerAsDefinitionRefed(MatchRole.VALUE);
		}
	}

	private void renderNode(PatternRenderer r, NodeX n) {

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
