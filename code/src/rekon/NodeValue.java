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
class NodeValue extends ObjectValue {

	private Names disjuncts;

	NodeValue(NodeName name) {

		disjuncts = new NameList(name);
	}

	NodeValue(Set<? extends NodeName> disjuncts) {

		this.disjuncts = new NameList(disjuncts);
	}

	NodeValue asNodeValue() {

		return this;
	}

	Names getDisjuncts() {

		return disjuncts;
	}

	void collectNames(NameCollector collector) {

		if (collector.definition()) {

			collectDefinitionNames(collector);
		}
		else {

			collectSignatureNames(collector);
		}
	}

	Collection<Relation> getSignatureRelations() {

		NodePattern p = extractSingleNodeProfile();

		return p != null ? p.getSignatureRelations() : Collections.emptySet();
	}

	boolean subsumesOther(Value v) {

		NodeValue n = v.asNodeValue();

		return n != null && subsumesNodeValue(n);
	}

	void render(ExpressionRenderer r) {

		NodeName n = checkSimpleNodeValue();

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

		NodeName n = checkSimpleNodeValue();

		if (n != null) {

			if (collector.extendedMatch() && n.dynamic()) {

				for (NodePattern d : n.getDefinitions()) {

					d.collectNames(collector);
				}
			}
			else {

				collector.collectFor(n);
			}
		}
		else {

			collector.collectRoot();
		}
	}

	private void collectSignatureNames(NameCollector collector) {

		for (Name d : disjuncts.getNames()) {

			collector.collectFor(d);

			if (collector.extendedMatch()) {

				NodePattern p = ((NodeName)d).getProfile();

				if (p != null) {

					p.collectNames(collector);
				}
			}
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

	private NodePattern extractSingleNodeProfile() {

		NodeName n = checkSimpleNodeValue();

		return n != null ? n.getProfile() : null;
	}

	private NodeName checkSimpleNodeValue() {

		if (disjuncts.size() == 1) {

			return (NodeName)disjuncts.getFirstName();
		}

		return null;
	}
}
