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

package rekon.owl.convert;

import java.util.*;

import org.semanticweb.owlapi.model.*;

import rekon.build.input.*;
import rekon.util.*;

/**
 * @author Colin Puleston
 */
abstract class TypeAxiomConverter
					<S extends OWLAxiom, I extends InputAxiom>
					extends CompoundIterable<I>{

	private List<List<I>> inputAxiomsByThread = new ArrayList<List<I>>();

	TypeAxiomConverter(AxiomConverter parentConverter) {

		parentConverter.addTypeAxiomConverter(this);

		for (int i = 0 ; i < MultiThreadProcessor.usableProcessors() ; i++) {

			List<I> threadAxs = new ArrayList<I>();

			inputAxiomsByThread.add(threadAxs);

			addComponent(threadAxs);
		}
	}

	boolean addAxiom(OWLAxiom source, int threadIndex) {

		S typeSource = checkCastSourceAxiom(source);

		if (typeSource != null) {

			for (S resSource : resolveSourceAxiom(typeSource)) {

				I input = checkConvert(resSource);

				if (input != null) {

					inputAxiomsByThread.get(threadIndex).add(input);
				}
			}

			return true;
		}

		return false;
	}

	abstract Class<S> getSourceAxiomType();

	Collection<S> resolveSourceAxiom(S ax) {

		return Collections.singleton(ax);
	}

	abstract I checkConvert(S source);

	private S checkCastSourceAxiom(OWLAxiom ax) {

		Class<S> type = getSourceAxiomType();

		return type.isAssignableFrom(ax.getClass()) ? type.cast(ax) : null;
	}
}
