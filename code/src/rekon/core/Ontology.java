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
	private List<NodeX> nodes = new ArrayList<NodeX>();

	private DynamicSubsumers dynamicSubsumers;
	private DynamicSubsumeds dynamicSubsumeds;

	public Ontology(OntologyNames names, StructureBuilder structureBuilder) {

		nodes.addAll(names.getClassNodes());
		nodes.addAll(names.getIndividualNodes());

		allNames.addAll(nodes);
		allNames.addAll(names.getNodeProperties());
		allNames.addAll(names.getDataProperties());

		structureBuilder.build(createMatchStructures());

		processAllNamesPostAdditions();

		NodeMatchers nodeMatchers = new NodeMatchers(nodes);

		new OntologyClassifier(allNames, nodes, nodeMatchers);

		dynamicSubsumers = new DynamicSubsumers(nodeMatchers);
		dynamicSubsumeds = new DynamicSubsumeds(nodeMatchers);
	}

	public DynamicOps createDynamicOps() {

		return new DynamicOps(this);
	}

	public InstanceOps createInstanceOps() {

		return new InstanceOps(this);
	}

	void addFreeClass(FreeClassNode cn) {

		allNames.add(cn);
		nodes.add(cn);
	}

	DynamicSubsumers getDynamicSubsumers() {

		return dynamicSubsumers;
	}

	DynamicSubsumeds getDynamicSubsumeds() {

		return dynamicSubsumeds;
	}

	private void processAllNamesPostAdditions() {

		for (Name n : allNames) {

			n.getClassifier().onPostAssertionAdditions();
		}
	}

	private MatchStructures createMatchStructures() {

		return new MatchStructures(new FreeOntologyClasses(this));
	}
}
