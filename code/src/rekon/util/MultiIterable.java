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
public class MultiIterable<E> implements Iterable<E> {

	private List<Iterable<? extends E>> components = new ArrayList<Iterable<? extends E>>();
	private MultiListReader<E> listReader = new MultiListReader<E>();

	private class MultiIterator implements Iterator<E> {

		private int currentComponentIdx = 0;
		private Iterator<? extends E> currentIterator = components.get(0).iterator();

		public boolean hasNext() {

			return resolveCurrentIterator().hasNext();
		}

		public E next() {

			return resolveCurrentIterator().next();
		}

		private Iterator<? extends E> resolveCurrentIterator() {

			while (!currentIterator.hasNext()) {

				if (++currentComponentIdx == components.size()) {

					break;
				}

				currentIterator = components.get(currentComponentIdx).iterator();
			}

			return currentIterator;
		}
	}

	public Iterator<E> iterator() {

		return new MultiIterator();
	}

	public void addComponent(Iterable<? extends E> component) {

		if (component instanceof MultiIterable) {

			addMultiComponents((MultiIterable<? extends E>)component);
		}
		else {

			addAtomicComponent(component);
		}
	}

	ListReader<E> asListReader() {

		return listReader;
	}

	private void addMultiComponents(MultiIterable<? extends E> multiIter) {

		for (Iterable<? extends E> comp : multiIter.components) {

			addAtomicComponent(comp);
		}
	}

	private void addAtomicComponent(Iterable<? extends E> comp) {

		components.add(comp);
		listReader.addComponent(comp);
	}

}

