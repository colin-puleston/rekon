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
abstract class ExtremeSetBuilder<E> {

	void absorbAll(Collection<E> newSet) {

		for (E element : newSet) {

			absorb(element);
		}
	}

	boolean absorb(E newElement) {

		boolean adding = false;

		for (E e : copyCurrentSet()) {

			if (!adding && mostExtremeFirst(e, newElement)) {

				return false;
			}

			if (mostExtremeFirst(newElement, e)) {

				removeFromSet(e);

				adding |= true;
			}
		}

		addToSet(newElement);

		return true;
	}

	abstract void addToSet(E e);

	abstract void removeFromSet(E e);

	abstract Collection<E> copyCurrentSet();

	abstract boolean mostExtremeFirst(E e1, E e2);
}
