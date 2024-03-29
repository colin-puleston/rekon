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
public abstract class NumberRange extends DataValue {

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

	boolean subsumesOther(Value v) {

		NumberRange r = asTypeRange(v);

		return r != null && subsumesRange(r);
	}

	void render(PatternRenderer r) {

		r.addLine("[" + renderLimit(min) + ", " + renderLimit(max) + "]");
	}

	abstract NumberRange asTypeRange(Value v);

	abstract boolean notMoreThan(Number test, Number limit);

	private boolean subsumesRange(NumberRange n) {

		return subsumesMin(n) && subsumesMax(n);
	}

	private boolean subsumesMin(NumberRange n) {

		if (min == null) {

			return true;
		}

		if (n.min == null) {

			return false;
		}

		return notMoreThan(min, n.min);
	}

	private boolean subsumesMax(NumberRange n) {

		if (max == null) {

			return true;
		}

		if (n.max == null) {

			return false;
		}

		return notMoreThan(n.max, max);
	}

	private String renderLimit(Number limit) {

		return limit == null ? "?" : limit.toString();
	}
}
