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
public abstract class Names implements Iterable<Name> {

	static public final Names NO_NAMES = new NameList();

	static void addNames(Collection<Name> target, Iterable<? extends Name> names) {

		for (Name n : names) {

			target.add(n);
		}
	}

	private class AsNodes implements Iterable<NodeX> {

		private class NodesIterator implements Iterator<NodeX> {

			private Iterator<Name> nameIterator = Names.this.iterator();

			public boolean hasNext() {

				return nameIterator.hasNext();
			}

			public NodeX next() {

				return (NodeX)nameIterator.next();
			}
		}

		public Iterator<NodeX> iterator() {

			return new NodesIterator();
		}
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

	public boolean equals(Object other) {

		return getClass() == other.getClass() && equalsNames(getClass().cast(other));
	}

	public int hashCode() {

		return getNames().hashCode();
	}

	public String toString() {

		return getClass().getSimpleName() + "(" + getNames() + ")";
	}

	public Iterator<Name> iterator() {

		return getNames().iterator();
	}

	public int size() {

		return getNames().size();
	}

	public boolean isEmpty() {

		return getNames().isEmpty();
	}

	public abstract Collection<Name> getNames();

	public Collection<Name> copyNames() {

		return new ArrayList<Name>(getNames());
	}

	public Iterable<NodeX> asNodes() {

		return new AsNodes();
	}

	public Collection<NodeX> copyNodes() {

		List<NodeX> nodes = new ArrayList<NodeX>();

		for (NodeX n : asNodes()) {

			nodes.add(n);
		}

		return nodes;
	}

	Names filterForType(Class<? extends Name> type) {

		return allOfType(type) ? this : deriveForTypeOnly(type);
	}

	Name getFirstName() {

		return getNames().iterator().next();
	}

	NodeX getFirstNode() {

		return (NodeX)getFirstName();
	}

	boolean anySubsumes(Name test) {

		for (Name n : getNames()) {

			if (n.subsumes(test)) {

				return true;
			}
		}

		return false;
	}

	private boolean equalsNames(Names other) {

		return getNames().equals(other.getNames());
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
