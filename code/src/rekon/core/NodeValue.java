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
public class NodeValue extends ObjectValue {

	private Names disjuncts;

	public NodeValue(NodeName name) {

		disjuncts = new NameList(name);
	}

	public NodeValue(Set<? extends NodeName> disjuncts) {

		this.disjuncts = new NameList(disjuncts);
	}

	NodeValue asNodeValue() {

		return this;
	}

	NodeName asNodeName() {

		if (disjuncts.size() == 1) {

			return (NodeName)disjuncts.getFirstName();
		}

		return null;
	}

	Names getDisjuncts() {

		return disjuncts;
	}

	void registerDefinitionRefedNames() {

		disjuncts.registerAsDefinitionRefed(PatternNameRole.VALUE);
	}

	void collectNames(NameCollector collector) {

		if (collector.definition()) {

			collectDefinitionNames(collector);
		}
		else {

			collectSignatureNames(collector);
		}
	}

	Collection<Relation> collectSignatureRelations(NameSet visitedNodes) {

		NodeName n = asNodeName();

		if (n != null) {

			NodePattern p = n.getProfile();

			if (p != null && visitedNodes.add(n)) {

				return p.resolveSignatureRelations(visitedNodes);
			}
		}

		return Collections.emptySet();
	}

	boolean subsumesOther(Value v) {

		NodeValue n = v.asNodeValue();

		return n != null && subsumesNodeValue(n);
	}

	boolean classifyTarget(boolean initialPass) {

		for (Name d : disjuncts.getNames()) {

			if (((NodeName)d).classifyTargetValue(initialPass)) {

				return true;
			}
		}

		return false;
	}

	boolean newSubsumers(NodeMatcher matcher) {

		for (Name d : disjuncts.getNames()) {

			if (((NodeName)d).newSubsumers(matcher)) {

				return true;
			}
		}

		return false;
	}

	void render(PatternRenderer r) {

		NodeName n = asNodeName();

		if (n != null) {

			r.addLine(n.getLabel());
		}
		else {

			r.addLine("OR");

			r = r.nextLevel();

			for (Name d : disjuncts.getNames()) {

				r.addLine(d.getLabel());
			}
		}
	}

	private void collectDefinitionNames(NameCollector collector) {

		NodeName n = asNodeName();

		if (n != null) {

			collector.collectForDefinitionValue(n);
		}
		else {

			collector.collectRoot();
		}
	}

	private void collectSignatureNames(NameCollector collector) {

		for (Name d : disjuncts.getNames()) {

			collector.collectForSignatureValue((NodeName)d);
		}
	}

	private boolean subsumesNodeValue(NodeValue n) {

		for (Name d : n.disjuncts.getNames()) {

			if (!subsumesNodeName((NodeName)d)) {

				return false;
			}
		}

		return true;
	}

	private boolean subsumesNodeName(NodeName n) {

		for (Name d : disjuncts.getNames()) {

			if (nodeNameSubsumption((NodeName)d, n)) {

				return true;
			}
		}

		return false;
	}

	private boolean nodeNameSubsumption(NodeName subsumer, NodeName subsumed) {

		if (subsumer.dynamic()) {

			NodePattern pSubsumed = subsumed.getProfile();

			return pSubsumed != null && patternSubsumption(subsumer, pSubsumed);
		}

		return subsumer.subsumes(subsumed);
	}

	private boolean patternSubsumption(NodeName subsumer, NodePattern pSubsumed) {

		for (NodePattern pSubsumer : subsumer.getDefinitions()) {

			if (pSubsumer.subsumes(pSubsumed)) {

				return true;
			}
		}

		return false;
	}
}
