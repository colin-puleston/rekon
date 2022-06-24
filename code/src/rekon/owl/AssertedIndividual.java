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

import java.util.*;

import org.semanticweb.owlapi.model.*;

/**
 * @author Colin Puleston
 */
class AssertedIndividual extends AssertedEntity<OWLNamedIndividual> {

	private Set<OWLClass> types = new HashSet<OWLClass>();
	private Set<OWLClassExpression> typeExprs = new HashSet<OWLClassExpression>();

	private Set<AssertedObjectValue> objectValues = new HashSet<AssertedObjectValue>();
	private Set<AssertedDataValue> dataValues = new HashSet<AssertedDataValue>();

	AssertedIndividual(OWLNamedIndividual entity) {

		super(entity);
	}

	void addTypeExpr(OWLClassExpression type) {

		if (type instanceof OWLClass) {

			types.add((OWLClass)type);
		}
		else {

			typeExprs.add(type);
		}
	}

	void addObjectValue(AssertedObjectValue value) {

		objectValues.add(value);
	}

	void addDataValue(AssertedDataValue value) {

		dataValues.add(value);
	}

	Collection<OWLClass> getTypes() {

		return types;
	}

	Collection<OWLClassExpression> getTypeExprs() {

		return typeExprs;
	}

	Collection<AssertedObjectValue> getObjectValues() {

		return objectValues;
	}

	Collection<AssertedDataValue> getDataValues() {

		return dataValues;
	}
}
