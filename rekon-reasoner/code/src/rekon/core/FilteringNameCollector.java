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
class FilteringNameCollector {

	static private enum RankStatus {COLLECT, PRE_COLLECT, ROOT_COLLECTED}

	private boolean definition;
	private List<Names> allNames = new ArrayList<Names>();

	class RankCollector extends NameCollector {

		private int rank;
		private RankStatus rankStatus;
		private NameSet rankNames = new NameSet();

		private RankCollector nextRankCollector = null;

		RankCollector() {

			rank = allNames.size();
			rankStatus = getRankInitialStatus();

			allNames.add(rankNames);
		}

		boolean definition() {

			return definition;
		}

		void collectName(Name n) {

			if (rankStatus == RankStatus.COLLECT) {

				if (n.rootName()) {

					setRootCollected();
				}
				else {

					if (profile() || !n.local()) {

						rankNames.add(n);
					}
				}
			}
		}

		void collectNames(Names ns) {

			if (rankStatus == RankStatus.COLLECT) {

				for (Name n : ns) {

					collectName(n);

					if (rankStatus == RankStatus.ROOT_COLLECTED) {

						break;
					}
				}
			}
		}

		boolean continueForNextRelationsRank() {

			return continueForNextRelationsRank(rank);
		}

		NameCollector forNextRank() {

			if (nextRankCollector == null) {

				nextRankCollector = createRankCollector();
			}

			return nextRankCollector;
		}

		boolean preCollectRank(int rank) {

			return false;
		}

		boolean continueForNextRelationsRank(int rank) {

			return rank == 0;
		}

		private RankStatus getRankInitialStatus() {

			return preCollectRank(rank) ? RankStatus.PRE_COLLECT : RankStatus.COLLECT;
		}

		private List<Names> createRankedNamesList() {

			List<Names> rankedNames = new ArrayList<Names>();

			extendRankedNamesList(rankedNames);

			return rankedNames;
		}

		private void extendRankedNamesList(List<Names> rankedNames) {

			rankedNames.add(rankNames);

			if (nextRankCollector != null) {

				nextRankCollector.extendRankedNamesList(rankedNames);
			}
		}

		private void setRootCollected() {

			rankNames = MatchNamesResolver.ROOT_COLLECTED_NAME_SET;
			rankStatus = RankStatus.ROOT_COLLECTED;
		}
	}

	FilteringNameCollector(boolean definition) {

		this.definition = definition;
	}

	List<Names> collect(Pattern p) {

		RankCollector firstRankCollector = createRankCollector();

		p.collectNames(firstRankCollector);

		return firstRankCollector.createRankedNamesList();
	}

	RankCollector createRankCollector() {

		return new RankCollector();
	}
}

