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

package rekon.build;

import java.util.*;

import rekon.core.*;
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
class MatchStuctureBuilder {

	private MatchStructures matchStructures;

	MatchStuctureBuilder(MatchStructures matchStructures) {

		this.matchStructures = matchStructures;
	}

	void checkAddProfilePattern(NodeX node, Collection<Relation> relations) {

		matchStructures.checkAddProfilePattern(node, relations);
	}

	void addProfileDisjunction(NodeX node, List<Pattern> disjuncts) {

		addDisjunction(node, disjuncts, false);
	}

	ClassNode addDefinitionClass() {

		return matchStructures.addDefinitionClass();
	}

	ClassNode addDefinitionClass(Pattern defn) {

		ClassNode c = addDefinitionClass();

		matchStructures.addDefinitionPattern(c, defn);

		return c;
	}

	void addDefinitionPattern(NodeX node, Pattern defn) {

		matchStructures.addDefinitionPattern(node, defn);
	}

	void addDefinitionDisjunction(ClassNode node, List<Pattern> disjuncts) {

		addDisjunction(node, disjuncts, true);
	}

	List<NodeX> resolveDisjunctionToNodes(List<Pattern> disjuncts) {

		List<NodeX> nodeDjs = new ArrayList<NodeX>();

		for (Pattern d : disjuncts) {

			nodeDjs.add(resolveDisjunctToNode(d));
		}

		return nodeDjs;
	}

	private void addDisjunction(NodeX node, List<Pattern> disjuncts, boolean definition) {

		List<NodeX> nodeDjs = resolveDisjunctionToNodes(disjuncts);

		matchStructures.addDisjunction(node, nodeDjs, definition);
	}

	private NodeX resolveDisjunctToNode(Pattern disjunct) {

		NodeX n = disjunct.toSingleNode();

		return n != null ? n : addDefinitionClass(disjunct);
	}
}
