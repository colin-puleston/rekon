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

	private DynamicOpsHandlers dynamicOpsHandlers;

	IndividualOps(DynamicOpsHandlers dynamicOpsHandlers) {

		this.dynamicOpsHandlers = dynamicOpsHandlers;
	}

	Set<Set<OWLNamedIndividual>> getIndividuals(OWLClassExpression expr, boolean direct) {

		DynamicOpsHandler hdlr = dynamicOpsHandlers.getFor(expr);

		return retrieveEntities(toEquivGroups(hdlr.getIndividuals(direct)));
	}

	boolean equivalent(OWLNamedIndividual inObject1, OWLNamedIndividual inObject2) {

		DynamicOpsHandler hdlr1 = dynamicOpsHandlers.getFor(inObject1);
		DynamicOpsHandler hdlr2 = dynamicOpsHandlers.getFor(inObject2);

		return hdlr1.equivalentTo(hdlr2);
	}

	boolean subsumption(OWLNamedIndividual inSup, OWLNamedIndividual inSub) {

		DynamicOpsHandler supHdlr = dynamicOpsHandlers.getFor(inSup);
		DynamicOpsHandler subHdlr = dynamicOpsHandlers.getFor(inSub);

		return supHdlr.subsumes(subHdlr);
	}

	boolean hasType(OWLNamedIndividual ind, OWLClassExpression type) {

		DynamicOpsHandler typeHdlr = dynamicOpsHandlers.getFor(type);
		DynamicOpsHandler indHdlr = dynamicOpsHandlers.getFor(ind);

		return typeHdlr.subsumes(indHdlr);
	}

	Names getEquivalentNames(OWLNamedIndividual inObject) {

		return dynamicOpsHandlers.getFor(inObject).getEquivalents();
	}

	Class<OWLNamedIndividual> getEntityType() {

		return OWLNamedIndividual.class;
	}

	private Collection<Names> toEquivGroups(Names names) {

		return new EquivalentsGrouper().group(names);
	}
}
