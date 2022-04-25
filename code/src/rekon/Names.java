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

package rekon;

import java.util.*;

/**
 * @author Colin Puleston
 */
abstract class Names {

	static final Names NO_NAMES = new NameList();

	private Boolean singleType = null;

	public String toString() {

		return getClass().getSimpleName() + "(" + getNames() + ")";
	}

	abstract NameSet toSet();

	Names filterForType(Class<? extends Name> type) {

		return hasSingleType(type) ? this : deriveForTypeOnly(type);
	}

	boolean add(Name name) {

		return getNames().add(name);
	}

	void addAll(Collection<? extends Name> allNames) {

		getNames().addAll(allNames);
	}

	void addAll(Names allNames) {

		addAll(allNames.getNames());
	}

	abstract void clear();

	NameSet expandWithSubsumers() {

		NameSet expanded = new NameSet(this);

		for (Name n : getNames()) {

			expanded.addAll(n.getSubsumers().getNames());
		}

		return expanded;
	}

	int size() {

		return getNames().size();
	}

	boolean isEmpty() {

		return getNames().isEmpty();
	}

	Name getFirstName() {

		return getNames().iterator().next();
	}

	abstract Collection<Name> getNames();

	Collection<Name> copyNames() {

		return new ArrayList<Name>(getNames());
	}

	private boolean hasSingleType(Class<? extends Name> type) {

		if (singleType == null) {

			singleType = checkSingleType();
		}

		return singleType && type.isAssignableFrom(getFirstName().getClass());
	}

	private boolean checkSingleType() {

		Class<? extends Name> type = null;

		for (Name n : getNames()) {

			if (type == null) {

				type = n.getClass();
			}
			else {

				if (type.isAssignableFrom(n.getClass())) {

					return false;
				}
			}
		}

		return type != null;
	}

	private NameList deriveForTypeOnly(Class<? extends Name> type) {

		NameList derived = new NameList();

		for (Name n : getNames()) {

			if (type.isAssignableFrom(n.getClass())) {

				derived.add(n);
			}
		}

		return derived;
	}
}
