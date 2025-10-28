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
class ComponentBuilder {

	static private class NonCustomisingCustomiser implements BuildCustomiser {

		public NodeX checkToCustomNode(InputNode source) {

			return null;
		}
	}

	private MatchStructures structures;
	private BuildCustomiser customiser = new NonCustomisingCustomiser();

	private PatternBuilder patternBuilder;
	private RelationBuilder relationBuilder;

	private Map<InputNode, ClassNode> patternClasses = new HashMap<InputNode, ClassNode>();

	ComponentBuilder(OntologyNames names, MatchStructures structures, boolean dynamic) {

		this.structures = structures;

		patternBuilder = new PatternBuilder(this, names, dynamic);
		relationBuilder = new RelationBuilder(this, dynamic);
	}

	void setCustomiser(BuildCustomiser customiser) {

		this.customiser = customiser;
	}

	Pattern toPattern(InputNode source) {

		return patternBuilder.toPattern(source);
	}

	Relation toRelation(InputRelation source) {

		return relationBuilder.toRelation(source);
	}

	NodeX toSingleValueNode(InputNode source) {

		NodeX customNode = customiser.checkToCustomNode(source);

		if (customNode != null) {

			return customNode;
		}

		switch (source.getNodeType()) {

			case CLASS:

				return source.asClassNode();

			case INDIVIDUAL:

				return source.asIndividualNode();

			case CONJUNCTION:
			case RELATION:

				return toPatternClassNode(source);

			case OUT_OF_SCOPE:

				return null;
		}

		throw new Error("Unexpected node-type: " + source.getNodeType());
	}

	private ClassNode toPatternClassNode(InputNode source) {

		ClassNode pCls = patternClasses.get(source);

		if (pCls == null) {

			Pattern p = toPattern(source);

			if (p != null) {

				pCls = structures.createFreeClass();

				structures.addProfile(pCls, p);

				patternClasses.put(source, pCls);
			}
		}

		return pCls;
	}
}
