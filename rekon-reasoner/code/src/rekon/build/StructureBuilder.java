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
public class StructureBuilder implements StructureCreator {

	private OntologyNames names;
	private InputAxioms axioms;

	public StructureBuilder(OntologyNames names, InputAxioms axioms) {

		this.names = names;
		this.axioms = axioms;
	}

	public void create(MatchStructures matchStructures) {

		ComponentBuilder comps = new ComponentBuilder(names, matchStructures, false);

		new BasicStructureBuilder(names, axioms);
		new SimpleProfilesBuilder(matchStructures, names, axioms, comps);
		new SubsBasedComplexStructureBuilder(matchStructures, axioms, comps);
		new EquivsBasedComplexStructureBuilder(matchStructures, axioms, comps);
	}
}
