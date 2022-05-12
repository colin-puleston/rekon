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
abstract class NameCollector {

	static final NameCollector definitionRequests = new RootNameCollector(true, true, false);
	static final NameCollector signatureOptions = new RootNameCollector(false, false, false);

	static final NameCollector definitionRequestsExtendedMatch = new RootNameCollector(true, true, true);
	static final NameCollector signatureOptionsExtendedMatch = new RootNameCollector(false, false, true);

	static final NameCollector signatureRequests = new RootNameCollector(false, false, false);
	static final NameCollector definitionOptions = new RootNameCollector(true, false, false);

	static private class RootNameCollector extends NameCollector {

		private boolean definition;
		private boolean request;
		private boolean extendedMatch;

		private boolean ranked = false;

		RootNameCollector(boolean definition, boolean request, boolean extendedMatch) {

			super(new ArrayList<Names>());

			this.definition = definition;
			this.request = request;
			this.extendedMatch = extendedMatch;
		}

		boolean definition() {

			return definition;
		}

		boolean request() {

			return request;
		}

		boolean extendedMatch() {

			return extendedMatch;
		}

		boolean ranked() {

			return ranked;
		}

		List<Names> collectRanked(NodePattern p) {

			return collect(p, true);
		}

		List<Names> collectOneRank(NodePattern p) {

			return collect(p, false);
		}

		Names collectUnranked(NodePattern p) {

			return collect(p, false).get(0);
		}

		RootNameCollector getRootCollector() {

			return this;
		}

		private List<Names> collect(NodePattern p, boolean ranked) {

			this.ranked = ranked;

			p.collectNames(this);

			return clearAllRanks();
		}
	}

	static private class NestedNameCollector extends NameCollector {

		private RootNameCollector rootCollector;

		NestedNameCollector(RootNameCollector rootCollector) {

			super(rootCollector);

			this.rootCollector = rootCollector;
		}

		boolean definition() {

			return rootCollector.definition();
		}

		boolean request() {

			return rootCollector.request();
		}

		boolean extendedMatch() {

			return rootCollector.extendedMatch();
		}

		boolean ranked() {

			return rootCollector.ranked();
		}

		RootNameCollector getRootCollector() {

			return rootCollector;
		}
	}

	private List<Names> allNames;
	private NameSet rankNames = new NameSet();
	private NameCollector nextRankCollector = null;

	private boolean nonMatchingRank = false;

	abstract boolean definition();

	boolean signature() {

		return !definition();
	}

	abstract boolean request();

	boolean option() {

		return !request();
	}

	abstract boolean extendedMatch();

	abstract boolean ranked();

	List<Names> collectRanked(NodePattern p) {

		throw new Error("Method should never be invoked!");
	}

	List<Names> collectOneRank(NodePattern p) {

		throw new Error("Method should never be invoked!");
	}

	Names collectUnranked(NodePattern p) {

		throw new Error("Method should never be invoked!");
	}

	void collectFor(Name n) {

		if (!nonMatchingRank) {

			if (n.rootName()) {

				setNonMatchingRank();
			}
			else {

				n.collectNames(rankNames);
			}
		}
	}

	void collectForAll(Names ns) {

		if (!nonMatchingRank) {

			for (Name n : ns.getNames()) {

				collectFor(n);

				if (nonMatchingRank) {

					break;
				}
			}
		}
	}

	void collectRoot() {

		if (!nonMatchingRank) {

			setNonMatchingRank();
		}
	}

	NameCollector forNextRank() {

		if (!ranked()) {

			return this;
		}

		if (nextRankCollector == null) {

			nextRankCollector = new NestedNameCollector(getRootCollector());
		}

		return nextRankCollector;
	}

	List<Names> clearAllRanks() {

		List<Names> allNamesCopy = new ArrayList<Names>();

		clearRanks(allNamesCopy);

		return allNamesCopy;
	}

	abstract RootNameCollector getRootCollector();

	private NameCollector(NameCollector rootCollector) {

		this(rootCollector.allNames);
	}

	private NameCollector(List<Names> allNames) {

		this.allNames = allNames;

		allNames.add(rankNames);
	}

	private void setNonMatchingRank() {

		rankNames.clear();

		nonMatchingRank = true;
	}

	private void clearRanks(List<Names> allNamesCopy) {

		if (nonMatchingRank) {

			allNamesCopy.add(Names.NO_NAMES);

			nonMatchingRank = false;
		}
		else {

			if (rankNames.isEmpty()) {

				return;
			}

			Names rankNamesCopy = new NameList(rankNames);

			rankNames.clear();

			allNamesCopy.add(rankNamesCopy);
		}

		if (nextRankCollector != null) {

			nextRankCollector.clearRanks(allNamesCopy);
		}
	}
}

