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
public class NameSet extends Names {

	static public final NameSet NO_NAMES = new NameSet();

	private Set<Name> names;

	public NameSet() {

		names = new THashSet<Name>();
	}

	public NameSet(Name name) {

		this();

		add(name);
	}

	public NameSet(Names template) {

		this(template.getNames());
	}

	public NameSet(Collection<? extends Name> names) {

		this.names = new THashSet<Name>(names);
	}

	public boolean remove(Name name) {

		return names.remove(name);
	}

	public void removeAll(Names allNames) {

		names.removeAll(allNames.getNames());
	}

	public void removeAll(Collection<? extends Name> allNames) {

		names.removeAll(allNames);
	}

	public void retainAll(Names allNames) {

		names.retainAll(allNames.getNames());
	}

	public void absorb(Name name) {

		for (Name n : copyNames()) {

			if (name.subsumes(n)) {

				return;
			}

			if (n.subsumes(name)) {

				remove(n);
			}
		}

		add(name);
	}

	public void clear() {

		names.clear();
	}

	public Collection<Name> getNames() {

		return names;
	}

	public boolean contains(Name name) {

		return names.contains(name);
	}

	public boolean containsAll(Names allNames) {

		return names.containsAll(allNames.getNames());
	}

	public boolean containsAny(Names allNames) {

		for (Name name : allNames.getNames()) {

			if (names.contains(name)) {

				return true;
			}
		}

		return false;
	}

	public NameSet toSet() {

		return this;
	}
}
