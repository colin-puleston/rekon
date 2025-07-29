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

	private NodeProperty sup;
	private List<NodeProperty> subs = new ArrayList<NodeProperty>();

	public PropertyChain(NodeProperty transitiveProp) {

		this(transitiveProp, Arrays.asList(transitiveProp, transitiveProp));
	}

	public PropertyChain(NodeProperty sup, List<NodeProperty> subs) {

		this.sup = sup;
		this.subs.addAll(subs);

		subs.get(0).addChain(this);
	}

	public void checkInverseChain() {

		if (sup.hasInverse()) {

			List<NodeProperty> invSubs = findInverseSubs();

			if (invSubs != null) {

				new PropertyChain(sup.getInverse(), invSubs);
			}
		}
	}

	NodeRelation createLinkRelation(NodeValue target) {

		return new NodeRelation(sup, target);
	}

	boolean hasSuper(PropertyX prop) {

		return sup.subsumes(prop);
	}

	boolean hasSub(PropertyX prop, int index) {

		return subs.get(index).subsumes(prop);
	}

	boolean lastSub(int index) {

		return index == subs.size() - 1;
	}

	private List<NodeProperty> findInverseSubs() {

		List<NodeProperty> invSubs = new ArrayList<NodeProperty>();

		for (int i = subs.size() - 1 ; i >= 0 ; i--) {

			NodeProperty sub = subs.get(i);

			if (!sub.hasInverse()) {

				return null;
			}

			invSubs.add(sub.getInverse());
		}

		return invSubs;
	}
}
