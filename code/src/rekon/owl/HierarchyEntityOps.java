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

package rekon.owl;

import java.util.*;

import org.semanticweb.owlapi.model.*;

import rekon.core.*;

/**
 * @author Colin Puleston
 */
abstract class HierarchyEntityOps
				<E extends OWLEntity, I extends OWLObject>
				extends EntityOps<E, I> {

	final E topEntity;
	final E bottomEntity;

	final Set<E> topEntityGroup;
	final Set<E> bottomEntityGroup;

	private Set<Set<E>> emptyGroups = Collections.emptySet();

	private SupersRetriever supersRetriever = new SupersRetriever();
	private SubsRetriever subsRetriever = new SubsRetriever();

	private abstract class LinkedEntityRetriever extends EntityRetriever {

		Set<Set<E>> toEquivGroups(Names names, boolean direct) {

			Set<Set<E>> groups = toEquivGroups(names);

			if (!direct || groups.isEmpty()) {

				groups.add(getDefaultEntityGroup());
			}

			return groups;
		}

		abstract Set<E> getDefaultEntityGroup();
	}

	private class SupersRetriever extends LinkedEntityRetriever {

		Set<E> getDefaultEntityGroup() {

			return topEntityGroup;
		}
	}

	private class SubsRetriever extends LinkedEntityRetriever {

		Set<E> getDefaultEntityGroup() {

			return bottomEntityGroup;
		}
	}

	HierarchyEntityOps(E topEntity, E bottomEntity) {

		this.topEntity = topEntity;
		this.bottomEntity = bottomEntity;

		topEntityGroup = Collections.singleton(topEntity);
		bottomEntityGroup = Collections.singleton(bottomEntity);
	}

	Set<E> getEquivalents(I inObject) {

		if (inObject.equals(topEntity)) {

			return topEntityGroup;
		}

		if (inObject.equals(bottomEntity)) {

			return bottomEntityGroup;
		}

		return super.getEquivalents(inObject);
	}

	Set<Set<E>> getSupers(I inObject, boolean direct) {

		if (inObject.equals(topEntity)) {

			return emptyGroups;
		}

		if (inObject.equals(bottomEntity)) {

			return toEquivGroups(getBottomNameSupers(direct));
		}

		Names sups = getSuperNames(inObject, direct);

		return supersRetriever.toEquivGroups(sups, direct);
	}

	Set<Set<E>> getSubs(I inObject, boolean direct) {

		if (inObject.equals(bottomEntity)) {

			return emptyGroups;
		}

		Names subs = getSubNames(inObject, direct);

		return subsRetriever.toEquivGroups(subs, direct);
	}

	abstract boolean subsumption(I inSup, I inSub);

	abstract Names getSuperNames(I inObject, boolean direct);

	abstract Names getSubNames(I inObject, boolean direct);

	abstract Iterable<? extends Name> getAllEntityNames();

	abstract Names getDirectEntitySubs(Name name);

	private Names getBottomNameSupers(boolean direct) {

		return direct ? getLeafEntityNames() : new NameList(getAllEntityNames());
	}

	private Names getLeafEntityNames() {

		NameSet leafs = new NameSet();

		for (Name n : getAllEntityNames()) {

			if (getDirectEntitySubs(n).isEmpty()) {

				leafs.add(n);
			}
		}

		return leafs;
	}
}
