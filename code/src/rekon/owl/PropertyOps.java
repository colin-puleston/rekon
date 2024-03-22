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

package rekon.owl;

import org.semanticweb.owlapi.model.*;

import rekon.core.*;

/**
 * @author Colin Puleston
 */
abstract class PropertyOps<P extends OWLProperty> extends HierarchyEntityOps<P, P> {

	PropertyOps(P topEntity, P bottomEntity) {

		super(topEntity, bottomEntity);
	}

	boolean equivalent(P inObject1, P inObject2) {

		PropertyX p1 = getPropertyName(inObject1);

		if (p1 != null) {

			PropertyX p2 = getPropertyName(inObject2);

			if (p2 != null) {

				return p1.hasEquivalent(p2);
			}
		}

		return false;
	}

	boolean subsumption(P inSup, P inSub) {

		PropertyX pSup = getPropertyName(inSup);

		if (pSup != null) {

			PropertyX pSub = getPropertyName(inSub);

			if (pSub != null) {

				return pSup.subsumes(pSub);
			}
		}

		return false;
	}

	Names getEquivalentNames(P inObject) {

		PropertyX p = getPropertyName(inObject);

		return p != null ? p.getEquivalents() : Names.NO_NAMES;
	}

	Names getSuperNames(P inObject, boolean direct) {

		PropertyX p = getPropertyName(inObject);

		return p != null ? p.getSupers(direct) : Names.NO_NAMES;
	}

	Names getSubNames(P inObject, boolean direct) {

		PropertyX p = getPropertyName(inObject);

		return p != null ? p.getSubs(direct) : Names.NO_NAMES;
	}

	Names getDirectEntitySubs(Name name) {

		return name.getSubs(true);
	}

	abstract PropertyX getPropertyName(P prop);
}
