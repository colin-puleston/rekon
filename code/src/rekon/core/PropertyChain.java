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
public class PropertyChain {

	private NodePropertyName sup;
	private List<NodePropertyName> subsTail = new ArrayList<NodePropertyName>();

	public PropertyChain(NodePropertyName transitiveProp) {

		this(transitiveProp, transitiveProp, Collections.singletonList(transitiveProp));
	}

	public PropertyChain(NodePropertyName sup, List<NodePropertyName> subs) {

		this(sup, subs.get(0), subs.subList(1, subs.size()));
	}

	SomeRelation createLinkRelation(NodeValue target) {

		return new SomeRelation(sup, target);
	}

	boolean hasSuper(PropertyName prop) {

		return sup.subsumes(prop);
	}

	boolean hasTailSub(PropertyName prop, int index) {

		return subsTail.get(index).subsumes(prop);
	}

	boolean lastTailSub(int index) {

		return index == subsTail.size() - 1;
	}

	private PropertyChain(
				NodePropertyName sup,
				NodePropertyName subsHead,
				List<NodePropertyName> subsTail) {

		this.sup = sup;
		this.subsTail = subsTail;

		subsHead.addChain(this);
	}
}
