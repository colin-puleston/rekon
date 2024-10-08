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
public abstract class NodeProperty extends PropertyX {

	private NodeProperty inverse = null;
	private List<PropertyChain> chains = new ArrayList<PropertyChain>();

	public void setSymmetric() {

		setInverse(this);
	}

	public void setInverse(NodeProperty inverse) {

		if (this.inverse == null) {

			this.inverse = inverse;

			inverse.setInverse(this);
		}
		else {

			if (inverse != this.inverse) {

				this.inverse.addEquivalent(inverse);
			}
		}
	}

	public void addChain(PropertyChain chain) {

		chains.add(chain);
	}

	boolean hasInverse() {

		return inverse != null;
	}

	NodeProperty getInverse() {

		return inverse;
	}

	boolean directChains() {

		return !chains.isEmpty();
	}

	boolean anyChains() {

		if (!chains.isEmpty()) {

			return true;
		}

		for (Name s : getSubsumers()) {

			if (!((NodeProperty)s).chains.isEmpty()) {

				return true;
			}
		}

		return false;
	}

	Collection<PropertyChain> getAllChains() {

		List<PropertyChain> allChains = new ArrayList<PropertyChain>(chains);

		for (Name s : getSubsumers()) {

			allChains.addAll(((NodeProperty)s).chains);
		}

		return allChains;
	}
}
