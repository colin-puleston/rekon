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
import rekon.owl.convert.*;

/**
 * @author Colin Puleston
 */
class OwlQueryables {

	private CoreBuilder coreBuilder;

	private OwlConverter converter;
	private NameMapper names;

	private Queryables queryables;

	OwlQueryables(Ontology ontology, CoreBuilder coreBuilder, OwlConverter converter) {

		this.coreBuilder = coreBuilder;
		this.converter = converter;

		names = converter.getNameMapper();
		queryables = ontology.createQueryables();
	}

	Queryable create(OWLClassExpression expr) {

		if (expr instanceof OWLClass) {

			return create((OWLClass)expr);
		}

		return queryables.create(toDynamicPatternSource(expr));
	}

	Queryable create(OWLClass cls) {

		return queryables.create(names.get(cls));
	}

	Queryable create(OWLNamedIndividual ind) {

		return queryables.create(names.get(ind));
	}

	private PatternSource toDynamicPatternSource(OWLClassExpression expr) {

		return coreBuilder.createPatternSource(converter.toQueryNode(expr));
	}
}
