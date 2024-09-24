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

/**
 * @author Colin Puleston
 */
public abstract class MultiThreadListProcessor<E> extends MultiThreadProcessor<E> {

	private ListReader<E> list = null;

	public void invokeListProcesses(Iterable<E> elements) {

		list = toListReader(elements);

		setMaxProcesses(list.size());
		execProcesses();
	}

	protected void execThreadProcess(int totalThreads, int threadIndex) {

		for (int i = threadIndex ; i < list.size() ; i += totalThreads) {

			processElement(list.get(i));
		}
	}

	protected void execAllInSingleThread() {

		execThreadProcess(1, 0);
	}

	protected abstract void processElement(E e);

	private ListReader<E> toListReader(Iterable<E> elements) {

		if (elements instanceof CompoundIterable) {

			return ((CompoundIterable<E>)elements).asListReader();
		}

		return new SingleListReader<E>(elements);
	}
}