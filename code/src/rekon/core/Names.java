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
public abstract class Names {

	static public final Names NO_NAMES = new NameList();

	public String toString() {

		return getClass().getSimpleName() + "(" + getNames() + ")";
	}

	public int size() {

		return getNames().size();
	}

	public boolean isEmpty() {

		return getNames().isEmpty();
	}

	public boolean add(Name name) {

		return getNames().add(name);
	}

	public void addAll(Collection<? extends Name> allNames) {

		getNames().addAll(allNames);
	}

	public void addAll(Names allNames) {

		addAll(allNames.getNames());
	}

	public abstract void clear();

	public abstract Collection<Name> getNames();

	abstract NameSet toSet();

	Names filterForType(Class<? extends Name> type) {

		return allOfType(type) ? this : deriveForTypeOnly(type);
	}

	NameSet expandWithSubsumers() {

		NameSet expanded = new NameSet(this);

		for (Name n : getNames()) {

			expanded.addAll(n.getSubsumers().getNames());
		}

		return expanded;
	}

	Name getFirstName() {

		return getNames().iterator().next();
	}

	Collection<Name> copyNames() {

		return new ArrayList<Name>(getNames());
	}

	private boolean allOfType(Class<? extends Name> type) {

		for (Name n : getNames()) {

			if (!type.isAssignableFrom(n.getClass())) {

				return false;
			}
		}

		return true;
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
