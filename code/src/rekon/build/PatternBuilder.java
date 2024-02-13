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
class PatternBuilder {

	private ComponentBuilder componentBuilder;
	private ClassNode rootClassNode;

	private Patterns patterns;

	private class PatternSpec {

		private List<InputNode> conjuncts = new ArrayList<InputNode>();

		public boolean equals(Object other) {

			return conjuncts.equals(((PatternSpec)other).conjuncts);
		}

		public int hashCode() {

			return conjuncts.hashCode();
		}

		PatternSpec(InputNode source) {

			if (source.hasComplexType(InputComplexType.CONJUNCTION)) {

				conjuncts.addAll(source.asComplex().asConjuncts());
			}
			else {

				conjuncts.add(source);
			}
		}

		List<InputNode> getConjuncts() {

			return conjuncts;
		}
	}

	private class PatternDisjunctionSpec {

		private Set<PatternSpec> disjuncts = new HashSet<PatternSpec>();

		PatternDisjunctionSpec(InputComplex source) {

			if (source.hasComplexType(InputComplexType.DISJUNCTION)) {

				addDisjunctsFor(source.asDisjuncts());
			}
			else {

				addDisjunctFor(source.toNode());
			}
		}

		List<Pattern> checkCreate() {

			List<Pattern> patternDisjuncts = new ArrayList<Pattern>();

			for (PatternSpec d : disjuncts) {

				Pattern pd = patterns.get(d);

				if (pd == null) {

					return null;
				}

				patternDisjuncts.add(pd);
			}

			return patternDisjuncts;
		}

		private void addDisjunctsFor(Collection<InputNode> source) {

			for (InputNode n : source) {

				addDisjunctFor(n);
			}
		}

		private void addDisjunctFor(InputNode source) {

			disjuncts.add(new PatternSpec(source));
		}
	}

	private class Patterns extends EntityBuilder<PatternSpec, Pattern> {

		private class ConjunctsConverter {

			private NameSet nodes = new NameSet();
			private Set<Relation> rels = new HashSet<Relation>();

			Pattern checkCreate(Collection<InputNode> conjuncts) {

				for (InputNode c : conjuncts) {

					if (!processConjunct(c)) {

						return null;
					}
				}

				if (nodes.isEmpty()) {

					nodes.add(rootClassNode);
				}

				return new Pattern(nodes, rels);
			}

			private boolean processConjunct(InputNode conjunct) {

				switch (conjunct.getNodeType()) {

					case CLASS:

						nodes.absorb(conjunct.asClassNode());

						return true;

					case INDIVIDUAL:

						return false;

					case COMPLEX:

						return processComplexConjunct(conjunct.asComplex());

					case OUT_OF_SCOPE:

						return false;

				}

				throw new Error("Unexpected node-type: " + conjunct.getNodeType());
			}

			private boolean processComplexConjunct(InputComplex conjunct) {

				switch (conjunct.getComplexType()) {

					case CONJUNCTION:

						return false;

					case DISJUNCTION:

						NodeX n = checkCreateNodeForDisjunction(conjunct.asDisjuncts());

						if (n == null) {

							return false;
						}

						nodes.absorb(n);

						return true;

					case RELATION:

						Relation r = componentBuilder.toRelation(conjunct.asRelation());

						if (r == null) {

							return false;
						}

						rels.add(r);

						return true;

					case OUT_OF_SCOPE:

						return false;
				}

				throw new Error("Unexpected complex-type: " + conjunct.getComplexType());
			}
		}

		Patterns(boolean dynamic) {

			super(dynamic);
		}

		Pattern get(InputComplex source) {

			return get(new PatternSpec(source.toNode()));
		}

		Pattern checkCreate(PatternSpec source) {

			List<InputNode> conjuncts = source.getConjuncts();

			if (conjuncts.size() == 1) {

				return checkCreate(conjuncts.get(0));
			}

			return checkCreateForConjuncts(conjuncts);
		}

		private Pattern checkCreate(InputNode source) {

			switch (source.getNodeType()) {

				case CLASS:

					return new Pattern(source.asClassNode());

				case INDIVIDUAL:

					return new Pattern(source.asIndividualNode());

				case COMPLEX:

					return checkCreateForComplex(source.asComplex());

				case OUT_OF_SCOPE:

					return null;
			}

			throw new Error("Unexpected node-type: " + source.getNodeType());
		}

		private Pattern checkCreateForComplex(InputComplex source) {

			switch (source.getComplexType()) {

				case CONJUNCTION:

					return checkCreateForConjuncts(source.asConjuncts());

				case DISJUNCTION:

					return null;

				case RELATION:

					return checkCreateForRelation(source.asRelation());

				case OUT_OF_SCOPE:

					return null;
			}

			throw new Error("Unexpected complex-type: " + source.getComplexType());
		}

		private Pattern checkCreateForConjuncts(Collection<InputNode> source) {

			return new ConjunctsConverter().checkCreate(source);
		}

		private Pattern checkCreateForRelation(InputRelation source) {

			Relation r = componentBuilder.toRelation(source);

			return r != null ? new Pattern(rootClassNode, r) : null;
		}

		private NodeX checkCreateNodeForDisjunction(Collection<InputNode> source) {

			Collection<NodeX> disjuncts = componentBuilder.toDisjunction(source);

			if (disjuncts == null || disjuncts.isEmpty()) {

				return null;
			}

			return componentBuilder.disjunctsToAtomicNode(disjuncts);
		}
	}

	PatternBuilder(ComponentBuilder componentBuilder, OntologyNames names, boolean dynamic) {

		this.componentBuilder = componentBuilder;

		rootClassNode = names.getRootClassNode();
		patterns = new Patterns(dynamic);
	}

	Pattern toPattern(InputComplex source) {

		return patterns.get(source);
	}

	List<Pattern> toPatternDisjunction(InputComplex source) {

		return new PatternDisjunctionSpec(source).checkCreate();
	}
}
