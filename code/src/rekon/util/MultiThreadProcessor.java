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

package rekon.util;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author Colin Puleston
 */
public abstract class MultiThreadProcessor<E> {

	static public final Option OPTION = new Option(true, "multithread");

	private int totalThreads = Runtime.getRuntime().availableProcessors();

	private class ThreadProcessor extends ForkJoinTask<Boolean> {

		static private final long serialVersionUID = -1;

		private int threadIndex;

		public Boolean getRawResult() {

			return true;
		}

		protected void setRawResult(Boolean r) {
		}

		protected boolean exec() {

			execThreadProcess(totalThreads, threadIndex);

			return true;
		}

		ThreadProcessor(int threadIndex) {

			this.threadIndex = threadIndex;
		}
	}

	public void setMaxProcesses(int maxProcesses) {

		if (maxProcesses < totalThreads) {

			totalThreads = maxProcesses;
		}
	}

	public void execProcesses() {

		if (OPTION.enabled()) {

			ForkJoinTask.invokeAll(createThreadProcessors());
		}
		else {

			execAllInSingleThread();
		}
	}

	protected abstract void execThreadProcess(int totalThreads, int threadIndex);

	protected abstract void execAllInSingleThread();

	private List<ThreadProcessor> createThreadProcessors() {

		List<ThreadProcessor> procs = new ArrayList<ThreadProcessor>();

		for (int i = 0 ; i < totalThreads ; i++) {

			procs.add(new ThreadProcessor(i));
		}

		return procs;
	}
}