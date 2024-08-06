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
public class Ontology {

	private MultiIterable<Name> allNames = new MultiIterable<Name>();
	private MultiIterable<NodeX> allNodes = new MultiIterable<NodeX>();

	private List<ClassNode> freeClassNodes = new ArrayList<ClassNode>();

	private List<PatternMatcher> profilePatterns = new ArrayList<PatternMatcher>();
	private List<PatternMatcher> definitionPatterns = new ArrayList<PatternMatcher>();

	private List<DisjunctionMatcher> allDisjunctions = new ArrayList<DisjunctionMatcher>();
	private List<DisjunctionMatcher> definitionDisjunctions = new ArrayList<DisjunctionMatcher>();

	private DerivedDisjunctions derivedDisjunctions;

	private DynamicSubsumers dynamicSubsumers;
	private DynamicSubsumeds dynamicSubsumeds;

	private ConstructInclusions inclusions;

	public Ontology(OntologyNames names, StructureBuilder structureBuilder) {

		FreeOntologyClasses freeClasses = new FreeOntologyClasses(this);
		MatchStructures matchStructs = new MatchStructures(freeClasses);

		derivedDisjunctions = new DerivedDisjunctions(matchStructs, freeClasses);

		structureBuilder.build(matchStructs);

		allNodes.addComponent(names.getClassNodes());
		allNodes.addComponent(names.getIndividualNodes());
		allNodes.addComponent(freeClassNodes);

		allNames.addComponent(allNodes);
		allNames.addComponent(names.getNodeProperties());
		allNames.addComponent(names.getDataProperties());

		inclusions = new ConstructInclusions(names, this);

		expandAllNameSubsumers();
		addAllNodeMatchers();
	}

	public void classify(OntologyClassifyListener classifyListener) {

		new OntologyClassifier(this, classifyListener);

		dynamicSubsumers = new DynamicSubsumers(this);
		dynamicSubsumeds = new DynamicSubsumeds(this);
	}

	public Queryables createQueryables() {

		return new Queryables(this);
	}

	public InstanceOps createInstanceOps() {

		return new InstanceOps(this);
	}

	void addFreeClass(ClassNode cn) {

		if (cn.mapped()) {

			throw new Error("Not a free ClassNode!");
		}

		freeClassNodes.add(cn);
	}

	void addDerivedProfilePattern(PatternMatcher p) {

		profilePatterns.add(p);
	}

	ConstructInclusions getConstructInclusions() {

		return inclusions;
	}

	Iterable<Name> getAllNames() {

		return allNames;
	}

	Iterable<NodeX> getAllNodes() {

		return allNodes;
	}

	List<PatternMatcher> getProfilePatterns() {

		return profilePatterns;
	}

	List<PatternMatcher> getDefinitionPatterns() {

		return definitionPatterns;
	}

	List<DisjunctionMatcher> getAllDisjunctions() {

		return allDisjunctions;
	}

	List<DisjunctionMatcher> getDefinitionDisjunctions() {

		return definitionDisjunctions;
	}

	DerivedDisjunctions getDerivedDisjunctions() {

		return derivedDisjunctions;
	}

	DynamicSubsumers getDynamicSubsumers() {

		return dynamicSubsumers;
	}

	DynamicSubsumeds getDynamicSubsumeds() {

		return dynamicSubsumeds;
	}

	private void expandAllNameSubsumers() {

		for (Name n : allNames) {

			n.getClassifier().expandSubsumers();
		}
	}

	private void addAllNodeMatchers() {

		for (NodeX n : allNodes) {

			profilePatterns.addAll(n.getProfilePatternMatcherAsList());
			definitionPatterns.addAll(n.getDefinitionPatternMatchers());
			allDisjunctions.addAll(n.getAllDisjunctionMatchers());
		}

		addDefinitionDisjunctions();
	}

	private void addDefinitionDisjunctions() {

		for (DisjunctionMatcher d : allDisjunctions) {

			if (d.definition()) {

				definitionDisjunctions.add(d);
			}
		}
	}
}
