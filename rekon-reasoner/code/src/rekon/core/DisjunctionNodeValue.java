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
class DisjunctionNodeValue extends NodeValue {

	private Names disjunctNodes;

	DisjunctionNodeValue(Names disjunctNodes) {

		this.disjunctNodes = disjunctNodes;
	}

	boolean singleValueNode() {

		return false;
	}

	NodeX getSingleValueNode() {

		throw new RuntimeException("Not a single value node!");
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

		collector.collectForDisjunctNodes(this);
	}

	boolean subsumesNode(NodeX n) {

		for (NodeX d : disjunctNodes.asNodes()) {

			if (d.subsumes(n)) {

				return true;
			}
		}

		return false;
	}

	void registerAsDefinitionRefed() {

		registerAllAsDefinitionRefed(disjunctNodes);
		registerAllAsDefinitionRefed(getMostSpecificCommonDisjunctSubsumers());
	}

	void render(PatternRenderer r) {

		r.addLine("OR");

		r = r.nextLevel();

		for (NodeX d : disjunctNodes.asNodes()) {

			renderNode(r, d);
		}
	}

	private void registerAllAsDefinitionRefed(Names nodes) {

		for (NodeX n : nodes.asNodes()) {

			n.registerAsDefinitionRefed(MatchRole.NESTED_PATTERN_VALUE);
		}
	}
}
