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
class DisjunctionBuilder {

	private ComponentBuilder componentBuilder;

	private Disjunctions disjunctions;

	private class Disjunctions extends EntityBuilder<Collection<InputNode>, Collection<NodeX>> {

		Disjunctions(boolean dynamic) {

			super(dynamic);
		}

		Collection<NodeX> checkCreate(Collection<InputNode> source) {

			Set<NodeX> disjuncts = new HashSet<NodeX>();

			for (InputNode n : source) {

				Set<? extends NodeX> djs = toDisjuncts(n);

				if (djs == null) {

					return null;
				}

				disjuncts.addAll(djs);
			}

			return disjuncts;
		}

		private Set<? extends NodeX> toDisjuncts(InputNode source) {

			if (source.hasComplexType(InputComplexType.DISJUNCTION)) {

				return toIndividualDisjuncts(source.asComplex().asDisjuncts());
			}

			NodeX n = componentBuilder.toAtomicNode(source);

			return n != null ? Collections.singleton(n) : null;
		}

		private Set<IndividualNode> toIndividualDisjuncts(Collection<InputNode> source) {

			Set<IndividualNode> disjuncts = new HashSet<IndividualNode>();

			for (InputNode d : source) {

				if (!d.hasNodeType(InputNodeType.INDIVIDUAL)) {

					return null;
				}

				disjuncts.add(d.asIndividualNode());
			}

			return disjuncts;
		}
	}

	DisjunctionBuilder(ComponentBuilder componentBuilder, boolean dynamic) {

		this.componentBuilder = componentBuilder;

		disjunctions = new Disjunctions(dynamic);
	}

	Collection<NodeX> toDisjunction(Collection<InputNode> source) {

		return disjunctions.get(source);
	}
}
