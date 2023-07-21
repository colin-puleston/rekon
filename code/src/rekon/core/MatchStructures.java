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
public class MatchStructures {

	private NodeMatchers nodeMatchers;
	private FreeClasses freeClasses;

	public void checkAddProfilePattern(NodeName node, Collection<Relation> relations) {

		if (node.getClassifier().multipleAssertedSubsumers() || !relations.isEmpty()) {

			nodeMatchers.addProfilePattern(node, new Pattern(node, relations));
		}
	}

	public void addDefinitionPattern(NodeName node, Pattern defn) {

		nodeMatchers.addDefinitionPattern(node, defn);
	}

	public void addDisjunction(
					ClassName node,
					Collection<? extends NodeName> disjuncts) {

		nodeMatchers.addDisjunction(node, disjuncts);
	}

	public ClassName addPatternClass() {

		return freeClasses.createPatternClass();
	}

	public ClassName addGCIImpliedClass() {

		return freeClasses.createGCIImpliedClass();
	}

	MatchStructures(NodeMatchers nodeMatchers, FreeClasses freeClasses) {

		this.nodeMatchers = nodeMatchers;
		this.freeClasses = freeClasses;
	}
}
