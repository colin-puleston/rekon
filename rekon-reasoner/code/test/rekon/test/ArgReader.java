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

class ArgReader {

	private TestConfig<?> config;
	private Iterator<String> args;

	ArgReader(TestConfig<?> config, String[] args) {

		this.config = config;
		this.args = Arrays.asList(args).iterator();
	}

	ArgReader(TestConfig<?> config, String compoundArg, int minSections, int maxSections) {

		this(config, compoundArg.split(":"), minSections, maxSections);
	}

	boolean hasNext() {

		return args.hasNext();
	}

	String readNext() {

		if (args.hasNext()) {

			return args.next();
		}

		config.exitOnError("Bad arguments!");

		return "";
	}

	Integer parseNextInt() {

		String value = readNext();

		try {

			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {

			config.exitOnError("Bad integer string: " + value);

			return 0;
		}
	}

	Integer parseNextInt(int defaultValue) {

		return args.hasNext() ? parseNextInt() : defaultValue;
	}

	private ArgReader(TestConfig<?> config, String[] splitArg, int minSections, int maxSections) {

		this(config, splitArg);

		if (splitArg.length < minSections || splitArg.length > maxSections) {

			config.exitOnError("Bad compound arg: " + splitArg);
		}
	}
}
