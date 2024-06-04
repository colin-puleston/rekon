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
class NumberRange {

	private Number min;
	private Number max;

	public String toString() {

		return renderRange();
	}

	NumberRange(Number min, Number max) {

		this.min = min;
		this.max = max;
	}

	Number getMin() {

		return min;
	}

	Number getMax() {

		return max;
	}

	boolean subsumesRange(NumberRange r) {

		return notMoreThan(min, r.min) && notLessThan(max, r.max);
	}

	String renderRange() {

		return "[" + renderLimit(min) + ", " + renderLimit(max) + "]";
	}

	private boolean notMoreThan(Number test, Number limit) {

		return test.doubleValue() <= limit.doubleValue();
	}

	private boolean notLessThan(Number test, Number limit) {

		return test.doubleValue() >= limit.doubleValue();
	}

	private String renderLimit(Number limit) {

		return limit == null ? "?" : limit.toString();
	}
}
