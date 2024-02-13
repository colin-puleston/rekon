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

package rekon.owl;

/**
 * @author Colin Puleston
 */
class Logger {

	static final Logger SINGLETON = new Logger();

	static private final String LOGGING_SYSTEM_PROPERTY = "rekon.logging";

	static private boolean loggingOn = false;

	static {

		loggingOn = Boolean.valueOf(System.getProperty(LOGGING_SYSTEM_PROPERTY));
	}

	void logOutOfScopeWarningLine(String entity) {

		logOutOfScopeWarningLine(entity, false);
	}

	void logOutOfScopeWarningLine(String entity, boolean inContext) {

		String qualifier = inContext ? " in context" : "";

		logWarningLine(entity + " out-of-scope" + qualifier + "...");
	}

	void logSeparatorLine() {

		logLine("");
	}

	void logLine(String line) {

		if (loggingOn) {

			System.out.println(line);
		}
	}

	private void logWarningLine(String warning) {

		logLine("REKON WARNING: " + warning);
	}
}
