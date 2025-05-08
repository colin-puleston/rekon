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
class SubsBasedComplexStructureBuilder
			extends
				ComplexStructureBuilder<InputClassSubSuper> {

	private MatchStructures matchStructures;
	private ComponentBuilder components;

	private abstract class ComplexSubBuilder<SP> {

		ComplexSubBuilder(InputNode sub, SP sup) {

			ClassNode subCls = complexToClassNode(sub);

			if (subCls != null) {

				ClassNode supCls = resolveSuperClass(sup);

				if (supCls != null) {

					subCls.addSubsumer(supCls);
				}
			}
		}

		abstract ClassNode resolveSuperClass(SP sup);
	}

	private class ComplexSubSimpleSuperBuilder extends ComplexSubBuilder<ClassNode> {

		ComplexSubSimpleSuperBuilder(InputNode sub, ClassNode sup) {

			super(sub, sup);
		}

		ClassNode resolveSuperClass(ClassNode sup) {

			return sup;
		}
	}

	private class AllComplexBuilder extends ComplexSubBuilder<InputNode> {

		AllComplexBuilder(InputNode sub, InputNode sup) {

			super(sub, sup);
		}

		ClassNode resolveSuperClass(InputNode sup) {

			return complexToClassNode(sup);
		}
	}

	SubsBasedComplexStructureBuilder(
		MatchStructures matchStructures,
		InputAxioms axioms,
		ComponentBuilder components) {

		this.matchStructures = matchStructures;
		this.components = components;

		build(axioms.getClassSubSupers());
	}

	InputNode getFirst(InputClassSubSuper axiom) {

		return axiom.getSub();
	}

	InputNode getSecond(InputClassSubSuper axiom) {

		return axiom.getSuper();
	}

	void buildFor(InputNode first, InputNode second) {

		new AllComplexBuilder(first, second);
	}

	void buildFor(InputNode first, ClassNode second) {

		new ComplexSubSimpleSuperBuilder(first, second);
	}

	void buildFor(ClassNode first, InputNode second) {
	}

	private ClassNode complexToClassNode(InputNode in) {

		Pattern p = components.toPattern(in);

		if (p == null) {

			return null;
		}

		NodeX n = p.toSingleNode();

		return n != null ? (ClassNode)n : createDefinitionClass(p);
	}

	private ClassNode createDefinitionClass(Pattern defn) {

		ClassNode c = matchStructures.createFreeClass();

		matchStructures.addDefinition(c, defn);

		return c;
	}
}
