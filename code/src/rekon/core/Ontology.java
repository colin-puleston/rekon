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
public class Ontology {

	private List<Name> allNames = new ArrayList<Name>();
	private List<NodeName> nodeNames = new ArrayList<NodeName>();

	private Collection<ObjectPropertyName> objectPropertyNames;
	private Collection<DataPropertyName> dataPropertyNames;

	private MatchableNodes matchables = new MatchableNodes();

	public Ontology(OntologyInitialiser initialiser) {

		initialiser.setMatchStructures(createMatchStructures());
		initialiser.createNameHierarchies();

		nodeNames.addAll(initialiser.getClassNames());
		nodeNames.addAll(initialiser.getIndividualNames());

		objectPropertyNames = initialiser.getObjectPropertyNames();
		dataPropertyNames = initialiser.getDataPropertyNames();

		initialiser.createNodeProfiles();
		initialiser.createClassDefinitions();

		new InverseRelationsAdder(matchables);

		allNames.addAll(nodeNames);
		allNames.addAll(objectPropertyNames);
		allNames.addAll(dataPropertyNames);

		processAllNamesPostAdditions();

		new OntologyClassifier(this);
	}

	public DynamicOps createDynamicOps() {

		return new DynamicOps(this);
	}

	List<Name> getAllNames() {

		return allNames;
	}

	List<NodeName> getNodeNames() {

		return nodeNames;
	}

	MatchableNodes getMatchables() {

		return matchables;
	}

	private void processAllNamesPostAdditions() {

		for (Name n : allNames) {

			n.getClassifier().onPostAssertionAdditions();
		}
	}

	private MatchStructures createMatchStructures() {

		return new MatchStructures(matchables, new FreeOntologyClasses(nodeNames));
	}
}
