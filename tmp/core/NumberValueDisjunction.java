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
class NumberValueDisjunction  {

	static private abstract class Limit implements Comparable<Limit> {

		final NumberValue value;
		final NumberRange range;

		public int compareTo(Limit other) {

			return Double.compare(compareValue(), other.compareValue());
		}

		Limit(NumberValue value, NumberRange range) {

			this.value = value;
			this.range = range;
		}

		abstract Number getLimit();

		abstract boolean minValue();

		boolean maxValue() {

			return !minValue();
		}

		private double compareValue() {

			return getLimit().doubleValue();
		}
	}

	static private class Min extends Limit {

		Min(NumberValue value, NumberRange range) {

			super(value, range);
		}

		Number getLimit() {

			return range.getMin();
		}

		boolean minValue() {

			return true;
		}
	}

	static private class Max extends Limit {

		Max(NumberValue value, NumberRange range) {

			super(value, range);
		}

		Number getLimit() {

			return range.getMax();
		}

		boolean minValue() {

			return false;
		}
	}

	private SortedSet<Limit> limits = new TreeSet<Limit>();
	private List<NumberRange> disjunctRanges = new ArrayList<NumberRange>();

	private Set<NumberValue> disjunctRangeSources = new HashSet<NumberValue>();

	NumberValueDisjunction(Collection<NumberValue> disjuncts) {

		for (NumberValue v : disjuncts) {

			for (NumberRange r : v.getDisjunctRanges()) {

				limits.add(new Min(v, r));
				limits.add(new Max(v, r));
			}
		}
	}

	NumberValue asNumberValue() {

		NumberValue s = asSingleRangeInputValue();

		if (s != null) {

			return s;
		}

		createDisjunctRanges();

		if (disjunctRangeSources.size() == 1) {

			return disjunctRangeSources.iterator().next();
		}

		return new NumberRangeDisjunction(disjunctRanges);
	}

	private NumberValue asSingleRangeInputValue() {

		List<Limit> limitList = new ArrayList<Limit>(limits);

		Limit min = limitList.get(0);
		Limit max = limitList.get(limitList.size() - 1);

		if (min.range == max.range) {

			return min.value;
		}

		return null;
	}

	private void createDisjunctRanges() {

		Limit min = null;
		Limit max = null;

		for (Limit l : limits) {

			if (l.minValue()) {

				if (min == null) {

					min = l;
				}
				else {

					if (max != null) {

						addDisjunctRange(min, max);

						min = l;
						max = null;
					}
				}
			}
			else {

				max = l;
			}
		}

		addDisjunctRange(min, max);
	}

	private void addDisjunctRange(Limit min, Limit max) {

		disjunctRangeSources.add(min.value);
		disjunctRangeSources.add(max.value);

		disjunctRanges.add(new NumberRange(min.getLimit(), max.getLimit()));
	}
}
