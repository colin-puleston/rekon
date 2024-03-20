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

import rekon.util.*;

/**
 * @author Colin Puleston
 */
class ProgressLogger {

	static final ProgressLogger SINGLETON = new ProgressLogger();
	static final Option OPTION = new Option(false, "logging.progress");

	void logMajorStartupStartPoint(String text) {

		logSeparatorLine();
		logMajorStartupPoint(text);
	}

	void logMajorStartupEndPoint(String text) {

		logSeparatorLine();
		logMajorStartupPoint(text);
		logSeparatorLine();
	}

	void logMinorStartupPoint(String text) {

		logLine("  " + text);
	}

	private void logMajorStartupPoint(String text) {

		logLine("REKON: " + text);
	}

	private void logSeparatorLine() {

		logLine("");
	}

	private void logLine(String line) {

		if (OPTION.enabled()) {

			Logger.SINGLETON.logLine(line);
		}
	}
}
