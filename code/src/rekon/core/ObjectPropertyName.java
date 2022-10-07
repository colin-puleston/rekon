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
public abstract class ObjectPropertyName extends PropertyName {

	private Set<ObjectPropertyName> inverses = new HashSet<ObjectPropertyName>();
	private List<PropertyChain> chains = new ArrayList<PropertyChain>();

	private boolean transitive = false;

	public void addInverses(Collection<ObjectPropertyName> invs) {

		inverses.addAll(invs);

		for (ObjectPropertyName inv : invs) {

			inv.inverses.add(this);
		}
	}

	public void addChain(PropertyChain chain) {

		chains.add(chain);
	}

	public void setTransitive() {

		transitive = true;
	}

	public void setSymmetric() {

		inverses.add(this);
	}

	Collection<ObjectPropertyName> getInverses() {

		return inverses;
	}

	boolean anyChains() {

		if (!chains.isEmpty()) {

			return true;
		}

		for (Name s : getSubsumers().getNames()) {

			if (!((ObjectPropertyName)s).chains.isEmpty()) {

				return true;
			}
		}

		return false;
	}

	Collection<PropertyChain> getAllChains() {

		List<PropertyChain> allChains = new ArrayList<PropertyChain>(chains);

		for (Name s : getSubsumers().getNames()) {

			allChains.addAll(((ObjectPropertyName)s).chains);
		}

		return allChains;
	}

	boolean transitive() {

		return transitive;
	}

	ObjectPropertyName lookForMostGeneralTransitiveProperty() {

		ObjectPropertyName mostGeneral = null;

		for (Name s : getSubsumers().getNames()) {

			ObjectPropertyName os = (ObjectPropertyName)s;

			if (os.transitive) {

				if (mostGeneral == null || os.subsumes(mostGeneral)) {

					mostGeneral = os;
				}
			}
		}

		return mostGeneral != null ? mostGeneral : (transitive ? this : null);
	}
}
