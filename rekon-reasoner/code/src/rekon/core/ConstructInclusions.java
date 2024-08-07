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

/**
 * @author Colin Puleston
 */
class ConstructInclusions {

	final boolean propertyDomains;

	final boolean propertyChains;

	final boolean allRelations;

	final boolean disjunctions;

	private enum PropertyAttribute {

		DOMAIN {

			boolean present(NodeProperty p) {

				return p.hasDomain();
			}
		},

		CHAINS {

			boolean present(NodeProperty p) {

				return p.directChains();
			}
		};

		abstract boolean present(NodeProperty p);
	}

	ConstructInclusions(OntologyNames names, Ontology ontology) {

		propertyDomains = properties(names, PropertyAttribute.DOMAIN);
		propertyChains = properties(names, PropertyAttribute.CHAINS);
		allRelations = allRelations(ontology);
		disjunctions = !ontology.getAllDisjunctions().isEmpty();
	}

	private boolean properties(OntologyNames names, PropertyAttribute attr) {

		for (NodeProperty p : names.getNodeProperties()) {

			if (attr.present(p)) {

				return true;
			}
		}

		return false;
	}

	private boolean allRelations(Ontology ontology) {

		for (NodeX n : ontology.getAllNodes()) {

			for (PatternMatcher p : n.getAllPatternMatchers()) {

				for (Relation r : p.getPattern().getDirectRelations()) {

					if (r instanceof AllRelation) {

						return true;
					}
				}
			}
		}

		return false;
	}
}
