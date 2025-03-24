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

		abstract ClassNode resolvePatternDefinitionClass();

		abstract ClassNode resolveDisjunctionDefinitionClass();

		abstract ClassNode resolveMultiDefinitionClass();

		private void create() {

			ClassNode mdc = multiDefinitions() ? resolveMultiDefinitionClass() : null;

			for (Pattern defn : patternDefns) {

				ClassNode c = mdc != null ? mdc : resolvePatternDefinitionClass();

				addDefinitionPattern(c, defn);
			}

			for (List<Pattern> disjuncts : disjunctionDefns) {

				ClassNode c = mdc != null ? mdc : resolveDisjunctionDefinitionClass();

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

		boolean multiDefinitions() {

			return patternDefns.size() + disjunctionDefns.size() > 1;
		}
	}

	private class ClassComplexEquivalenceBasedBuilder extends EquivalenceBasedBuilder {

		private ClassNode first;

		ClassComplexEquivalenceBasedBuilder(InputClassComplexEquivalence axiom) {

			first = axiom.getFirst();

			checkCreate(axiom, axiom.getSecond());
		}

		ClassNode resolvePatternDefinitionClass() {

			return first;
		}

		ClassNode resolveDisjunctionDefinitionClass() {

			return first;
		}

		ClassNode resolveMultiDefinitionClass() {

			throw new Error("Method should never be invoked!");
		}
	}

	private class ComplexEquivalenceBasedBuilder extends EquivalenceBasedBuilder {

		ComplexEquivalenceBasedBuilder(InputComplexEquivalence axiom) {

			checkCreate(axiom, axiom.getFirst(), axiom.getSecond());
		}

		ClassNode resolvePatternDefinitionClass() {

			return createPatternClass();
		}

		ClassNode resolveDisjunctionDefinitionClass() {

			return createDisjunctionClass();
		}

		ClassNode resolveMultiDefinitionClass() {

			return createMultiDefinitionClass();
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

			return p != null ? createPatternClass(p) : null;
		}

		private ClassNode checkCreateDisjunctionSuperClass(Collection<InputNode> disjuncts) {

			List<Pattern> djs = components.toPatternDisjunction(disjuncts);

			if (djs == null) {

				return null;
			}

			ClassNode c = createDisjunctionClass();

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
