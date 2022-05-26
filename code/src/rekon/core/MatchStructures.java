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

	private MatchableNodes matchables;

	private FreeClasses patternClasses;
	private FreeClasses impliedClasses;

	public void setNodeProfile(NodeName name, Collection<Relation> relations) {

		if (name.getClassifier().multipleAsserteds() || !relations.isEmpty()) {

			matchables.add(name, new NodePattern(name, relations));
		}
	}

	public void addClassDefinitions(ClassName name, Collection<NodePattern> defns) {

		matchables.addDefinitions(name, defns);
	}

	public ClassName addPatternClass(NodePattern defn) {

		return addFreeClass(patternClasses, null, Collections.singleton(defn));
	}

	public ClassName addImpliedClass(NodePattern defn) {

		return addImpliedClass(null, defn);
	}

	public ClassName addImpliedClass(ClassName sup, NodePattern defn) {

		return addImpliedClass(sup, Collections.singleton(defn));
	}

	public ClassName addImpliedClass(Collection<NodePattern> defns) {

		return addImpliedClass(null, defns);
	}

	public void addImpliedClasses(ClassName sup, Collection<NodePattern> defns) {

		for (NodePattern defn : defns) {

			addImpliedClass(sup, defn);
		}
	}

	MatchStructures(MatchableNodes matchables, FreeClasses patternClasses) {

		this(matchables, patternClasses, null);
	}

	MatchStructures(
		MatchableNodes matchables,
		FreeClasses patternClasses,
		FreeClasses impliedClasses) {

		this.matchables = matchables;
		this.patternClasses = patternClasses;
		this.impliedClasses = impliedClasses;
	}

	private ClassName addImpliedClass(
							ClassName sup,
							Collection<NodePattern> defns) {

		if (impliedClasses == null) {

			throw new Error("Cannot add implied-class definitions!");
		}

		return addFreeClass(impliedClasses, sup, defns);
	}

	private ClassName addFreeClass(
							FreeClasses freeClasses,
							ClassName sup,
							Collection<NodePattern> defns) {

		ClassName n = freeClasses.create(sup, defns);

		matchables.addDefinitions(n, defns);

		return n;
	}
}
