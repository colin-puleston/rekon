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

import org.semanticweb.owlapi.model.*;

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class OntologyInitialiserImpl implements OntologyInitialiser {

	private Assertions assertions;

	private MappedNames mappedNames;
	private PatternComponents patternComponents;

	OntologyInitialiserImpl(OWLOntologyManager manager) {

		assertions = new Assertions(manager);
	}

	public void startInitialisation(PatternClasses patternClasses) {

		mappedNames = new MappedNames(assertions);
		patternComponents = new PatternComponents(mappedNames, patternClasses, false);
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

	public void createClassMatchables(MatchableNodes matchables) {

		ClassMatchablesCreator c = new ClassMatchablesCreator(assertions, patternComponents);

		for (ClassName n : mappedNames.getClassNames()) {

			c.createForClass(n, matchables);
		}

		for (IndividualName n : mappedNames.getIndividualNames()) {

			c.createForIndividual(n, matchables);
		}
	}

	public void createGCIs(GCIClasses gciClasses) {

		new GCIsCreator(assertions, mappedNames, patternComponents, gciClasses);
	}

	DynamicOpsInvoker createDynamicOpsInvoker(Ontology ontology) {

		return new DynamicOpsInvoker(ontology.createDynamicOps(), mappedNames, patternComponents);
	}
}
