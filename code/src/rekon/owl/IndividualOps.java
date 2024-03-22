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

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class IndividualOps extends EntityOps<OWLNamedIndividual, OWLNamedIndividual> {

	private OWLDataFactory factory;

	private MappedNames names;
	private QueriablesAccessor queriables;

	IndividualOps(
		OWLDataFactory factory,
		MappedNames names,
		QueriablesAccessor queriables) {

		this.factory = factory;
		this.names = names;
		this.queriables = queriables;
	}

	Set<Set<OWLNamedIndividual>> getIndividuals(OWLClassExpression expr, boolean direct) {

		return toEquivGroups(queriables.create(expr).getIndividuals(direct));
	}

	Set<Set<OWLNamedIndividual>> getObjectValues(OWLNamedIndividual ind, OWLObjectProperty prop) {

		IndividualNode i = names.get(ind);

		if (i != null) {

			PropertyX p = names.get(prop);

			if (p != null) {

				return toEquivGroups(i.getIndividualValues(p));
			}
		}

		return Collections.emptySet();
	}

	Set<OWLLiteral> getDataValues(OWLNamedIndividual ind, OWLDataProperty prop) {

		return null;
	}

	boolean equivalent(OWLNamedIndividual inObject1, OWLNamedIndividual inObject2) {

		return queriables.create(inObject1).equivalentTo(queriables.create(inObject2));
	}

	boolean subsumption(OWLNamedIndividual inSup, OWLNamedIndividual inSub) {

		return queriables.create(inSup).subsumes(queriables.create(inSub));
	}

	boolean hasType(OWLNamedIndividual ind, OWLClassExpression type) {

		return queriables.create(type).subsumes(queriables.create(ind));
	}

	Names getEquivalentNames(OWLNamedIndividual inObject) {

		return queriables.create(inObject).getEquivalents();
	}

	Class<OWLNamedIndividual> getEntityType() {

		return OWLNamedIndividual.class;
	}

	private Set<OWLLiteral> toLiterals(List<DataValue> values) {

		Set<OWLLiteral> literals = new HashSet<OWLLiteral>();

		for (DataValue v : values) {

			literals.add(toLiteral(v));
		}

		return literals;
	}

	private OWLLiteral toLiteral(DataValue value) {

		if (value instanceof BooleanValue) {

			return factory.getOWLLiteral(value.toBoolean());
		}

		if (value instanceof IntegerRange) {

			return factory.getOWLLiteral(value.toInteger());
		}

		if (value instanceof FloatRange) {

			return factory.getOWLLiteral(value.toFloat());
		}

		if (value instanceof DoubleRange) {

			return factory.getOWLLiteral(value.toDouble());
		}

		throw new Error("Unrecognised DataValue type: " + value.getClass());
	}
}
