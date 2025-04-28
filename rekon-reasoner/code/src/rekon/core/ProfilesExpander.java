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

import rekon.util.*;

/**
 * @author Colin Puleston
 */
class ProfilesExpander {

	static private class OntologyExpander {

		private Ontology ontology;

		private class ExpansionsChecker extends MultiThreadListProcessor<PatternMatcher> {

			private boolean anyNewExpansions = false;

			protected void processElement(PatternMatcher profile) {

				if (checkNewExpansion(profile) && !anyNewExpansions) {

					setNewExpansions();
				}
			}

			boolean checkNewExpansions() {

				anyNewExpansions = false;

				invokeListProcesses(ontology.getAllProfiles());

				return anyNewExpansions;
			}

			private boolean checkNewExpansion(PatternMatcher profile) {

				ProfileRelations prs = getProfileRelations(profile);

				prs.checkExpansion(new ProfileRelationsResolver());

				return prs.newlyExpanded();
			}

			private synchronized void setNewExpansions() {

				anyNewExpansions = true;
			}
		}

		OntologyExpander(Ontology ontology) {

			this.ontology = ontology;

			performAllExpansions();
		}

		private void performAllExpansions() {

			ExpansionsChecker checker = new ExpansionsChecker();
			boolean firstPass = true;

			while (true) {

				initExpansions(firstPass);

				if (!checker.checkNewExpansions() || subsumerOnlyExpansions()) {

					break;
				}

				firstPass = false;
			}
		}

		private void initExpansions(boolean firstPass) {

			for (PatternMatcher p : ontology.getAllProfiles()) {

				initExpansion(p, firstPass);
			}
		}

		private boolean subsumerOnlyExpansions() {

			return !ontology.getConstructInclusions().propertyChains;
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
		prs.checkExpansion(new ProfileRelationsResolver());
	}
}
