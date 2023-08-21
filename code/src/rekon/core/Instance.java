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

import gnu.trove.set.hash.*;

/**
 * @author Colin Puleston
 */
public abstract class Instance {

	private InstanceNode node = new InstanceNode(this);
	private Set<Instance> referencers = new THashSet<Instance>();

	private PatternCreator profileRecreator = null;

	public String toString() {

		return getClass().getSimpleName() + "(" + getLabel() + ")";
	}

	public InstanceNode getNode() {

		return node;
	}

	public abstract Object getInstanceId();

	public abstract String getLabel();

	void reset() {

		node = new InstanceNode(this);
	}

	void replaceGhost(Instance ghost) {

		referencers.addAll(ghost.referencers);

		for (Instance r : getReferenceds()) {

			r.referencers.remove(ghost);
			r.referencers.add(this);
		}
	}

	void setProfileRecreator(PatternCreator profileRecreator) {

		this.profileRecreator = profileRecreator;
	}

	boolean addReferencers() {

		Collection<Instance> refed = getReferenceds();

		if (!refed.isEmpty()) {

			for (Instance r : refed) {

				r.referencers.add(this);
			}

			return true;
		}

		return false;
	}

	void removeFromReferenceds() {

		for (Instance r : getReferenceds()) {

			r.referencers.remove(this);
		}
	}

	PatternCreator getProfileRecreator() {

		if (profileRecreator == null) {

			throw new Error("Profile-recreator has not been set!");
		}

		return profileRecreator;
	}

	boolean anyReferencers() {

		return !referencers.isEmpty();
	}

	Collection<Instance> getReferencers() {

		return referencers;
	}

	Collection<Instance> getReferenceds() {

		Set<Instance> refs = new THashSet<Instance>();

		for (InstanceNode rn : node.getReferenceds()) {

			refs.add(rn.getInstance());
		}

		return refs;
	}
}
