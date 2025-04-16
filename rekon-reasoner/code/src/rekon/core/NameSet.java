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
public class NameSet extends Names {

	static public final NameSet NO_NAMES = new NameSet();

	private Set<Name> names = new HashSet<Name>();

	private abstract class MostExtremeRetainer {

		void absorbAll(Names newNames) {

			for (Name name : newNames) {

				absorb(name);
			}
		}

		void absorb(Name newName) {

			boolean newRequired = false;

			for (Name n : copyNames()) {

				if (!newRequired && mostExtremeFirst(n, newName)) {

					return;
				}

				if (mostExtremeFirst(newName, n)) {

					remove(n);

					newRequired |= true;
				}
			}

			add(newName);
		}

		abstract boolean mostExtremeFirst(Name n1, Name n2);
	}

	private class MostGeneralRetainer extends MostExtremeRetainer {

		boolean mostExtremeFirst(Name n1, Name n2) {

			return n1.subsumes(n2);
		}
	}

	private class MostSpecificRetainer extends MostExtremeRetainer {

		boolean mostExtremeFirst(Name n1, Name n2) {

			return n2.subsumes(n1);
		}
	}

	public NameSet() {
	}

	public NameSet(Name name) {

		add(name);
	}

	public NameSet(Names template) {

		this(template.getNames());
	}

	public NameSet(Collection<? extends Name> names) {

		addNames(this.names, names);
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

	public void retainMostGeneral(Names newNames) {

		new MostGeneralRetainer().absorbAll(newNames);
	}

	public void retainMostGeneral(Name newName) {

		new MostGeneralRetainer().absorb(newName);
	}

	public void retainMostSpecific(Names newNames) {

		new MostSpecificRetainer().absorbAll(newNames);
	}

	public void retainMostSpecific(Name newName) {

		new MostSpecificRetainer().absorb(newName);
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

		for (Name name : allNames) {

			if (names.contains(name)) {

				return true;
			}
		}

		return false;
	}
}
