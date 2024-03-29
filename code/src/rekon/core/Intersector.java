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

	private class SmallestFirstComparator implements Comparator<C> {

		public int compare(C first, C second) {

			int diff = collectionSize(first) - collectionSize(second);

			return diff != 0 ? diff : 1;
		}
	}

	C intersectSets(Collection<C> sets) {

		S intersection = creteEmptySet();

		if (sets.size() == 0) {

			return intersection;
		}

		if (sets.size() == 1) {

			return sets.iterator().next();
		}

		Collection<C> smallestFirst = sortSmallestFirst(sets);

		if (emptyCollection(smallestFirst.iterator().next())) {

			return intersection;
		}

		for (C set : smallestFirst) {

			if (emptyCollection(set)) {

				return creteEmptySet();
			}

			if (emptyCollection(intersection)) {

				addAll(intersection, set);
			}
			else {

				retainAll(intersection, set);

				if (emptyCollection(intersection)) {

					break;
				}
			}
		}

		return intersection;
	}

	abstract S creteEmptySet();

	abstract int collectionSize(C collection);

	abstract boolean emptyCollection(C set);

	abstract void addAll(S set, C adding);

	abstract void retainAll(S set, C retaining);

	private Collection<C> sortSmallestFirst(Collection<? extends C> in) {

		Collection<C> out = new TreeSet<C>(new SmallestFirstComparator());

		out.addAll(in);

		return out;
	}
}
