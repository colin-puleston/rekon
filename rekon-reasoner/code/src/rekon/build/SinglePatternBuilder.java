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

import rekon.core.*;
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
public class SinglePatternBuilder implements PatternSource {

	private OntologyNames names;
	private InputNode source;

	private BuildCustomiser customiser = null;

	public SinglePatternBuilder(OntologyNames names, InputNode source) {

		this.names = names;
		this.source = source;
	}

	public void setCustomiser(BuildCustomiser customiser) {

		this.customiser = customiser;
	}

	public Pattern create(MatchStructures matchStructures) {

		ComponentBuilder comps = new ComponentBuilder(names, matchStructures, true);

		if (customiser != null) {

			comps.setCustomiser(customiser);
		}

		return comps.toPattern(source);
	}
}
