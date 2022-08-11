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
class NameCollector {

	static private class Config {

		private boolean definition;
		private boolean linkedCollection;
		private boolean ranked = false;

		private int startRank = 0;
		private int stopRank = -1;

		Config(boolean definition, boolean linkedCollection) {

			this.definition = definition;
			this.linkedCollection = linkedCollection;
		}

		void setRanked(int startRank, int stopRank) {

			ranked = true;

			this.startRank = startRank;
			this.stopRank = stopRank;
		}

		boolean definition() {

			return definition;
		}

		boolean linkedCollection() {

			return linkedCollection;
		}

		boolean ranked() {

			return ranked;
		}

		boolean preRequiredRanks(int rank) {

			return rank < startRank;
		}

		boolean lastRequiredRank(int rank) {

			return rank == stopRank - 1;
		}
	}

	static private enum RankStatus {COLLECT, PRE_COLLECT, ROOT_COLLECTED}

	private Config config;

	private List<Names> allNames;
	private Deque<Name> linkNames;

	private NameSet rankNames = new NameSet();
	private NameCollector nextRankCollector = null;

	private int rank;
	private RankStatus rankStatus = RankStatus.COLLECT;

	NameCollector(boolean definition, boolean linkedCollection) {

		this(
			new Config(definition, linkedCollection),
			new ArrayList<Names>(),
			new ArrayDeque<Name>());
	}

	List<Names> collectRanked(NodePattern p) {

		return collectRanked(p, 0, -1);
	}

	List<Names> collectRanked(NodePattern p, int startRank, int stopRank) {

		config.setRanked(startRank, stopRank);

		return collect(p);
	}

	Names collectUnranked(NodePattern p) {

		return collect(p).get(0);
	}

	boolean definition() {

		return config.definition();
	}

	boolean signature() {

		return !config.definition();
	}

	boolean linkedCollection() {

		return config.linkedCollection();
	}

	boolean lastRequiredRank() {

		return config.lastRequiredRank(rank);
	}

	void collectFor(Name n) {

		if (rankStatus == RankStatus.COLLECT) {

			if (n.rootName()) {

				setAllMatchRank();
			}
			else {

				rankNames.add(n);
			}
		}
	}

	void collectForAll(Names ns) {

		if (rankStatus == RankStatus.COLLECT) {

			for (Name n : ns.getNames()) {

				collectFor(n);

				if (rankStatus == RankStatus.ROOT_COLLECTED) {

					break;
				}
			}
		}
	}

	void collectRoot() {

		if (rankStatus == RankStatus.COLLECT) {

			setAllMatchRank();
		}
	}

	NameCollector forNextRank() {

		if (!config.ranked()) {

			return this;
		}

		if (nextRankCollector == null) {

			nextRankCollector = new NameCollector(config, allNames, linkNames);
		}

		return nextRankCollector;
	}

	boolean startLinkedSection(Name linkName) {

		if (!linkedCollection() || linkNames.contains(linkName)) {

			return false;
		}

		linkNames.push(linkName);

		return true;
	}

	void endLinkedSection() {

		linkNames.pop();
	}

	private NameCollector(Config config, List<Names> allNames, Deque<Name> linkNames) {

		this.config = config;
		this.allNames = allNames;
		this.linkNames = linkNames;

		rank = allNames.size();

		allNames.add(rankNames);

		if (config.preRequiredRanks(rank)) {

			rankStatus = RankStatus.PRE_COLLECT;
		}
	}

	private List<Names> collect(NodePattern p) {

		p.collectNames(this);

		return collectAllRankNames();
	}

	private List<Names> collectAllRankNames() {

		List<Names> allRankNames = new ArrayList<Names>();

		collectAllRankNames(allRankNames);

		return allRankNames;
	}

	private void collectAllRankNames(List<Names> allRankNames) {

		if (rankStatus == RankStatus.ROOT_COLLECTED) {

			allRankNames.add(Names.NO_NAMES);
		}
		else {

			allRankNames.add(rankNames);
		}

		if (nextRankCollector != null) {

			nextRankCollector.collectAllRankNames(allRankNames);
		}
	}

	private void setAllMatchRank() {

		rankNames.clear();

		rankStatus = RankStatus.ROOT_COLLECTED;
	}
}

