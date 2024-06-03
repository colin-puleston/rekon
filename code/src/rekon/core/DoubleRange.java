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
public class DoubleRange extends NumberRange<DoubleRange> {

	static public final DoubleRange UNCONSTRAINED = new DoubleRange(null, null);

	public DoubleRange(Double exact) {

		this(exact, exact);
	}

	public DoubleRange(Double min, Double max) {

		super(min, max);
	}

	DoubleRange asDoubleRange() {

		return this;
	}

	DoubleRange asTypeRange(Value v) {

		return v.asDoubleRange();
	}

	DoubleRange createTypeRange(Number min, Number max) {

		return new DoubleRange(min.doubleValue(), max.doubleValue());
	}

	boolean moreThanFinite(Number test, Number limit) {

		return test.doubleValue() > limit.doubleValue();
	}

	boolean notMoreThanFinite(Number test, Number limit) {

		return test.doubleValue() <= limit.doubleValue();
	}
}
