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
class ProfilePatternsExpander {

	static private class OntologyExpander {

		private Ontology ontology;
		private List<PatternMatcher> potentialProfiles = new ArrayList<PatternMatcher>();

		OntologyExpander(Ontology ontology) {

			this.ontology = ontology;

			findPotentialProfiles();
			performAllExpansions();
			resolvePotentialProfiles();
		}

		private void performAllExpansions() {

			List<PatternMatcher> currentProfiles = ontology.getProfilePatterns();
			boolean firstPass = true;

			while (true) {

				initExpansions(currentProfiles, firstPass);
				initExpansions(potentialProfiles, firstPass);

				boolean anyNew = false;

				anyNew |= checkExpansions(currentProfiles);
				anyNew |= checkExpansions(potentialProfiles);

				if (!anyNew) {

					break;
				}

				firstPass = false;
			}
		}

		private void findPotentialProfiles() {

			for (DisjunctionMatcher d : ontology.getAllDisjunctions()) {

				NodeX n = d.getNode();

				if (n.getProfilePatternMatcher() == null) {

					potentialProfiles.add(n.addProfilePatternMatcher());
				}
			}
		}

		private void initExpansions(List<PatternMatcher> profiles, boolean firstPass) {

			for (PatternMatcher p : profiles) {

				initExpansion(p, firstPass);
			}
		}

		private boolean checkExpansions(List<PatternMatcher> profiles) {

			boolean anyNew = false;

			for (PatternMatcher p : profiles) {

				ProfileRelations prs = getProfileRelations(p);
				ProfileRelationsExpander e = new ProfileRelationsExpander(ontology);

				prs.checkExpansion(e, true);

				anyNew |= prs.newlyExpanded();
			}

			return anyNew;
		}

		private void resolvePotentialProfiles() {

			for (PatternMatcher p : potentialProfiles) {

				ProfileRelations prs = getProfileRelations(p);

				if (prs.anyRelations()) {

					ontology.addDerivedProfilePattern(p);
				}
				else {

					p.getNode().removeProfilePatternMatcher();
				}
			}

			potentialProfiles.clear();
		}

		private void initExpansion(PatternMatcher p, boolean firstPass) {

			getProfileRelations(p).initExpansion(firstPass);
		}

		private ProfileRelations getProfileRelations(PatternMatcher p) {

			return p.getPattern().getProfileRelations();
		}
	}

	static void expandAll(Ontology ontology) {

		new OntologyExpander(ontology);
	}

	static void checkExpandLocal(Pattern pattern) {

		ProfileRelations prs = pattern.getProfileRelations();

		prs.initExpansion(true);
		prs.checkExpansion(new ProfileRelationsExpander(), true);
	}
}
