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

package rekon.core;

import java.util.*;

/**
 * @author Colin Puleston
 */
abstract class Intersector<C, S extends C> {

	private List<C> sets = new ArrayList<C>();

	private class SmallestFirstComparator implements Comparator<C> {

		public int compare(C first, C second) {

			int diff = collectionSize(first) - collectionSize(second);

			return diff != 0 ? diff : 1;
		}
	}

	C intersectSets(Collection<C> sets) {

		if (sets.size() == 1) {

			return sets.iterator().next();
		}

		S intersection = creteEmptySet();

		for (C set : sort(sets)) {

			if (emptySet(intersection)) {

				addAll(intersection, set);
			}
			else {

				retainAll(intersection, set);

				if (emptySet(intersection)) {

					break;
				}
			}
		}

		return intersection;
	}

	abstract S creteEmptySet();

	abstract int collectionSize(C collection);

	abstract boolean emptySet(S set);

	abstract void addAll(S set, C adding);

	abstract void retainAll(S set, C retaining);

	private Collection<C> sort(Collection<? extends C> in) {

		Collection<C> out = new TreeSet<C>(new SmallestFirstComparator());

		out.addAll(in);

		return out;
	}
}

/*

class IntSetIntersector extends Intersector<TIntSet, TIntSet> {

	IntSetIntersector(Collection<TIntSet> sets) {

		addSets(sets);
	}

	TIntSet creteEmptySet() {

		return new TIntHashSet();
	}

	int collectionSize(TIntSet collection) {

		return collection.size();
	}

	boolean emptySet(TIntSet set) {

		return set.isEmpty();
	}

	void addAll(TIntSet set, TIntSet adding) {

		set.addAll(adding);
	}

	void retainAll(TIntSet set, TIntSet retaining) {

		set.retainAll(retaining);
	}
}

class SetIntersector<E> extends Intersector<Collection<E>, Set<E>> {

	Set<E> creteEmptySet() {

		return new THashSet<E>();
	}

	int collectionSize(Collection<E> collection) {

		return collection.size();
	}

	boolean emptySet(Set<E> set) {

		return set.isEmpty();
	}

	void addAll(Set<E> set, Collection<E> adding) {

		set.addAll(adding);
	}

	void retainAll(Set<E> set, Collection<E> retaining) {

		set.retainAll(retaining);
	}
}

*/