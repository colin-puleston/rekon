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
public abstract class Instance {

	private InstanceNode node;
	private Set<Instance> referencers = new HashSet<Instance>();

	private SinglePatternBuilder profileRebuilder = null;

	public String toString() {

		return getClass().getSimpleName() + "(" + getLabel() + ")";
	}

	public InstanceNode getNode() {

		return node;
	}

	public abstract Object getInstanceId();

	public abstract String getLabel();

	public boolean undefinedRef() {

		return profileRebuilder == null;
	}

	protected Instance(boolean undefinedRef) {

		node = new InstanceNode(this, undefinedRef);
	}

	void checkAddAsReferencer(SinglePatternBuilder profileBuilder) {

		Collection<Instance> refed = getReferenceds();

		if (!refed.isEmpty()) {

			for (Instance r : refed) {

				r.referencers.add(this);
			}

			profileRebuilder = profileBuilder;
		}
	}

	void removeAsReferencer() {

		for (Instance r : getReferenceds()) {

			r.referencers.remove(this);
		}
	}

	void setAsUndefinedRef() {

		profileRebuilder = null;
	}

	SinglePatternBuilder getProfileRebuilder() {

		if (profileRebuilder == null) {

			throw new Error("Profile-rebuilder has not been set for: " + node);
		}

		return profileRebuilder;
	}

	boolean anyReferencers() {

		return !referencers.isEmpty();
	}

	Collection<Instance> getReferencers() {

		return referencers;
	}

	Collection<Instance> getReferenceds() {

		Set<Instance> refs = new HashSet<Instance>();

		for (InstanceNode rn : node.getReferenceds()) {

			refs.add(rn.getInstance());
		}

		return refs;
	}
}
