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

package rekon.build;

import java.util.*;

import rekon.core.*;
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
class RelationBuilder {

	private ComponentBuilder componentBuilder;

	private ClassNode rootClassNode;

	private SomeRelations someRelations;
	private AllRelations allRelations;
	private DataRelations dataRelations;

	private abstract class Relations extends EntityBuilder<InputRelation, Relation> {

		Relations(boolean dynamic) {

			super(dynamic);
		}
	}

	private abstract class NodeRelations extends Relations {

		private boolean complement = false;

		NodeRelations(boolean dynamic) {

			super(dynamic);
		}

		Relation get(InputRelation source, boolean complement) {

			this.complement = complement;

			return source.getRelationType() == handledInputType() ? get(source) : null;
		}

		Relation checkCreate(InputRelation source) {

			return checkCreate(source, complement);
		}

		abstract Relation checkCreate(InputRelation source, boolean complement);

		Relation checkCreateDefault(NodeProperty prop, InputNode value) {

			NodeValue target = toNodeValue(value);

			return target != null ? create(prop, target) : null;
		}

		abstract Relation create(NodeProperty prop, NodeValue target);

		abstract InputRelationType handledInputType();

		abstract NodeValue getNodeValueForEmptyDisjunction();

		Relation createForNoValue(NodeProperty prop) {

			return new AllRelation(prop, OntologyNames.ABSENT_CLASS_VALUE);
		}

		private NodeValue toNodeValue(InputNode source) {

			if (source.hasComplexType(InputComplexType.DISJUNCTION)) {

				return toNodeValue(source.asComplex().asDisjuncts());
			}

			NodeX n = componentBuilder.toAtomicNode(source);

			return n != null ? new NodeValue(n) : null;
		}

		private NodeValue toNodeValue(Collection<InputNode> source) {

			Collection<NodeX> disjuncts = componentBuilder.toDisjunction(source);

			if (disjuncts == null) {

				return null;
			}

			if (disjuncts.isEmpty()) {

				return getNodeValueForEmptyDisjunction();
			}

			return new NodeValue(componentBuilder.disjunctsToAtomicNode(disjuncts));
		}
	}

	private class SomeRelations extends NodeRelations {

		SomeRelations(boolean dynamic) {

			super(dynamic);
		}

		Relation checkCreate(InputRelation source, boolean complement) {

			NodeProperty prop = source.getNodeProperty();
			InputNode value = source.getExpressionValue();

			if (complement) {

				if (isClassNode(value, rootClassNode)) {

					return createForNoValue(prop);
				}

				return null;
			}

			return checkCreateDefault(prop, value);
		}

		Relation create(NodeProperty prop, NodeValue target) {

			return new SomeRelation(prop, target);
		}

		InputRelationType handledInputType() {

			return InputRelationType.SOME_NODES;
		}

		NodeValue getNodeValueForEmptyDisjunction() {

			return null;
		}
	}

	private class AllRelations extends NodeRelations {

		AllRelations(boolean dynamic) {

			super(dynamic);
		}

		Relation checkCreate(InputRelation source, boolean complement) {

			if (complement) {

				return null;
			}

			NodeProperty prop = source.getNodeProperty();
			InputNode value = source.getExpressionValue();

			if (isClassNode(value, OntologyNames.ABSENT_CLASS_NODE)) {

				return createForNoValue(prop);
			}

			return checkCreateDefault(prop, value);
		}

		Relation create(NodeProperty prop, NodeValue target) {

			return new AllRelation(prop, target);
		}

		InputRelationType handledInputType() {

			return InputRelationType.ALL_NODES;
		}

		NodeValue getNodeValueForEmptyDisjunction() {

			return OntologyNames.ABSENT_CLASS_VALUE;
		}
	}

	private class DataRelations extends Relations {

		DataRelations(boolean dynamic) {

			super(dynamic);
		}

		Relation checkCreate(InputRelation source) {

			return new DataRelation(source.getDataProperty(), source.getDataValue());
		}
	}

	RelationBuilder(ComponentBuilder componentBuilder, OntologyNames names, boolean dynamic) {

		this.componentBuilder = componentBuilder;

		someRelations = new SomeRelations(dynamic);
		allRelations = new AllRelations(dynamic);
		dataRelations = new DataRelations(dynamic);

		rootClassNode = names.getRootClassNode();
	}

	Relation toRelation(InputRelation source) {

		return toRelation(source, false);
	}

	private Relation toRelation(InputRelation source, boolean complement) {

		switch (source.getRelationType()) {

			case COMPLEMENT:

				return toRelation(source.asComplemented(), true);

			case SOME_NODES:

				return someRelations.get(source, complement);

			case ALL_NODES:

				return allRelations.get(source, complement);

			case DATA_VALUE:

				return dataRelations.get(source);

			case OUT_OF_SCOPE:

				return null;
		}

		throw new Error("Unexpected relation-type: " + source.getRelationType());
	}

	private boolean isClassNode(InputNode node, ClassNode test) {

		return node.hasNodeType(InputNodeType.CLASS) && node.asClassNode() == test;
	}
}
