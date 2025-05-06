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

/**
 * @author Colin Puleston
 */
public class Queryables {

	static private final Queryable INVALID_INPUT = new InertQueryable();

	static private class InertQueryable implements Queryable {

		public Names getEquivalents() {

			return Names.NO_NAMES;
		}

		public Names getSupers(boolean direct) {

			return Names.NO_NAMES;
		}

		public Names getSubs(boolean direct) {

			return Names.NO_NAMES;
		}

		public Names getIndividuals(boolean direct) {

			return Names.NO_NAMES;
		}

		public boolean equivalentTo(Queryable other) {

			return false;
		}

		public boolean subsumes(Queryable other) {

			return false;
		}

		InertQueryable() {
		}
	}

	private Ontology ontology;

	public Queryable create(NodeX node) {

		return new QueryableNode(node);
	}

	public Queryable create(PatternSource source) {

		DynamicExpression expr = new DynamicExpression(source);

		if (expr.expressionCreated()) {

			NodeX node = expr.toSingleNode();

			if (node != null) {

				return new QueryableNode(node);
			}

			return new QueryableExpression(ontology, expr);
		}

		return INVALID_INPUT;
	}

	Queryables(Ontology ontology) {

		this.ontology = ontology;
	}
}
