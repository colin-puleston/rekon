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
class MatchableNodes {

	private List<PatternNode> patternNodes = new ArrayList<PatternNode>();
	private List<DisjunctionNode> disjunctionNodes = new ArrayList<DisjunctionNode>();

	void addPatternNode(NodeName name, NodePattern profile) {

		addPatternNode(new PatternNode(name, profile));
	}

	void addPatternNodeDefinition(ClassName name, NodePattern defn) {

		resolvePatternNode(name).addDefinition(defn);
	}

	void addDisjunctionNode(ClassName name, Collection<? extends NodeName> disjuncts) {

		addDisjunctionNode(new DisjunctionNode(name, disjuncts));
	}

	List<PatternNode> getAllPatternNodes() {

		return patternNodes;
	}

	List<PatternNode> getDefinedPatternNodes() {

		List<PatternNode> defineds = new ArrayList<PatternNode>();

		for (PatternNode n : patternNodes) {

			if (n.hasDefinitions()) {

				defineds.add(n);
			}
		}

		return defineds;
	}

	List<DisjunctionNode> getDisjunctionNodes() {

		return disjunctionNodes;
	}

	void addPatternNode(PatternNode node) {

		patternNodes.add(node);
	}

	void addDisjunctionNode(DisjunctionNode node) {

		disjunctionNodes.add(node);
	}

	private PatternNode resolvePatternNode(NodeName name) {

		PatternNode n = name.getPatternNode();

		if (n == null) {

			n = new PatternNode(name);

			addPatternNode(n);
		}

		return n;
	}
}
