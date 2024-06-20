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
class InsertedProfileDisjunctions {

	private MatchStructures matchStructures;
	private FreeOntologyClasses freeOntologyClasses;

	private Map<Set<NodeX>, ClassNode> classesByDisjuncts
						= new HashMap<Set<NodeX>, ClassNode>();

	InsertedProfileDisjunctions(
		MatchStructures matchStructures,
		FreeOntologyClasses freeOntologyClasses) {

		this.matchStructures = matchStructures;
		this.freeOntologyClasses = freeOntologyClasses;
	}

	ClassNode resolve(Collection<NodeX> disjuncts) {

		Set<NodeX> resolvedDisjuncts = resolveNestedDisjuncts(disjuncts);
		ClassNode cn = classesByDisjuncts.get(resolvedDisjuncts);

		if (cn == null) {

			cn = freeOntologyClasses.createInsertedClass();

			matchStructures.addDisjunction(cn, resolvedDisjuncts, false);

			classesByDisjuncts.put(resolvedDisjuncts, cn);
		}

		return cn;
	}

	private Set<NodeX> resolveNestedDisjuncts(Collection<NodeX> disjuncts) {

		Set<NodeX> resolvedDisjuncts = new HashSet<NodeX>();

		for (NodeX d : disjuncts) {

			if (freeOntologyClasses.insertedClass(d)) {

				absorbInsertedDisjunct(resolvedDisjuncts, d);
			}
		}

		return resolvedDisjuncts;
	}

	private void absorbInsertedDisjunct(Collection<NodeX> disjuncts, NodeX inserted) {

		for (DisjunctionMatcher disjunction : inserted.getAllDisjunctionMatchers()) {

			for (NodeX d : disjunction.getDirectDisjuncts().asNodes()) {

				absorbNestedDisjunct(disjuncts, d);
			}
		}
	}

	private void absorbNestedDisjunct(Collection<NodeX> disjuncts, NodeX nested) {

		if (disjuncts.contains(nested)) {

			return;
		}

		for (NodeX d : new ArrayList<NodeX>(disjuncts)) {

			if (d.subsumes(nested)) {

				return;
			}

			if (nested.subsumes(d)) {

				disjuncts.remove(d);
			}
		}

		disjuncts.add(nested);
	}
}