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
public class InstanceOps {

	private DynamicSubsumers dynamicSubsumers;
	private DynamicSubsumeds dynamicSubsumeds;

	private class Updater {

		private Set<Instance> added = new HashSet<Instance>();

		void add(Instance instance, SinglePatternBuilder profileBuilder) {

			InstanceNode node = instance.getNode();
			InstancePattern ip = new InstancePattern(node, profileBuilder);

			dynamicSubsumers.inferSubsumers(ip);
			node.completeClassification();

			dynamicSubsumeds.checkAddInstanceOption(node);

			if (instance.addAsReferencer()) {

				instance.setProfileRebuilder(profileBuilder);
			}

			added.add(instance);
			updateReferencers(instance);
		}

		boolean remove(Instance instance) {

			InstanceNode node = instance.getNode();

			node.removeFromClassification();
			dynamicSubsumeds.checkRemoveInstanceOption(node);

			instance.removeAsReferencer();

			updateReferencers(instance);

			return !instance.anyReferencers();
		}

		private void updateReferencers(Instance instance) {

			for (Instance r : instance.getReferencers()) {

				if (!added.contains(r)) {

					remove(r);
					r.reset();
					add(r, r.getProfileRebuilder());
				}
			}
		}
	}

	public InstanceOps(Ontology ontology) {

		dynamicSubsumers = ontology.getDynamicSubsumers();
		dynamicSubsumeds = ontology.getDynamicSubsumeds();
	}

	public void add(Instance instance, SinglePatternBuilder profileBuilder) {

		new Updater().add(instance, profileBuilder);
	}

	public boolean remove(Instance instance) {

		return new Updater().remove(instance);
	}

	public List<Instance> match(MultiPatternBuilder queryBuilder) {

		DynamicExpression q = new DynamicExpression(queryBuilder);
		NameSet matches = dynamicSubsumeds.inferAllSubsumedNodes(q);

		return matches.isEmpty()
				? Collections.emptyList()
				: matchesToInstances(matches);
	}

	public boolean matches(
						MultiPatternBuilder queryBuilder,
						SinglePatternBuilder profileBuilder) {

		DynamicExpression q = new DynamicExpression(queryBuilder);
		DynamicExpression p = new DynamicExpression(profileBuilder);

		return q.getExpressionMatcher().subsumes(p.getExpressionMatcher());
	}

	private List<Instance> matchesToInstances(NameSet matches) {

		Set<Instance> instances = new HashSet<Instance>();

		addInstances(instances, matches.filterForType(InstanceNode.class));

		for (Name n : matches) {

			if (n instanceof ClassNode) {

				addInstances(instances, n.getSubs(InstanceNode.class, false));
			}
		}

		return new ArrayList<Instance>(instances);
	}

	private void addInstances(Set<Instance> instances, Names nodes) {

		for (Name n : nodes) {

			instances.add(((InstanceNode)n).getInstance());
		}
	}
}
