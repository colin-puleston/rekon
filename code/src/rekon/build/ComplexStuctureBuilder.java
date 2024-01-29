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

/**
 * @author Colin Puleston
 */
class ComplexStuctureBuilder {

	private MatchComponents matchComponents;
	private MatchStructures matchStructures;

	private BuildLogger logger;

	private abstract class EquivsBasedBuilder {

		private Set<Pattern> patternDefns = new HashSet<Pattern>();
		private Set<List<Pattern>> disjunctionDefns = new HashSet<List<Pattern>>();

		boolean create(Collection<InputExpression> equivs) {

			for (InputExpression e : equivs) {

				if (!absorbEquiv(e)) {

					return false;
				}
			}

			if (!patternDefns.isEmpty() || !disjunctionDefns.isEmpty()) {

				ClassNode c = resolveDefinedClass();

				for (Pattern defn : patternDefns) {

					matchStructures.addDefinitionPattern(c, defn);
				}

				for (List<Pattern> disjuncts : disjunctionDefns) {

					addDisjunction(c, disjuncts, true);
				}
			}

			return true;
		}

		abstract boolean handleOutOfScopeEquiv(InputExpression equiv);

		abstract ClassNode resolveDefinedClass();

		private boolean absorbEquiv(InputExpression equiv) {

			List<Pattern> djs = matchComponents.toPatternDisjunction(equiv);

			if (djs == null) {

				if (!handleOutOfScopeEquiv(equiv)) {

					return false;
				}
			}
			else {

				if (djs.size() == 1) {

					patternDefns.add(djs.get(0));
				}
				else {

					disjunctionDefns.add(djs);
				}
			}

			return true;
		}
	}

	private class ClassEquivalentsBasedBuilder extends EquivsBasedBuilder {

		private InputClass input;

		ClassEquivalentsBasedBuilder(InputClass input) {

			this.input = input;

			create(input.getEquivExprs());
		}

		boolean handleOutOfScopeEquiv(InputExpression equiv) {

			logger.logOutOfScopeEquivalent(input, equiv);

			return true;
		}

		ClassNode resolveDefinedClass() {

			return input.getName();
		}
	}

	private class GCIEquivalentsBasedBuilder extends EquivsBasedBuilder {

		GCIEquivalentsBasedBuilder(InputEquivalence equivs) {

			if (!create(equivs.getEquivs())) {

				logger.logOutOfScopeEquivalence(equivs);
			}
		}

		boolean handleOutOfScopeEquiv(InputExpression equiv) {

			return false;
		}

		ClassNode resolveDefinedClass() {

			return matchStructures.addDefinitionClass();
		}
	}

	private class ClassDisjunctionSupersBasedBuilder {

		private InputClass input;
		private ClassNode subCls;

		ClassDisjunctionSupersBasedBuilder(InputClass input) {

			subCls = input.getName();

			for (InputExpression inputDj : input.getSuperDisjunctionExprs()) {

				checkCreate(inputDj);
			}
		}

		private void checkCreate(InputExpression inputDisjunction) {

			List<Pattern> djs = matchComponents.toPatternDisjunction(inputDisjunction);

			if (djs != null) {

				addDisjunction(subCls, djs, false);
			}
			else {

				logger.logOutOfScopeSuper(input, inputDisjunction);
			}
		}
	}

	private class GCISubSuperBasedBuilder {

		GCISubSuperBasedBuilder(InputSubSuper subSuper) {

			InputExpression sub = subSuper.getSub();
			List<Pattern> subDjs = matchComponents.toPatternDisjunction(sub);

			if (subDjs == null) {

				logger.logOutOfScopeSubSuper(subSuper);
			}

			ClassNode supCls = resolveSuperClass(subSuper.getSuper());

			if (supCls != null) {

				for (NodeX subCls : resolveDisjunctionToNodes(subDjs)) {

					subCls.addSubsumer(supCls);
				}
			}
		}

		private ClassNode resolveSuperClass(InputExpression sup) {

			if (sup.getExpressionType() == InputExpressionType.CLASS) {

				return sup.asClassNode();
			}

			Pattern p = matchComponents.toPattern(sup);

			return p != null ? addDefinitionClass(p) : null;
		}
	}

	ComplexStuctureBuilder(
		InputAssertions assertions,
		MatchComponents matchComponents,
		MatchStructures matchStructures,
		BuildLogger logger) {

		this.matchComponents = matchComponents;
		this.matchStructures = matchStructures;
		this.logger = logger;

		for (InputClass c : assertions.getClasses()) {

			new ClassEquivalentsBasedBuilder(c);
			new ClassDisjunctionSupersBasedBuilder(c);
		}

		for (InputEquivalence equivs : assertions.getEquivalenceGCIs()) {

			new GCIEquivalentsBasedBuilder(equivs);
		}

		for (InputSubSuper subSuper :  assertions.getSubSuperGCIs()) {

			new GCISubSuperBasedBuilder(subSuper);
		}
	}

	private void addDisjunction(
					ClassNode node,
					List<Pattern> disjuncts,
					boolean definition) {

		List<NodeX> nodeDjs = resolveDisjunctionToNodes(disjuncts);

		matchStructures.addDisjunction(node, nodeDjs, definition);
	}

	private List<NodeX> resolveDisjunctionToNodes(List<Pattern> disjuncts) {

		List<NodeX> nodeDjs = new ArrayList<NodeX>();

		for (Pattern d : disjuncts) {

			nodeDjs.add(resolveDisjunctToNode(d));
		}

		return nodeDjs;
	}

	private NodeX resolveDisjunctToNode(Pattern disjunct) {

		NodeX n = disjunct.toSingleNode();

		return n != null ? n : addDefinitionClass(disjunct);
	}

	private ClassNode addDefinitionClass(Pattern defn) {

		ClassNode c = matchStructures.addDefinitionClass();

		matchStructures.addDefinitionPattern(c, defn);

		return c;
	}
}
