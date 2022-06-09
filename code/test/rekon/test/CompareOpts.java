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

package rekon.test;

import java.util.*;

class CompareOpts extends TestOpts {

	final Input input;
	final Output output;
	final int maxQueries;

	enum Input {

		CLASS, QUERY;
	}

	enum Output {

		EQUIVS, SUPS, SUBS, ANCS, DECS, ALL;

		Output[] toAtoms() {

			if (this == ALL) {

				return Arrays.copyOf(values(), values().length - 1);
			}

			return new Output[]{this};
		}
	}

	CompareOpts(String arg) {

		String[] vals = parseArg(arg, 2, 3);

		input = Input.valueOf(vals[0]);
		output = Output.valueOf(vals[1]);
		maxQueries = vals.length == 3 ? parseInt(vals[2]) : -1;
	}
}
