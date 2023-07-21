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

	private PatternSubsumers patternSubsumers;
	private PatternSubsumeds patternSubsumeds;

	public Ontology(OntologyNames names, StructureBuilder structureBuilder) {

		nodeNames.addAll(names.getClassNames());
		nodeNames.addAll(names.getIndividualNames());

		allNames.addAll(nodeNames);
		allNames.addAll(names.getNodePropertyNames());
		allNames.addAll(names.getDataPropertyNames());

		NodeMatchers nodeMatchers = createStructure(structureBuilder);

		processAllNamesPostAdditions();

		new OntologyClassifier(allNames, nodeNames, nodeMatchers);

		patternSubsumers = new PatternSubsumers(nodeMatchers);
		patternSubsumeds = new PatternSubsumeds(nodeMatchers);
	}

	public DynamicOps createDynamicOps() {

		return new DynamicOps(this);
	}

	public InstanceOps createInstanceOps() {

		return new InstanceOps(this);
	}

	void addFreeClass(FreeClassName cn) {

		allNames.add(cn);
		nodeNames.add(cn);
	}

	PatternSubsumers getPatternSubsumers() {

		return patternSubsumers;
	}

	PatternSubsumeds getPatternSubsumeds() {

		return patternSubsumeds;
	}

	private void processAllNamesPostAdditions() {

		for (Name n : allNames) {

			n.getClassifier().onPostAssertionAdditions();
		}
	}

	private NodeMatchers createStructure(StructureBuilder builder) {

		NodeMatchers nodeMatchers = new NodeMatchers();

		builder.build(createMatchStructures(nodeMatchers));
		new InverseRelationsAdder(nodeMatchers);

		return nodeMatchers;
	}

	private MatchStructures createMatchStructures(NodeMatchers nodeMatchers) {

		return new MatchStructures(nodeMatchers, new FreeOntologyClasses(this));
	}
}
