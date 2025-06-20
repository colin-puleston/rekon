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

import com.carrotsearch.hppc.*;

/**
 * @author Colin Puleston
 */
class IntegerIntersection extends IntegerCollector {

	static private final IntSetIntersector intersector = new IntSetIntersector();

	static private class IntSetIntersector extends Intersector<IntHashSet, IntHashSet> {

		IntHashSet creteEmptySet() {

			return new IntHashSet();
		}

		int collectionSize(IntHashSet collection) {

			return collection.size();
		}

		boolean emptyCollection(IntHashSet set) {

			return set.isEmpty();
		}

		void addAll(IntHashSet set, IntHashSet adding) {

			set.addAll(adding);
		}

		void retainAll(IntHashSet set, IntHashSet retaining) {

			set.retainAll(retaining);
		}
	}

	private List<IntHashSet> integerSets = new ArrayList<IntHashSet>();

	private boolean anySubsetsAbsorbed = false;

	void absorb(IntHashSet integers) {

		if (integers.isEmpty()) {

			integerSets.clear();
		}
		else {

			if (potentiallyNonEmptySubsetResult()) {

				integerSets.add(integers);
			}
		}

		onAbsorbedSubset();
	}

	IntHashSet getSubsetResult() {

		return intersector.intersectSets(integerSets);
	}

	boolean emptySubsetResult() {

		return integerSets.isEmpty();
	}

	boolean emptySubsetResultIfExcluded(int exclude) {

		for (IntHashSet integers : integerSets) {

			if (integers.size() == 1 && integers.contains(exclude)) {

				return true;
			}
		}

		return false;
	}

	boolean potentiallyNonEmptySubsetResult() {

		return !anySubsetsAbsorbed || !integerSets.isEmpty();
	}

	boolean enableSettingToAllOptions() {

		return !anySubsetsAbsorbed;
	}

	void absorbSubsetResultInto(IntegerIntersection intersection) {

		intersection.absorbSubsetResultFrom(this);
	}

	private void absorbSubsetResultFrom(IntegerIntersection intersection) {

		integerSets.addAll(intersection.integerSets);

		onAbsorbedSubset();
	}

	private void onAbsorbedSubset() {

		anySubsetsAbsorbed |= true;

		ensureNotAllOptionsResult();
	}
}
