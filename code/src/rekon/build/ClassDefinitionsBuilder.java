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

		void checkCreate(InputEquivalence<?, ?> axiom, InputComplexNode... complexEquivs) {

			if (absorbComplexEquivs(complexEquivs)) {

				create();
			}
		}

		abstract ClassNode resolveDefinedClass();

		private void create() {

			ClassNode c = resolveDefinedClass();

			for (Pattern defn : patternDefns) {

				addDefinitionPattern(c, defn);
			}

			for (List<Pattern> disjuncts : disjunctionDefns) {

				addDefinitionDisjunction(c, disjuncts);
			}
		}

		private boolean absorbComplexEquivs(InputComplexNode... equivs) {

			for (InputComplexNode e : equivs) {

				if (!absorbComplexEquiv(e)) {

					return false;
				}
			}

			return true;
		}

		private boolean absorbComplexEquiv(InputComplexNode e) {

			List<Pattern> djs = components.toPatternDisjunction(e.toNode());

			if (djs == null) {

				return false;
			}

			if (djs.size() == 1) {

				patternDefns.add(djs.get(0));
			}
			else {

				disjunctionDefns.add(djs);
			}

			return true;
		}
	}

	private class ClassComplexEquivalenceBasedBuilder extends EquivalenceBasedBuilder {

		private InputClassComplexEquivalence axiom;

		ClassComplexEquivalenceBasedBuilder(InputClassComplexEquivalence axiom) {

			this.axiom = axiom;

			checkCreate(axiom, axiom.getSecond());
		}

		ClassNode resolveDefinedClass() {

			return axiom.getFirst();
		}
	}

	private class ComplexEquivalenceBasedBuilder extends EquivalenceBasedBuilder {

		ComplexEquivalenceBasedBuilder(InputComplexEquivalence axiom) {

			checkCreate(axiom, axiom.getFirst(), axiom.getSecond());
		}

		ClassNode resolveDefinedClass() {

			return createDefinitionClass();
		}
	}

	private abstract class ComplexSubBasedBuilder<SP> {

		ComplexSubBasedBuilder(InputSubSuper<InputComplexNode, SP> axiom) {

			InputNode sub = axiom.getSub().toNode();
			List<Pattern> subDjs = components.toPatternDisjunction(sub);

			if (subDjs != null) {

				ClassNode supCls = resolveSuperClass(axiom.getSuper());

				if (supCls != null) {

					for (NodeX subCls : resolveDisjunctionToNodes(subDjs)) {

						subCls.addSubsumer(supCls);
					}
				}
			}
		}

		abstract ClassNode resolveSuperClass(SP sup);
	}

	private class ComplexSubClassSuperBasedBuilder extends ComplexSubBasedBuilder<ClassNode> {

		ComplexSubClassSuperBasedBuilder(InputComplexSubClassSuper axiom) {

			super(axiom);
		}

		ClassNode resolveSuperClass(ClassNode sup) {

			return sup;
		}
	}

	private class ComplexSubSuperBasedBuilder extends ComplexSubBasedBuilder<InputComplexSuper> {

		ComplexSubSuperBasedBuilder(InputComplexSubSuper axiom) {

			super(axiom);
		}

		ClassNode resolveSuperClass(InputComplexSuper sup) {

			if (sup.getComplexSuperType() == InputComplexSuperType.DISJUNCTION) {

				return checkCreateDisjunctionSuperClass(sup.asDisjuncts());
			}

			Pattern p = components.toPattern(sup.toNode());

			return p != null ? createDefinitionClass(p) : null;
		}

		private ClassNode checkCreateDisjunctionSuperClass(Collection<InputNode> disjuncts) {

			List<Pattern> djs = components.toPatternDisjunction(disjuncts);

			if (djs == null) {

				return null;
			}

			ClassNode c = createDefinitionClass();

			addProfileDisjunction(c, djs);

			return c;
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
