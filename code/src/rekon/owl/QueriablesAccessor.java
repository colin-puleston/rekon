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

package rekon.owl;

import org.semanticweb.owlapi.model.*;

import rekon.core.*;
import rekon.build.*;

/**
 * @author Colin Puleston
 */
class QueriablesAccessor {

	private MappedNames names;
	private CoreBuilder coreBuilder;
	private ExpressionConverter exprConverter;

	private Queriables queriables;

	QueriablesAccessor(
		Ontology ontology,
		MappedNames names,
		CoreBuilder coreBuilder,
		ExpressionConverter exprConverter) {

		this.names = names;
		this.coreBuilder = coreBuilder;
		this.exprConverter = exprConverter;

		queriables = ontology.createQueriables();
	}

	Queriable create(OWLClassExpression expr) {

		if (expr instanceof OWLClass) {

			return create((OWLClass)expr);
		}

		return queriables.create(toDynamicPatternSource(expr));
	}

	Queriable create(OWLClass cls) {

		return queriables.create(names.get(cls));
	}

	Queriable create(OWLNamedIndividual ind) {

		return queriables.create(names.get(ind));
	}

	private MultiPatternSource toDynamicPatternSource(OWLClassExpression expr) {

		return coreBuilder.createMultiPatternSource(exprConverter.toNode(expr));
	}
}
