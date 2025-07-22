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
class DynamicSubsumeds {

	private PotentialLocalSubsumeds profilesFilter;

	private abstract class NodeSubsumptions {

		final NameSet subsumeds = new NameSet();

		private PatternMatcher defn;

		NodeSubsumptions(LocalExpression expr) {

			defn = expr.getExpressionMatcher();
		}

		void findAll() {

			for (PatternMatcher c : profilesFilter.getPotentialsFor(defn)) {

				Name n = c.getNode();

				if (requiredCandidate(n) && defn.subsumes(c)) {

					subsumeds.add(n);
				}
			}
		}

		abstract boolean requiredCandidate(Name n);
	}

	private class AllNodeSubsumptions extends NodeSubsumptions {

		AllNodeSubsumptions(LocalExpression expr) {

			super(expr);

			findAll();
		}

		boolean requiredCandidate(Name n) {

			return true;
		}
	}

	private class ClassSubsumptions extends NodeSubsumptions {

		private NameSet filterNames;

		ClassSubsumptions(LocalExpression expr) {

			this(expr, null);
		}

		ClassSubsumptions(LocalExpression expr, NameSet filterNames) {

			super(expr);

			this.filterNames = filterNames;

			findAll();
		}

		boolean requiredCandidate(Name n) {

			if (n instanceof ClassNode) {

				return filterNames == null || filterNames.contains(n);
			}

			return false;
		}
	}

	DynamicSubsumeds(Ontology ontology) {

		profilesFilter = createProfilesFilter(ontology);
	}

	DynamicSubsumeds(PotentialLocalSubsumeds profilesFilter) {

		this.profilesFilter = profilesFilter;
	}

	NameSet inferSubsumedClasses(LocalExpression expr) {

		return new ClassSubsumptions(expr).subsumeds;
	}

	NameSet inferSubsumedClasses(LocalExpression expr, NameSet filterNames) {

		return new ClassSubsumptions(expr, filterNames).subsumeds;
	}

	NameSet inferAllSubsumedNodes(LocalExpression expr) {

		return new AllNodeSubsumptions(expr).subsumeds;
	}

	private PotentialLocalSubsumeds createProfilesFilter(Ontology ontology) {

		return new PotentialLocalSubsumeds(findAllMappedNodeProfiles(ontology));
	}

	private List<PatternMatcher> findAllMappedNodeProfiles(Ontology ontology) {

		List<PatternMatcher> filtered = new ArrayList<PatternMatcher>();

		for (PatternMatcher m : ontology.getAllProfiles()) {

			if (m.getNode().mapped()) {

				filtered.add(m);
			}
		}

		return filtered;
	}
}
