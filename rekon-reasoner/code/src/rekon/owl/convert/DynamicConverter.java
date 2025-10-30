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

package rekon.owl.convert;

import org.semanticweb.owlapi.model.*;

import rekon.core.*;
import rekon.build.*;

/**
 * @author Colin Puleston
 */
public class DynamicConverter {

	private NameMapper names;
	private ExpressionConverter expressions;

	private Queryables queryables;

	public Queryable toQueryable(OWLClassExpression expr) {

		if (expr instanceof OWLClass) {

			return toQueryable((OWLClass)expr);
		}

		return queryables.create(toDynamicPatternBuilder(expr));
	}

	public Queryable toQueryable(OWLClass cls) {

		return queryables.create(names.get(cls));
	}

	public Queryable toQueryable(OWLNamedIndividual ind) {

		return queryables.create(names.get(ind));
	}

	public DynamicPatternBuilder toDynamicPatternBuilder(OWLClassExpression expr) {

		return new DynamicPatternBuilder(names, expressions.toDynamicNode(expr));
	}

	DynamicConverter(
		NameMapper names,
		Ontology ontology,
		ExpressionConverter expressions) {

		this.names = names;
		this.expressions = expressions;

		queryables = ontology.createQueryables();
	}
}
