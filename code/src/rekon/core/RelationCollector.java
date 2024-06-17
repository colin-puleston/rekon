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
class RelationCollector {

	private Set<Relation> collected = new HashSet<Relation>();
	private boolean anyAdditions = false;

	RelationCollector() {
	}

	RelationCollector(Set<Relation> initialCollection) {

		absorbAll(initialCollection, true);
	}

	void absorbAll(Collection<Relation> relations) {

		absorbAll(relations, false);
	}

	void absorb(Relation r) {

		absorb(r, false);
	}

	boolean checkAdditions() {

		if (anyAdditions) {

			onAdditions();
		}

		return anyAdditions;
	}

	void onAdditions() {
	}

	Set<Relation> getCollected() {

		return collected;
	}

	Collection<Relation> copyCollected() {

		return new ArrayList<Relation>(collected);
	}

	private void absorbAll(Collection<Relation> relations, boolean initialising) {

		for (Relation r : relations) {

			absorb(r, initialising);
		}
	}

	private void absorb(Relation r, boolean initialising) {

		if (collected.contains(r)) {

			return;
		}

		for (Relation cr : copyCollected()) {

			if (r.subsumes(cr)) {

				return;
			}

			if (cr.subsumes(r)) {

				collected.remove(cr);
			}
		}

		collected.add(r);

		if (!initialising) {

			anyAdditions |= true;
		}
	}
}
