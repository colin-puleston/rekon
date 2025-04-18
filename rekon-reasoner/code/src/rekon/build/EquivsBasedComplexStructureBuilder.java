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
class EquivsBasedComplexStructureBuilder {

	private MatchStructures matchStructures;
	private ComponentBuilder components;

	private abstract class EquivalenceBasedBuilder {

		private List<Pattern> definitions = new ArrayList<Pattern>();

		void checkCreate(InputEquivalence<?, ?> axiom, InputComplexSuper... complexEquivs) {

			if (absorbComplexEquivs(complexEquivs)) {

				create();
			}
		}

		abstract ClassNode resolveDefinitionClass();

		private void create() {

			ClassNode c = resolveDefinitionClass();

			for (Pattern defn : definitions) {

				matchStructures.addDefinitionPattern(c, defn);
			}
		}

		private boolean absorbComplexEquivs(InputComplexSuper... equivs) {

			for (InputComplexSuper e : equivs) {

				if (!absorbComplexEquiv(e)) {

					return false;
				}
			}

			return true;
		}

		private boolean absorbComplexEquiv(InputComplexSuper e) {

			List<Pattern> djs = components.toPatternDisjunction(e.toNode());

			if (djs != null && djs.size() == 1) {

				definitions.add(djs.get(0));

				return true;
			}

			return false;
		}
	}

	private class ClassComplexEquivalenceBasedBuilder extends EquivalenceBasedBuilder {

		private ClassNode first;

		ClassComplexEquivalenceBasedBuilder(InputClassComplexEquivalence axiom) {

			first = axiom.getFirst();

			checkCreate(axiom, axiom.getSecond());
		}

		ClassNode resolveDefinitionClass() {

			return first;
		}
	}

	private class ComplexEquivalenceBasedBuilder extends EquivalenceBasedBuilder {

		ComplexEquivalenceBasedBuilder(InputComplexEquivalence axiom) {

			checkCreate(axiom, axiom.getFirst(), axiom.getSecond());
		}

		ClassNode resolveDefinitionClass() {

			return matchStructures.createPatternClass();
		}
	}

	EquivsBasedComplexStructureBuilder(
		MatchStructures matchStructures,
		InputAxioms axioms,
		ComponentBuilder components) {

		this.matchStructures = matchStructures;
		this.components = components;

		for (InputClassComplexEquivalence ax : axioms.getClassComplexEquivalences()) {

			new ClassComplexEquivalenceBasedBuilder(ax);
		}

		for (InputComplexEquivalence ax : axioms.getComplexEquivalences()) {

			new ComplexEquivalenceBasedBuilder(ax);
		}
	}
}
