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
class NameSet extends Names {

	private Set<Name> names;

	NameSet() {

		names = new HashSet<Name>();
	}

	NameSet(Name name) {

		this();

		add(name);
	}

	NameSet(Names template) {

		this(template.getNames());
	}

	NameSet(Collection<? extends Name> names) {

		this.names = new HashSet<Name>(names);
	}

	NameSet toSet() {

		return this;
	}

	boolean remove(Name name) {

		return names.remove(name);
	}

	void removeAll(Names allNames) {

		names.removeAll(allNames.getNames());
	}

	void removeAll(Collection<? extends Name> allNames) {

		names.removeAll(allNames);
	}

	void retainAll(Names allNames) {

		names.retainAll(allNames.getNames());
	}

	void clear() {

		names.clear();
	}

	boolean contains(Name name) {

		return names.contains(name);
	}

	boolean containsAll(Names allNames) {

		return names.containsAll(allNames.getNames());
	}

	boolean containsAny(Names allNames) {

		for (Name name : allNames.getNames()) {

			if (names.contains(name)) {

				return true;
			}
		}

		return false;
	}

	Collection<Name> getNames() {

		return names;
	}
}
