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
abstract class EntityOps<I extends OWLObject, E extends I> {

	private EntityRetriever defaultRetriever = new EntityRetriever();
	private EquivalenceChecker equivalenceChecker = new EquivalenceChecker();

	class EntityRetriever {

		Set<E> toSingleGroup(Names names) {

			return toEntityGroup(names);
		}

		Set<Set<E>> toEquivGroups(Names names) {

			Set<Set<E>> groups = new HashSet<Set<E>>();

			for (Names nameGroup : groupEquivs(names)) {

				Set<E> entities = toEntityGroup(nameGroup);

				if (!entities.isEmpty()) {

					groups.add(entities);
				}
			}

			return groups;
		}

		private Collection<Names> groupEquivs(Names names) {

			return new EquivalentsGrouper().group(names);
		}

		private Set<E> toEntityGroup(Names names) {

			Set<E> entities = new HashSet<E>();

			for (Name n : names) {

				E e = toMappedEntity(n);

				if (!insertedEntity(e)) {

					entities.add(e);
				}
			}

			return entities;
		}
	}

	private class EquivalenceChecker {

		boolean check(Set<I> inObjects) {

			if (inObjects.size() < 2) {

				return true;
			}

			Iterator<I> i = inObjects.iterator();
			I first = i.next();

			do {

				if (!equivalent(first, i.next())) {

					return false;
				}
			}
			while (i.hasNext());

			return true;
		}
	}

	Set<E> getEquivalents(I inObject) {

		return defaultRetriever.toSingleGroup(getEquivalentNames(inObject));
	}

	boolean equivalents(Set<I> inObjects) {

		return equivalenceChecker.check(inObjects);
	}

	Set<Set<E>> toEquivGroups(Names names) {

		return defaultRetriever.toEquivGroups(names);
	}

	abstract Names getEquivalentNames(I inObject);

	abstract boolean equivalent(I inObject1, I inObject2);

	abstract E toMappedEntity(Name name);

	boolean insertedEntity(E entity) {

		return false;
	}
}
