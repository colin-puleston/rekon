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

	static final NameCollector definitionRequests = new NameCollector(true, true);
	static final NameCollector signatureOptions = new NameCollector(false, false);

	static final NameCollector signatureRequests = new NameCollector(false, false);
	static final NameCollector definitionOptions = new NameCollector(true, false);

	static private class Config {

		private boolean definition;
		private boolean request;

		private boolean linkedCollection = false;
		private boolean ranked = false;

		private int startRank = 0;
		private int endRank = -1;

		Config(boolean definition, boolean request) {

			this.definition = definition;
			this.request = request;
		}

		void setLinkedCollection() {

			linkedCollection = true;
		}

		void setRanked() {

			ranked = true;
		}

		void setRanked(int startRank, int endRank) {

			ranked = true;

			this.startRank = startRank;
			this.endRank = endRank;
		}

		void reset() {

			linkedCollection = false;
			ranked = false;

			startRank = 0;
			endRank = -1;
		}

		boolean definition() {

			return definition;
		}

		boolean request() {

			return request;
		}

		boolean linkedCollection() {

			return linkedCollection;
		}

		boolean ranked() {

			return ranked;
		}

		private int startRank() {

			return startRank;
		}

		private int endRank() {

			return endRank;
		}
	}

	private Config config;

	private List<Names> allNames;
	private Deque<Name> linkNames;

	private NameSet rankNames = new NameSet();
	private NameCollector nextRankCollector = null;

	private boolean nonMatchingRank = false;

	void setLinkedCollection() {

		config.setLinkedCollection();
	}

	List<Names> collectRanked(NodePattern p) {

		config.setRanked();

		return collect(p);
	}

	List<Names> collectRanked(NodePattern p, int startRank, int endRank) {

		config.setRanked(startRank, endRank);

		return collect(p);
	}

	Names collectUnranked(NodePattern p) {

		return collect(p).get(0);
	}

	void collectFor(Name n) {

		if (!nonMatchingRank) {

			if (n.rootName()) {

				setNonMatchingRank();
			}
			else {

				rankNames.add(n);
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

	boolean definition() {

		return config.definition();
	}

	boolean signature() {

		return !config.definition();
	}

	boolean request() {

		return config.request();
	}

	boolean option() {

		return !config.request();
	}

	boolean linkedCollection() {

		return config.linkedCollection();
	}

	boolean ranked() {

		return config.ranked();
	}

	boolean lastRequiredRank() {

		return currentRank() == config.endRank();
	}

	private NameCollector(boolean definition, boolean request) {

		this(new Config(definition, request), new ArrayList<Names>(), new ArrayDeque<Name>());
	}

	private NameCollector(Config config, List<Names> allNames, Deque<Name> linkNames) {

		this.config = config;
		this.allNames = allNames;
		this.linkNames = linkNames;

		allNames.add(rankNames);

		if (currentRank() < config.startRank()) {

			nonMatchingRank = true;
		}
	}

	private List<Names> collect(NodePattern p) {

		p.collectNames(this);

		config.reset();

		return clearAllRanks();
	}

	private List<Names> clearAllRanks() {

		List<Names> allNamesCopy = new ArrayList<Names>();

		clearRanks(allNamesCopy);

		return allNamesCopy;
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

	private void setNonMatchingRank() {

		rankNames.clear();

		nonMatchingRank = true;
	}

	private int currentRank() {

		return allNames.size() - 1;
	}
}

