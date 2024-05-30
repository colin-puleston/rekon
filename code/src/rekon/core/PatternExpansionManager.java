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
class PatternExpansionManager {

	private Ontology ontology;

	private NameSet visited = new NameSet();
	private boolean incompleteTraversal = false;

	PatternExpansionManager(NodeX initialNode) {

		this(null, initialNode);
	}

	PatternExpansionManager(Ontology ontology, NodeX initialNode) {

		this.ontology = ontology;

		visited.add(initialNode);
	}

	boolean startVisit(NodeX node) {

		if (visited.add(node)) {

			return true;
		}

		incompleteTraversal = true;

		return false;
	}

	ClassNode addDerivedValueDisjunction(Collection<NodeX> disjuncts) {

		if (localExpansion()) {

			throw new Error("Unexpected disjunction-value derivation!");
		}

		return ontology.addDerivedProfileValueDisjunction(disjuncts);
	}

	boolean localExpansion() {

		return ontology == null;
	}

	boolean incompleteTraversal() {

		return incompleteTraversal;
	}
}

