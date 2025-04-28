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

import com.carrotsearch.hppc.*;

/**
 * @author Colin Puleston
 */
abstract class IntegerCollector {

	private boolean allOptionsResult = false;

	void absorbAllOptions() {

		if (!allOptionsResult && enableSettingToAllOptions()) {

			allOptionsResult = true;
		}
	}

	abstract void absorb(IntHashSet integers);

	void absorbFrom(IntegerCollector collector) {

		if (collector.allOptionsResult()) {

			absorbAllOptions();
		}
		else {

			absorb(collector.getSubsetResult());
		}
	}

	void absorbInto(IntegerIntersection intersection) {

		if (allOptionsResult) {

			intersection.absorbAllOptions();
		}
		else {

			absorbSubsetResultInto(intersection);
		}
	}

	boolean allOptionsResult() {

		return allOptionsResult;
	}

	boolean emptyResult() {

		return !allOptionsResult && emptySubsetResult();
	}

	boolean emptyResultIfExcluded(Integer exclude) {

		if (allOptionsResult) {

			return false;
		}

		if (exclude != null) {

			return emptySubsetResultIfExcluded(exclude);
		}

		return emptySubsetResult();
	}

	abstract IntHashSet getSubsetResult();

	abstract boolean emptySubsetResult();

	abstract boolean emptySubsetResultIfExcluded(int exclude);

	abstract boolean enableSettingToAllOptions();

	abstract void absorbSubsetResultInto(IntegerIntersection intersection);

	void ensureNotAllOptionsResult() {

		allOptionsResult &= false;
	}
}
