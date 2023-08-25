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
class NameClassification extends NameLinksHandler {

	static private final EmptyVerticalLinks EMPTY_VERTICAL_LINKS = new EmptyVerticalLinks();

	static private abstract class VerticalLinks {

		void addTransientDirect(Name in) {

			throw new UnexpectedMethodInvocationError();
		}

		void removeTransientDirect(Name in) {

			throw new UnexpectedMethodInvocationError();
		}

		abstract Names get(boolean direct);

		abstract boolean hasLinkTo(Name target, NameSet visited);

		abstract Names collectAll(NameSet collected);

		abstract NameSet getActiveDirects();
	}

	static private class EmptyVerticalLinks extends VerticalLinks {

		Names get(boolean direct) {

			return Names.NO_NAMES;
		}

		boolean hasLinkTo(Name target, NameSet visited) {

			return false;
		}

		Names collectAll(NameSet collected) {

			return collected;
		}

		NameSet getActiveDirects() {

			throw new UnexpectedMethodInvocationError();
		}
	}

	static private abstract class ActiveVerticalLinks extends VerticalLinks {

		private NameSet directs = new NameSet();

		void addTransientDirect(Name in) {

			directs.add(in);
		}

		void removeTransientDirect(Name in) {

			directs.remove(in);
		}

		Names get(boolean direct) {

			return direct ? directs : collectAll(new NameSet());
		}

		boolean hasLinkTo(Name target, NameSet visited) {

			for (Name d : directs) {

				if (d.equals(target) || hasLinkToNext(target, d, visited)) {

					return true;
				}
			}

			return false;
		}

		Names collectAll(NameSet collected) {

			for (Name d : directs) {

				if (collected.add(d)) {

					getSameLinksFor(d).collectAll(collected);
				}
			}

			return collected;
		}

		NameSet getActiveDirects() {

			return directs;
		}

		abstract VerticalLinks getSameLinksFor(Name n);

		private boolean hasLinkToNext(Name target, Name next, NameSet visited) {

			return visited.add(next) && getSameLinksFor(next).hasLinkTo(target, visited);
		}
	}

	static private class Supers extends ActiveVerticalLinks {

		VerticalLinks getSameLinksFor(Name n) {

			return n.getClassification().supers;
		}
	}

	static private class Subs extends ActiveVerticalLinks {

		VerticalLinks getSameLinksFor(Name n) {

			return n.getClassification().subs;
		}
	}

	static void completeAllClassifications(List<Name> allNames) {

		for (Name n : allNames) {

			n.setClassification();
		}

		initialiseAllClassifications(allNames);
		optimiseAllLinks(allNames);

		for (Name n : allNames) {

			n.getClassification().completeInitialisation();
		}
	}

	static private void initialiseAllClassifications(List<Name> allNames) {

		for (Name n : allNames) {

			getInitialiserFor(n).resolveBasicLinksWithSubsumers();
		}

		new DirectSupersSetter(allNames);

		for (Name n : allNames) {

			getInitialiserFor(n).setAsDirectSub();
		}
	}

	static private void optimiseAllLinks(List<Name> allNames) {

		for (Name n : allNames) {

			n.getClassification().combineSingleIncomingSupers();
		}

		for (Name n : allNames) {

			n.getClassification().combineSingleIncomingSubs();
		}

		for (Name n : allNames) {

			n.getClassification().optimiseEmptyLinks();
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

		void setDirectSupers() {

			supers.getActiveDirects().addAll(ancestors);

			for (Name a : ancestors) {

				supers.getActiveDirects().removeAll(getInitialiserFor(a).ancestors);
			}
		}

		void setAsDirectSub() {

			for (Name d : supers.getActiveDirects()) {

				d.getClassification().subs.getActiveDirects().add(name);
			}
		}

		private boolean checkSubsumedToEquiv(Name subsumed) {

			if (ancestors.remove(subsumed)) {

				equivalents.add(subsumed);

				return true;
			}

			return false;
		}
	}

	private abstract class SingleIncomingVerticalLinksCombiner {

		SingleIncomingVerticalLinksCombiner() {

			VerticalLinks commonIncomings = null;

			for (Name linked : getOutgingLinks().get(true)) {

				VerticalLinks incomings = getIncomingLinks(linked);

				if (incomings.get(true).size() == 1) {

					if (commonIncomings == null) {

						commonIncomings = incomings;
					}
					else {

						setIncomingLinks(linked, commonIncomings);
					}
				}
			}
		}

		abstract VerticalLinks getOutgingLinks();

		abstract VerticalLinks getIncomingLinks(Name linked);

		abstract void setIncomingLinks(Name linked, VerticalLinks links);
	}

	private class SingleIncomingSupersCombiner extends SingleIncomingVerticalLinksCombiner {

		VerticalLinks getOutgingLinks() {

			return subs;
		}

		VerticalLinks getIncomingLinks(Name linked) {

			return linked.getClassification().supers;
		}

		void setIncomingLinks(Name linked, VerticalLinks links) {

			linked.getClassification().supers = links;
		}
	}

	private class SingleIncomingSubsCombiner extends SingleIncomingVerticalLinksCombiner {

		VerticalLinks getOutgingLinks() {

			return supers;
		}

		VerticalLinks getIncomingLinks(Name linked) {

			return linked.getClassification().subs;
		}

		void setIncomingLinks(Name linked, VerticalLinks links) {

			linked.getClassification().subs = links;
		}
	}

	NameClassification(Name name, NameSet subsumers) {

		this.name = name;

		initialiser = new Initialiser(subsumers);
	}

	void addTransientDirectSuper(Name s) {

		if (supers == EMPTY_VERTICAL_LINKS) {

			supers = new Supers();
		}

		supers.addTransientDirect(s);
	}

	void removeTransientDirectSuper(Name s) {

		supers.removeTransientDirect(s);

		optimiseEmptySupers();
	}

	void addTransientDirectSub(Name s) {

		if (subs == EMPTY_VERTICAL_LINKS) {

			subs = new Subs();
		}

		subs.addTransientDirect(s);

		combineSingleIncomingSubs();
	}

	void removeTransientDirectSub(Name s) {

		subs.removeTransientDirect(s);

		optimiseEmptySubs();
	}

	void optimiseEmptyLinks() {

		optimiseEmptyEquivs();
		optimiseEmptySupers();
		optimiseEmptySubs();
	}

	Names getSubsumers() {

		NameList ss = new NameList();

		ss.addAll(equivalents);
		ss.addAll(supers.get(false));

		return ss;
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

	boolean isSubsumer(Name test) {

		return equivalents.contains(test) || supers.hasLinkTo(test, new NameSet());
	}

	private void combineSingleIncomingSupers() {

		new SingleIncomingSupersCombiner();
	}

	private void combineSingleIncomingSubs() {

		new SingleIncomingSubsCombiner();
	}

	private void completeInitialisation() {

		initialiser = null;
	}

	private void optimiseEmptyEquivs() {

		if (equivalents.isEmpty()) {

			equivalents = NameSet.NO_NAMES;
		}
	}

	private void optimiseEmptySupers() {

		if (supers.getActiveDirects().isEmpty()) {

			supers = EMPTY_VERTICAL_LINKS;
		}
	}

	private void optimiseEmptySubs() {

		if (subs.getActiveDirects().isEmpty()) {

			subs = EMPTY_VERTICAL_LINKS;
		}
	}
}
