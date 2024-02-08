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
class ClassDefinitionsBuilder extends MatchStuctureBuilder {

	private ComponentBuilder components;

	private abstract class EquivalenceBasedBuilder {

		private Set<Pattern> patternDefns = new HashSet<Pattern>();
		private Set<List<Pattern>> disjunctionDefns = new HashSet<List<Pattern>>();

		boolean create(InputComplex... equivs) {

			for (InputComplex e : equivs) {

				if (!absorbEquiv(e)) {

					return false;
				}
			}

			if (!patternDefns.isEmpty() || !disjunctionDefns.isEmpty()) {

				ClassNode c = resolveDefinedClass();

				for (Pattern defn : patternDefns) {

					addDefinitionPattern(c, defn);
				}

				for (List<Pattern> disjuncts : disjunctionDefns) {

					addDefinitionDisjunction(c, disjuncts);
				}
			}

			return true;
		}

		abstract boolean convertOutOfScopeEquiv(InputComplex equiv);

		abstract ClassNode resolveDefinedClass();

		private boolean absorbEquiv(InputComplex equiv) {

			List<Pattern> djs = components.toPatternDisjunction(equiv);

			if (djs == null) {

				if (!convertOutOfScopeEquiv(equiv)) {

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

	private class ClassComplexEquivalenceBasedBuilder extends EquivalenceBasedBuilder {

		private InputClassComplexEquivalence equivs;

		ClassComplexEquivalenceBasedBuilder(InputClassComplexEquivalence equivs) {

			this.equivs = equivs;

			create(equivs.getSecond());
		}

		boolean convertOutOfScopeEquiv(InputComplex equiv) {

			equivs.notifyAxiomOutOfScope();

			return true;
		}

		ClassNode resolveDefinedClass() {

			return equivs.getFirst();
		}
	}

	private class ComplexEquivalenceBasedBuilder extends EquivalenceBasedBuilder {

		ComplexEquivalenceBasedBuilder(InputComplexEquivalence equivs) {

			if (!create(equivs.getFirst(), equivs.getSecond())) {

				equivs.notifyAxiomOutOfScope();
			}
		}

		boolean convertOutOfScopeEquiv(InputComplex equiv) {

			return false;
		}

		ClassNode resolveDefinedClass() {

			return addDefinitionClass();
		}
	}

	private abstract class ComplexSubBasedBuilder<SP> {

		ComplexSubBasedBuilder(InputSubSuper<InputComplex, SP> subSuper) {

			InputComplex sub = subSuper.getSub();
			List<Pattern> subDjs = components.toPatternDisjunction(sub);

			if (subDjs == null) {

				subSuper.notifyAxiomOutOfScope();
			}

			ClassNode supCls = resolveSuperClass(subSuper.getSuper());

			if (supCls != null) {

				for (NodeX subCls : resolveDisjunctionToNodes(subDjs)) {

					subCls.addSubsumer(supCls);
				}
			}
		}

		abstract ClassNode resolveSuperClass(SP sup);
	}

	private class ComplexSubClassSuperBasedBuilder extends ComplexSubBasedBuilder<ClassNode> {

		ComplexSubClassSuperBasedBuilder(InputComplexSubClassSuper subSuper) {

			super(subSuper);
		}

		ClassNode resolveSuperClass(ClassNode sup) {

			return sup;
		}
	}

	private class ComplexSubSuperBasedBuilder extends ComplexSubBasedBuilder<InputComplexSuper> {

		ComplexSubSuperBasedBuilder(InputComplexSubSuper subSuper) {

			super(subSuper);
		}

		ClassNode resolveSuperClass(InputComplexSuper sup) {

			Pattern p = components.toPattern(ComplexSuperConverter.toComplex(sup));

			return p != null ? addDefinitionClass(p) : null;
		}
	}

	ClassDefinitionsBuilder(
		MatchStructures matchStructures,
		InputAxioms axioms,
		ComponentBuilder components) {

		super(matchStructures);

		this.components = components;

		for (InputClassComplexEquivalence ax : axioms.getClassComplexEquivalences()) {

			new ClassComplexEquivalenceBasedBuilder(ax);
		}

		for (InputComplexEquivalence ax : axioms.getComplexEquivalences()) {

			new ComplexEquivalenceBasedBuilder(ax);
		}

		for (InputComplexSubClassSuper ax :  axioms.getComplexSubClassSupers()) {

			new ComplexSubClassSuperBasedBuilder(ax);
		}

		for (InputComplexSubSuper ax :  axioms.getComplexSubSupers()) {

			new ComplexSubSuperBasedBuilder(ax);
		}
	}
}
