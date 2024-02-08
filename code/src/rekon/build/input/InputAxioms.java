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

package rekon.build.input;

import java.util.*;

/**
 * @author Colin Puleston
 */
public interface InputAxioms {

	public Collection<InputClassEquivalence> getClassEquivalences();

	public Collection<InputClassComplexEquivalence> getClassComplexEquivalences();

	public Collection<InputComplexEquivalence> getComplexEquivalences();

	public Collection<InputClassSubSuper> getClassSubSupers();

	public Collection<InputClassSubComplexSuper> getClassSubComplexSupers();

	public Collection<InputComplexSubClassSuper> getComplexSubClassSupers();

	public Collection<InputComplexSubSuper> getComplexSubSupers();

	public Collection<InputIndividualEquivalence> getIndividualEquivalences();

	public Collection<InputIndividualClassType> getIndividualClassTypes();

	public Collection<InputIndividualComplexType> getIndividualComplexTypes();

	public Collection<InputIndividualRelation> getIndividualRelations();

	public Collection<InputNodePropertyEquivalence> getNodePropertyEquivalences();

	public Collection<InputNodePropertySubSuper> getNodePropertySubSupers();

	public Collection<InputNodePropertyInverse> getNodePropertyInverses();

	public Collection<InputNodePropertyChain> getNodePropertyChains();

	public Collection<InputNodePropertySymmetric> getNodePropertySymmetrics();

	public Collection<InputNodePropertyTransitive> getNodePropertyTransitives();

	public Collection<InputDataPropertyEquivalence> getDataPropertyEquivalences();

	public Collection<InputDataPropertySubSuper> getDataPropertySubSupers();
}