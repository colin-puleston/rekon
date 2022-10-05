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

package rekon.owl;

import java.util.*;

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class OntologyInitialiserImpl implements OntologyInitialiser {

	private Assertions assertions;

	private MappedNames mappedNames = null;
	private MatchComponents matchComponents = null;
	private MatchStructures matchStructures = null;

	public void setMatchStructures(MatchStructures matchStructures) {

		this.matchStructures = matchStructures;
	}

	public void createNameHierarchies() {

		mappedNames = new MappedNames(assertions);
		matchComponents = new MatchComponents(mappedNames, matchStructures, false);
	}

	public void createNodeProfiles() {

		new NodeProfilesCreator(assertions, mappedNames, matchComponents, matchStructures);
	}

	public void createClassDefinitions() {

		new ClassDefinitionsCreator(assertions, mappedNames, matchComponents, matchStructures);
	}

	public Collection<ClassName> getClassNames() {

		return mappedNames.getClassNames();
	}

	public Collection<IndividualName> getIndividualNames() {

		return mappedNames.getIndividualNames();
	}

	public Collection<ObjectPropertyName> getObjectPropertyNames() {

		return mappedNames.getObjectPropertyNames();
	}

	public Collection<DataPropertyName> getDataPropertyNames() {

		return mappedNames.getDataPropertyNames();
	}

	OntologyInitialiserImpl(Assertions assertions) {

		this.assertions = assertions;
	}

	DynamicOpsInvoker createDynamicOpsInvoker(Ontology ontology) {

		return new DynamicOpsInvoker(ontology.createDynamicOps(), mappedNames, matchComponents);
	}
}
