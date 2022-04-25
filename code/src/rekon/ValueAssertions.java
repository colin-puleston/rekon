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

package rekon;

import java.util.*;

import org.semanticweb.owlapi.model.*;

/**
 * @author Colin Puleston
 */
abstract class ValueAssertions
					<P extends OWLProperty,
					PE extends OWLPropertyExpression,
					V extends OWLObject,
					A extends ValueAssertion<P, V>> {

	private List<A> assertions = new ArrayList<A>();

	ValueAssertions(Map<PE, Collection<V>> byPropertyExpr, Class<P> propertyCls) {

		for (PE pe : byPropertyExpr.keySet()) {

			if (propertyCls.isAssignableFrom(pe.getClass())) {

				for (V v : byPropertyExpr.get(pe)) {

					assertions.add(create(propertyCls.cast(pe), v));
				}
			}
		}
	}

	List<A> getAll() {

		return assertions;
	}

	abstract A create(P property, V value);
}