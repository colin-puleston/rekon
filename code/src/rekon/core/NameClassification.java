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
class NameClassification extends NameClassificationHandler {

	static void completeClassifications(List<Name> allNames) {

		for (Name n : allNames) {

			n.setClassification();
		}

		initialiseClassifications(allNames);

		for (Name n : allNames) {

			n.getClassification().endInitialisation();
		}
	}

	static private void initialiseClassifications(List<Name> allNames) {

		for (Name n : allNames) {

			getInitialiserFor(n).resolveBasicLinksWithSubsumers();
		}

		new DirectSupersSetter(allNames);

		for (Name n : allNames) {

			getInitialiserFor(n).setAsDirectSub();
		}
	}

	static private Initialiser getInitialiserFor(Name n) {

		return n.getClassification().initialiser;
	}

	static private class DirectSupersSetter extends MultiThreadListProcessor<Name> {

		DirectSupersSetter(List<Name> allNames) {

			invokeListProcesses(allNames);
		}

		void processElement(Name n) {

			getInitialiserFor(n).setDirectSupers();
		}
	}

	private Name name;

	private NameSet equivalents = new NameSet();
	private VerticalLinks supers = new Supers();
	private VerticalLinks subs = new Subs();

	private Initialiser initialiser;

	private abstract class VerticalLinks {

		final NameSet directs = new NameSet();

		Names get(boolean direct) {

			return direct ? directs : collectAll(new NameSet());
		}

		boolean hasLinkTo(Name n) {

			return hasLinkTo(new NameSet(), n);
		}

		abstract VerticalLinks getSameLinksFor(Name n);

		private Names collectAll(NameSet collected) {

			for (Name d : directs.getNames()) {

				if (collected.add(d)) {

					getSameLinksFor(d).collectAll(collected);
				}
			}

			return collected;
		}

		private boolean hasLinkTo(NameSet visited, Name n) {

			if (visited.add(name)) {

				for (Name d : directs.getNames()) {

					if (d.equals(n) || getSameLinksFor(d).hasLinkTo(visited, n)) {

						return true;
					}
				}
			}

			return false;
		}
	}

	private class Supers extends VerticalLinks {

		VerticalLinks getSameLinksFor(Name n) {

			return n.getClassification().supers;
		}
	}

	private class Subs extends VerticalLinks {

		VerticalLinks getSameLinksFor(Name n) {

			return n.getClassification().subs;
		}
	}

	private class Initialiser {

		private NameSet ancestors;

		Initialiser(NameSet subsumers) {

			ancestors = subsumers;
		}

		void resolveBasicLinksWithSubsumers() {

			for (Name s : ancestors.copyNames()) {

				if (getInitialiserFor(s).checkSubsumedToEquiv(name)) {

					ancestors.remove(s);
					equivalents.add(s);
				}
			}
		}

		private boolean checkSubsumedToEquiv(Name subsumed) {

			if (ancestors.remove(subsumed)) {

				equivalents.add(subsumed);

				return true;
			}

			return false;
		}

		private void setDirectSupers() {

			supers.directs.addAll(ancestors);

			for (Name a : ancestors.getNames()) {

				supers.directs.removeAll(getInitialiserFor(a).ancestors);
			}
		}

		private void setAsDirectSub() {

			for (Name d : supers.directs.getNames()) {

				d.getClassification().subs.directs.add(name);
			}
		}
	}

	NameClassification(Name name, NameSet subsumers) {

		this.name = name;

		initialiser = new Initialiser(subsumers);
	}

	boolean classified() {

		return true;
	}

	boolean rootName() {

		return supers.get(true).isEmpty();
	}

	Names getSubsumers() {

		NameList ss = new NameList();

		ss.addAll(equivalents);
		ss.addAll(supers.get(false));

		return ss;
	}

	boolean isSubsumer(Name name) {

		return equivalents.contains(name) || supers.hasLinkTo(name);
	}

	Names getEquivalents() {

		return equivalents;
	}

	Names getSupers(boolean direct) {

		return supers.get(direct);
	}

	Names getSubs(boolean direct) {

		return subs.get(direct);
	}

	private void endInitialisation() {

		initialiser = null;
	}
}
