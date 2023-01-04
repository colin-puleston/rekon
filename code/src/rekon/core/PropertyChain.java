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

	private ObjectPropertyName sup;
	private List<ObjectPropertyName> subsTail = new ArrayList<ObjectPropertyName>();

	public PropertyChain(ObjectPropertyName transitiveProp) {

		this(transitiveProp, transitiveProp, Collections.singletonList(transitiveProp));
	}

	public PropertyChain(ObjectPropertyName sup, List<ObjectPropertyName> subs) {

		this(sup, subs.get(0), subs.subList(1, subs.size()));
	}

	SomeRelation createLinkRelation(ObjectValue target) {

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
				ObjectPropertyName sup,
				ObjectPropertyName subsHead,
				List<ObjectPropertyName> subsTail) {

		this.sup = sup;
		this.subsTail = subsTail;

		subsHead.addChain(this);
	}
}
