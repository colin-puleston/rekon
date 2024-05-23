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

/**
 * @author Colin Puleston
 */
public abstract class NumberRange<R extends NumberRange<?>> extends DataValue {

	private Number min;
	private Number max;

	NumberRange(Number min, Number max) {

		this.min = min;
		this.max = max;
	}

	<N extends Number>N toNumber(Class<N> type) {

		if (min.equals(max)) {

			return type.cast(min);
		}

		throw new RuntimeException("Does not represent exact value: " + this);
	}

	R unionWith(R r) {

		NumberRange<?> n = r;

		if (moreThan(min, n.max) || moreThan(n.min, max)) {

			return null;
		}

		return createTypeRange(smallerOf(min, n.min), largerOf(max, n.max));
	}

	boolean subsumesOther(Value v) {

		R r = asTypeRange(v);

		return r != null && subsumesRange(r);
	}

	void render(PatternRenderer r) {

		r.addLine("[" + renderLimit(min) + ", " + renderLimit(max) + "]");
	}

	abstract R asTypeRange(Value v);

	abstract R createTypeRange(Number min, Number max);

	abstract boolean moreThanFinite(Number test, Number limit);

	abstract boolean notMoreThanFinite(Number test, Number limit);

	private boolean subsumesRange(NumberRange<?> n) {

		return notMoreThan(min, n.min) && notLessThan(max, n.max);
	}

	private Number smallerOf(Number n1, Number n2) {

		return notMoreThan(n1, n2) ? n1 : n2;
	}

	private Number largerOf(Number n1, Number n2) {

		return notLessThan(n1, n2) ? n1 : n2;
	}

	private boolean moreThan(Number test, Number limit) {

		if (limit == null) {

			return false;
		}

		if (test == null) {

			return true;
		}

		return moreThanFinite(test, limit);
	}

	private boolean notMoreThan(Number test, Number limit) {

		if (test == null) {

			return true;
		}

		if (limit == null) {

			return false;
		}

		return notMoreThanFinite(test, limit);
	}

	private boolean notLessThan(Number test, Number limit) {

		if (test == null) {

			return true;
		}

		if (limit == null) {

			return false;
		}

		return notMoreThanFinite(limit, test);
	}

	private String renderLimit(Number limit) {

		return limit == null ? "?" : limit.toString();
	}
}
