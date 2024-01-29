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
public class CoreBuilder {

	static private class NonCustomisingCustomiser implements BuildCustomiser {

		public NodeX checkToCustomAtomicNode(InputExpression source) {

			return null;
		}
	}

	private OntologyNames names;
	private BuildCustomiser customiser;

	private class StructureBuilderImpl implements StructureBuilder {

		private InputAssertions assertions;
		private BuildLogger logger;

		public void build(MatchStructures matchStructures) {

			MatchComponents comps = createMatchComponents(matchStructures, false);

			new BasicStructureBuilder(assertions);
			new NodeProfilesBuilder(assertions, comps, matchStructures, logger);
			new ComplexStuctureBuilder(assertions, comps, matchStructures, logger);
		}

		StructureBuilderImpl(InputAssertions assertions, BuildLogger logger) {

			this.assertions = assertions;
			this.logger = logger;
		}
	}

	private class GeneralPatternBuilder implements SinglePatternBuilder, MultiPatternBuilder {

		private InputExpression expr;

		public Pattern create(MatchStructures matchStructures) {

			return createMatchComponents(matchStructures, true).toPattern(expr);
		}

		public Collection<Pattern> createAll(MatchStructures matchStructures) {

			return createMatchComponents(matchStructures, true).toPatternDisjunction(expr);
		}

		GeneralPatternBuilder(InputExpression expr) {

			this.expr = expr;
		}
	}

	public CoreBuilder(OntologyNames names) {

		this(names, new NonCustomisingCustomiser());
	}

	public CoreBuilder(OntologyNames names, BuildCustomiser customiser) {

		this.names = names;
		this.customiser = customiser;
	}

	public StructureBuilder createStructureBuilder(
								InputAssertions assertions,
								BuildLogger logger) {

		return new StructureBuilderImpl(assertions, logger);
	}

	public MultiPatternBuilder createMultiPatternBuilder(InputExpression expr) {

		return new GeneralPatternBuilder(expr);
	}

	public SinglePatternBuilder createSinglePatternBuilder(InputExpression expr) {

		return new GeneralPatternBuilder(expr);
	}

	private MatchComponents createMatchComponents(MatchStructures structs, boolean dynamic) {

		return new MatchComponents(names, structs, customiser, dynamic);
	}
}
