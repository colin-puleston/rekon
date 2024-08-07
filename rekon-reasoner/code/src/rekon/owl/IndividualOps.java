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
	private QueryablesAccessor queryables;

	IndividualOps(
		OWLDataFactory factory,
		MappedNames names,
		QueryablesAccessor queryables) {

		this.factory = factory;
		this.names = names;
		this.queryables = queryables;
	}

	Set<Set<OWLNamedIndividual>> getIndividuals(OWLClassExpression expr, boolean direct) {

		return toEquivGroups(queryables.create(expr).getIndividuals(direct));
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

		return queryables.create(inObject1).equivalentTo(queryables.create(inObject2));
	}

	boolean subsumption(OWLNamedIndividual inSup, OWLNamedIndividual inSub) {

		return queryables.create(inSup).subsumes(queryables.create(inSub));
	}

	boolean hasType(OWLNamedIndividual ind, OWLClassExpression type) {

		return queryables.create(type).subsumes(queryables.create(ind));
	}

	Names getEquivalentNames(OWLNamedIndividual inObject) {

		return queryables.create(inObject).getEquivalents();
	}

	OWLNamedIndividual toMappedEntity(Name name) {

		return MappedNames.toMappedEntity(name, OWLNamedIndividual.class);
	}
}
