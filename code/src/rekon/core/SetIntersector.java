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
class SetIntersector {

	static private class SmallestFirstComparator<E> implements Comparator<Collection<E>> {

		public int compare(Collection<E> first, Collection<E> second) {

			return first.size() - second.size();
		}
	}

	static <E>Collection<E> intersect(Collection<? extends Collection<E>> sets) {

		if (sets.size() == 1) {

			return sets.iterator().next();
		}

		Set<E> intersection = new HashSet<E>();

		for (Collection<E> set : sort(sets)) {

			if (intersection.isEmpty()) {

				intersection.addAll(set);
			}
			else {

				intersection.retainAll(set);

				if (intersection.isEmpty()) {

					break;
				}
			}
		}

		return intersection;
	}

	static private <E>Collection<Collection<E>> sort(Collection<? extends Collection<E>> in) {

		Collection<Collection<E>> out = new TreeSet<Collection<E>>(new SmallestFirstComparator<E>());

		out.addAll(in);

		return out;
	}
}
