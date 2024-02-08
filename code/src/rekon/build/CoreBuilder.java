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
public class CoreBuilder {

	static private class NonCustomisingCustomiser implements BuildCustomiser {

		public NodeX checkToCustomAtomicNode(InputNode source) {

			return null;
		}
	}

	private OntologyNames names;
	private BuildCustomiser customiser;

	private class StructureBuilderImpl implements StructureBuilder {

		private InputAxioms axioms;

		public void build(MatchStructures matchStructures) {

			ComponentBuilder components = createComponentBuilder(matchStructures, false);

			new BasicStructureBuilder(axioms);
			new NodeProfilesBuilder(matchStructures, names, axioms, components);
			new ClassDefinitionsBuilder(matchStructures, axioms, components);
		}

		StructureBuilderImpl(InputAxioms axioms) {

			this.axioms = axioms;
		}
	}

	private class GeneralPatternBuilder implements SinglePatternBuilder, MultiPatternBuilder {

		private InputComplex source;

		public Pattern create(MatchStructures matchStructures) {

			return createComponentBuilder(matchStructures, true).toPattern(source);
		}

		public Collection<Pattern> createAll(MatchStructures matchStructures) {

			return createComponentBuilder(matchStructures, true).toPatternDisjunction(source);
		}

		GeneralPatternBuilder(InputComplex source) {

			this.source = source;
		}
	}

	public CoreBuilder(OntologyNames names) {

		this(names, new NonCustomisingCustomiser());
	}

	public CoreBuilder(OntologyNames names, BuildCustomiser customiser) {

		this.names = names;
		this.customiser = customiser;
	}

	public StructureBuilder createStructureBuilder(InputAxioms axioms) {

		return new StructureBuilderImpl(axioms);
	}

	public MultiPatternBuilder createMultiPatternBuilder(InputComplex source) {

		return new GeneralPatternBuilder(source);
	}

	public SinglePatternBuilder createSinglePatternBuilder(InputComplex source) {

		return new GeneralPatternBuilder(source);
	}

	private ComponentBuilder createComponentBuilder(MatchStructures structs, boolean dynamic) {

		return new ComponentBuilder(names, structs, customiser, dynamic);
	}
}
