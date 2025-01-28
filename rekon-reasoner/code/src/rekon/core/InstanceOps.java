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

		void add(Instance instance, SinglePatternSource profileBuilder) {

			InstanceNode node = instance.getNode();
			InstancePattern ip = new InstancePattern(node, profileBuilder);

			dynamicSubsumers.inferSubsumers(ip);
			node.completeClassification();
			dynamicSubsumeds.checkAddInstanceOption(node);

			instance.checkAddAsReferencer(profileBuilder);
			added.add(instance);
			updateReferencers(instance);
		}

		void remove(Instance instance) {

			InstanceNode node = instance.getNode();

			dynamicSubsumeds.checkRemoveInstanceOption(node);

			updateReferencers(instance);
			instance.removeAsReferencer();

			node.clearLinks();
		}

		private void updateReferencers(Instance instance) {

			for (Instance r : instance.getReferencers()) {

				if (!added.contains(r)) {

					remove(r);
					r.getNode().resetClassifier();
					add(r, r.getProfileRebuilder());
				}
			}
		}
	}

	public InstanceOps(Ontology ontology) {

		dynamicSubsumers = ontology.getDynamicSubsumers();
		dynamicSubsumeds = ontology.getDynamicSubsumeds();
	}

	public void add(Instance instance, SinglePatternSource profileBuilder) {

		instance.getNode().checkClassifiable();

		new Updater().add(instance, profileBuilder);
	}

	public boolean remove(Instance instance) {

		new Updater().remove(instance);

		if (instance.anyReferencers()) {

			instance.setAsUndefinedRef();

			return false;
		}

		return true;
	}

	public List<Instance> match(MultiPatternSource queryBuilder) {

		DynamicExpression q = createQueryExpression(queryBuilder);
		NameSet matches = dynamicSubsumeds.inferAllSubsumedNodes(q);

		return matches.isEmpty()
				? Collections.emptyList()
				: matchesToInstances(matches);
	}

	public boolean matches(
						MultiPatternSource queryBuilder,
						SinglePatternSource profileBuilder) {

		DynamicExpression q = createQueryExpression(queryBuilder);
		DynamicExpression p = createInstanceExpression(profileBuilder);

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

	private DynamicExpression createQueryExpression(MultiPatternSource source) {

		return checkExpressionCreated(new DynamicExpression(source), "query");
	}

	private DynamicExpression createInstanceExpression(SinglePatternSource source) {

		return checkExpressionCreated(new DynamicExpression(source), "instance");
	}

	private DynamicExpression checkExpressionCreated(DynamicExpression expr, String exprDesc) {

		if (expr.expressionCreated()) {

			return expr;
		}

		throw new InstanceOpsException("Invalid " + exprDesc + "-expression!");
	}
}
