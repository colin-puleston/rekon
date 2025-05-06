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
class EquivsBasedComplexStructureBuilder
			extends
				ComplexStructureBuilder<InputClassEquivalence> {

	private MatchStructures matchStructures;
	private ComponentBuilder components;

	private abstract class DefinitionsBuilder {

		private List<Pattern> definitions = new ArrayList<Pattern>();

		void checkCreate(InputNode... complexEquivs) {

			if (absorbComplexEquivs(complexEquivs)) {

				create();
			}
		}

		abstract ClassNode resolveDefinitionClass();

		private void create() {

			ClassNode c = resolveDefinitionClass();

			for (Pattern defn : definitions) {

				matchStructures.addDefinition(c, defn);
			}
		}

		private boolean absorbComplexEquivs(InputNode... equivs) {

			for (InputNode e : equivs) {

				Pattern defn = components.toPattern(e);

				if (defn == null) {

					return false;
				}

				definitions.add(defn);
			}

			return true;
		}
	}

	private class SimpleDefinitionsBuilder extends DefinitionsBuilder {

		private ClassNode cls;

		SimpleDefinitionsBuilder(ClassNode cls, InputNode defn) {

			this.cls = cls;

			checkCreate(defn);
		}

		ClassNode resolveDefinitionClass() {

			return cls;
		}
	}

	private class GCIDefinitionsBuilder extends DefinitionsBuilder {

		GCIDefinitionsBuilder(InputNode equiv1, InputNode equiv2) {

			checkCreate(equiv1, equiv2);
		}

		ClassNode resolveDefinitionClass() {

			return matchStructures.createFreeClass();
		}
	}

	EquivsBasedComplexStructureBuilder(
		MatchStructures matchStructures,
		InputAxioms axioms,
		ComponentBuilder components) {

		this.matchStructures = matchStructures;
		this.components = components;

		build(axioms.getClassEquivalences());
	}

	InputNode getFirst(InputClassEquivalence axiom) {

		return axiom.getFirst();
	}

	InputNode getSecond(InputClassEquivalence axiom) {

		return axiom.getSecond();
	}

	void buildFor(InputNode first, InputNode second) {

		new GCIDefinitionsBuilder(first, second);
	}

	void buildFor(InputNode first, ClassNode second) {

		new SimpleDefinitionsBuilder(second, first);
	}

	void buildFor(ClassNode first, InputNode second) {

		new SimpleDefinitionsBuilder(first, second);
	}
}
