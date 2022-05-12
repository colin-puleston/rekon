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
class Timer {

	static private Map<String, Instance> instances = new HashMap<String, Instance>();

	static class Instance {

		private String prefix;

		private long startMillis = System.currentTimeMillis();

		Instance() {

			this(null);
		}

		Instance(String prefix) {

			this.prefix = prefix;

			instances.put(prefix, this);
		}

		void show() {

			doShow(prefix != null ? prefix : "TIME");
		}

		void show(String suffix) {

			doShow(prefix != null ? (prefix + "-" + suffix) : suffix);
		}

		private void doShow(String title) {

			long durationSecs = (System.currentTimeMillis() - startMillis) / 1000;

			System.out.println("\n" + title + " TIME: " + durationSecs);
		}
	}

	static void start(String prefix) {

		new Instance(prefix);
	}

	static void show(String prefix) {

		instances.get(prefix).show();
	}

	static void show(String prefix, String suffix) {

		instances.get(prefix).show(suffix);
	}

	static void stop(String prefix) {

		show(prefix);

		instances.remove(prefix);
	}

	static void stop(String prefix, String suffix) {

		show(prefix, suffix);

		instances.remove(prefix);
	}
}
