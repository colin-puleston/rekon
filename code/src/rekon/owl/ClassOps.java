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
class ClassOps extends HierarchyEntityOps<OWLClass, OWLClassExpression> {

	private MappedNames names;
	private QueriablesAccessor queriables;

	ClassOps(
		OWLDataFactory factory,
		MappedNames names,
		QueriablesAccessor queriables) {

		super(factory.getOWLThing(), factory.getOWLNothing());

		this.names = names;
		this.queriables = queriables;
	}

	Set<Set<OWLClass>> getTypes(OWLNamedIndividual ind, boolean direct) {

		return toEquivGroups(queriables.create(ind).getSupers(direct));
	}

	boolean equivalent(OWLClassExpression inObject1, OWLClassExpression inObject2) {

		return queriables.create(inObject1).equivalentTo(queriables.create(inObject2));
	}

	boolean subsumption(OWLClassExpression inSup, OWLClassExpression inSub) {

		return queriables.create(inSup).subsumes(queriables.create(inSub));
	}

	Names getEquivalentNames(OWLClassExpression inObject) {

		return queriables.create(inObject).getEquivalents();
	}

	Names getSuperNames(OWLClassExpression inObject, boolean direct) {

		return queriables.create(inObject).getSupers(direct);
	}

	Names getSubNames(OWLClassExpression inObject, boolean direct) {

		return queriables.create(inObject).getSubs(direct);
	}

	Iterable<? extends Name> getAllEntityNames() {

		return names.getClassNodes();
	}

	Names getDirectEntitySubs(Name name) {

		return name.getSubs(ClassNode.class, true);
	}

	Class<OWLClass> getEntityType() {

		return OWLClass.class;
	}

	boolean insertedEntity(OWLClass entity) {

		return NoValueSubstitutions.isNoValueClass(entity);
	}
}
