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

package rekon;

import java.util.*;

/**
 * @author Colin Puleston
 */
abstract class FreeClasses {

	static private class GCIName extends FreeClassName {

		GCIName(int index) {

			super(index);
		}
	}

	static private class OntologyExprName extends FreeClassName {

		OntologyExprName(int index) {

			super(index);
		}
	}

	static private class DynamicExprName extends FreeClassName {

		DynamicExprName(int index) {

			super(index);
		}

		boolean dynamic() {

			return true;
		}
	}

	static private abstract class OntologyNames extends FreeClasses {

		private Collection<NodeName> ontologyNodeNames;

		OntologyNames(MatchableNodes matchables, Collection<NodeName> ontologyNodeNames) {

			super(matchables);

			this.ontologyNodeNames = ontologyNodeNames;
		}

		FreeClassName create(ClassName sup, Collection<NodePattern> defns) {

			FreeClassName n = super.create(sup, defns);

			ontologyNodeNames.add(n);

			return n;
		}
	}

	static private class GCINames extends OntologyNames {

		GCINames(MatchableNodes matchables, Collection<NodeName> ontologyNodeNames) {

			super(matchables, ontologyNodeNames);
		}

		FreeClassName createType(int index) {

			return new GCIName(index);
		}
	}

	static private class OntologyExprNames extends OntologyNames {

		OntologyExprNames(MatchableNodes matchables, Collection<NodeName> ontologyNodeNames) {

			super(matchables, ontologyNodeNames);
		}

		FreeClassName createType(int index) {

			return new OntologyExprName(index);
		}
	}

	static private class DynamicExprNames extends FreeClasses {

		DynamicExprNames(MatchableNodes matchables) {

			super(matchables);
		}

		FreeClassName createType(int index) {

			return new DynamicExprName(index);
		}
	}

	static FreeClasses forGCIs(
							MatchableNodes matchables,
							Collection<NodeName> ontologyNodeNames) {

		return new GCINames(matchables, ontologyNodeNames);
	}

	static FreeClasses forOntologyExprs(
							MatchableNodes matchables,
							Collection<NodeName> ontologyNodeNames) {

		return new OntologyExprNames(matchables, ontologyNodeNames);
	}

	static FreeClasses forDynamicExprs(MatchableNodes matchables) {

		return new DynamicExprNames(matchables);
	}

	private MatchableNodes matchables;
	private int index = 0;

	FreeClassName create(NodePattern defn) {

		return create(null, defn);
	}

	FreeClassName create(Collection<NodePattern> defns) {

		return create(null, defns);
	}

	FreeClassName create(ClassName sup, NodePattern defn) {

		return create(sup, Collections.singleton(defn));
	}

	FreeClassName create(ClassName sup, Collection<NodePattern> defns) {

		FreeClassName n = createType(index++);
		NameClassifier classifier = n.getClassifier();

		if (sup != null) {

			classifier.addAssertedSubsumer(sup);
		}

		for (NodePattern defn : defns) {

			classifier.addAssertedSubsumers(defn.getNames());
		}

		classifier.onPostAssertionAdditions();
		matchables.addForFreeClass(n, defns);

		return n;
	}

	abstract FreeClassName createType(int index);

	private FreeClasses(MatchableNodes matchables) {

		this.matchables = matchables;
	}
}
