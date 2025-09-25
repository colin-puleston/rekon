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
abstract class FilteringNameCollector {

	static private enum RankStatus {COLLECT, PRE_COLLECT, ROOT_COLLECTED}

	private List<Names> allNames = new ArrayList<Names>();

	abstract class RankCollector extends NameCollector {

		private RankStatus rankStatus;
		private NameSet rankNames = new NameSet();

		private RankCollector nextRankCollector = null;

		RankCollector() {

			rankStatus = getRankInitialStatus();

			allNames.add(rankNames);
		}

		void collectForDisjunctNodes(DisjunctionNodeValue v) {

			if (definition()) {

				collectNames(v.getMostSpecificCommonDisjunctSubsumers());
			}
			else {

				super.collectForDisjunctNodes(v);
			}
		}

		void collectName(Name n) {

			if (rankStatus == RankStatus.COLLECT) {

				if (n.rootName()) {

					rankNames = new NameSet(n);
					rankStatus = RankStatus.ROOT_COLLECTED;
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

		List<Names> createRankedNamesList() {

			List<Names> rankedNames = new ArrayList<Names>();

			extendRankedNamesList(rankedNames);

			return rankedNames;
		}

		NameCollector forNextRank() {

			if (nextRankCollector == null) {

				nextRankCollector = createRankCollector();
			}

			return nextRankCollector;
		}

		boolean preCollectRank() {

			return false;
		}

		private RankStatus getRankInitialStatus() {

			return preCollectRank() ? RankStatus.PRE_COLLECT : RankStatus.COLLECT;
		}

		private void extendRankedNamesList(List<Names> rankedNames) {

			rankedNames.add(rankNames);

			if (nextRankCollector != null) {

				nextRankCollector.extendRankedNamesList(rankedNames);
			}
		}
	}

	List<Names> collect(Pattern p) {

		RankCollector firstRankCollector = createRankCollector();

		firstRankCollector.collectForPattern(p);

		return firstRankCollector.createRankedNamesList();
	}

	int getCurrentRankCount() {

		return allNames.size();
	}

	abstract RankCollector createRankCollector();
}

