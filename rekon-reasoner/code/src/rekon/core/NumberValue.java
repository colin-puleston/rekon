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
public abstract class NumberValue extends DataValue {

	static public NumberValue create(Number exact) {

		return new SingleNumberRange(exact);
	}

	static public NumberValue create(Number min, Number max) {

		return new SingleNumberRange(min, max);
	}

	static public NumberValue create(Collection<NumberValue> disjuncts) {

		return new NumberValueDisjunction(disjuncts).asNumberValue();
	}

	public boolean equals(Object other) {

		return other instanceof NumberValue && equalsNumberValue((NumberValue)other);
	}

	public int hashCode() {

		int hash = 0;

		for (NumberRange r : getDisjunctRanges()) {

			hash += r.hashCode();
		}

		return hash;
	}

	public String toString() {

		return renderRanges();
	}

	NumberValue asNumberValue() {

		return this;
	}

	boolean subsumesOther(Value v) {

		return v instanceof NumberValue && subsumesAllRanges((NumberValue)v);
	}

	void render(PatternRenderer r) {

		r.addLine(renderRanges());
	}

	abstract List<NumberRange> getDisjunctRanges();

	private boolean equalsNumberValue(NumberValue other) {

		List<NumberRange> rs = getDisjunctRanges();
		List<NumberRange> ors = other.getDisjunctRanges();

		if (rs.size() != ors.size()) {

			return false;
		}

		int i = 0;

		for (NumberRange r : rs) {

			if (!r.equals(ors.get(i++))) {

				return false;
			}
		}

		return true;
	}

	private boolean subsumesAllRanges(NumberValue v) {

		for (NumberRange r : v.getDisjunctRanges()) {

			if (!subsumesRange(r)) {

				return false;
			}
		}

		return true;
	}

	private boolean subsumesRange(NumberRange r) {

		for (NumberRange tr : getDisjunctRanges()) {

			if (tr.subsumesRange(r)) {

				return true;
			}
		}

		return false;
	}

	private String renderRanges() {

		StringBuilder s = new StringBuilder();
		boolean first = true;

		for (NumberRange r : getDisjunctRanges()) {

			if (first) {

				first = false;
			}
			else {

				s.append(',');
			}

			s.append(r.renderRange());
		}

		return s.toString();
	}
}
