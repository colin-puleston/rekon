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
public class Timer {

	static private Map<String, Instance> instances = new HashMap<String, Instance>();

	static private class Instance {

		private String prefix;
		private boolean active;

		private long startMillis = System.currentTimeMillis();
		private long totalMillis = 0;

		Instance(String prefix, boolean active) {

			this.prefix = prefix;
			this.active = active;

			instances.put(prefix, this);
		}

		void pause() {

			active = false;

			totalMillis += currentPointMillis();
		}

		void restart() {

			active = true;

			startMillis = System.currentTimeMillis();
		}

		void show(String suffix) {

			startMillis = System.currentTimeMillis();

			if (active) {

				totalMillis += currentPointMillis();
			}

			System.out.println("\n" + getTitle(suffix) + " TIME: " + (totalMillis / 1000));
		}

		boolean active() {

			return active;
		}

		private long currentPointMillis() {

			return System.currentTimeMillis() - startMillis;
		}

		private String getTitle(String suffix) {

			return suffix != null ? (prefix + "-" + suffix) : prefix;
		}
	}

	static public void start(String prefix) {

		new Instance(prefix, true);
	}

	static public void startPaused(String prefix) {

		new Instance(prefix, false);
	}

	static public void pause(String prefix) {

		getActive(prefix).pause();
	}

	static public void restart(String prefix) {

		getPaused(prefix).restart();
	}

	static public void show(String prefix) {

		getActive(prefix).show(null);
	}

	static public void show(String prefix, String suffix) {

		getActive(prefix).show(suffix);
	}

	static public void stop(String prefix) {

		remove(prefix).show(null);
	}

	static public void stop(String prefix, String suffix) {

		remove(prefix).show(suffix);
	}

	static private Instance getActive(String prefix) {

		return get(prefix, true, "active");
	}

	static private Instance getPaused(String prefix) {

		return get(prefix, false, "paused");
	}

	static private Instance get(String prefix, boolean active, String type) {

		Instance i = instances.get(prefix);

		if (i != null && i.active() == active) {

			return i;
		}

		throw new Error("No " + type + " instance: " + prefix);
	}

	static private Instance remove(String prefix) {

		Instance i = instances.remove(prefix);

		if (i != null) {

			return i;
		}

		throw new Error("No current instance: " + prefix);
	}
}
