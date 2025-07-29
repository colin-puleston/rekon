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

	private NodeRelations nodeRelations;
	private DataRelations dataRelations;

	private NodeValues nodeValues;

	private abstract class Relations extends EntityBuilder<InputRelation, Relation> {

		Relations(boolean dynamic) {

			super(dynamic);
		}
	}

	private class NodeRelations extends Relations {

		NodeRelations(boolean dynamic) {

			super(dynamic);
		}

		Relation checkCreate(InputRelation source) {

			NodeValue v = nodeValues.get(source.getNodeValue());

			return v != null ? new NodeRelation(source.getNodeProperty(), v) : null;
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

	private class NodeValues extends EntityBuilder<InputNodeValue, NodeValue> {

		NodeValues(boolean dynamic) {

			super(dynamic);
		}

		NodeValue checkCreate(InputNodeValue source) {

			Set<NodeX> disjuncts = new HashSet<NodeX>();

			for (InputNode in : source.getDisjuncts()) {

				NodeX n = componentBuilder.toSingleValueNode(in);

				if (n == null) {

					return null;
				}

				disjuncts.add(n);
			}

			return new NodeValue(disjuncts);
		}
	}

	RelationBuilder(ComponentBuilder componentBuilder, boolean dynamic) {

		this.componentBuilder = componentBuilder;

		nodeRelations = new NodeRelations(dynamic);
		dataRelations = new DataRelations(dynamic);

		nodeValues = new NodeValues(dynamic);
	}

	Relation toRelation(InputRelation source) {

		switch (source.getRelationType()) {

			case NODE_VALUED:

				return nodeRelations.get(source);

			case DATA_VALUED:

				return dataRelations.get(source);

			case OUT_OF_SCOPE:

				return null;
		}

		throw new Error("Unexpected relation-type: " + source.getRelationType());
	}
}
