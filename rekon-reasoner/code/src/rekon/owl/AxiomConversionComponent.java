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

import java.util.*;

import org.semanticweb.owlapi.model.*;

import rekon.build.input.*;
import rekon.util.*;

/**
 * @author Colin Puleston
 */
abstract class AxiomConversionComponent {

	abstract class TypeAxiomResolver<A extends OWLAxiom> {

		TypeAxiomResolver() {

			addTypeAxiomResolver(this);
		}

		Collection<? extends OWLAxiom> checkResolve(OWLAxiom axiom) {

			A typeAxiom = checkCastTo(axiom, getAxiomType());

			return typeAxiom != null ? resolveAxiomOfType(typeAxiom) : null;
		}

		abstract Class<A> getAxiomType();

		abstract Collection<? extends OWLAxiom> resolveAxiomOfType(A axiom);
	}

	abstract class TypeAxiomConverter<S extends OWLAxiom, I extends InputAxiom> {

		private List<List<I>> inputAxiomsByThread = new ArrayList<List<I>>();

		TypeAxiomConverter() {

			addTypeAxiomConverter(this);

			for (int i = 0 ; i < MultiThreadProcessor.usableProcessors() ; i++) {

				inputAxiomsByThread.add(new ArrayList<I>());
			}
		}

		boolean checkConvert(OWLAxiom source, int threadIndex) {

			S typeSource = checkCastTo(source, getSourceAxiomType());

			if (typeSource != null) {

				I input = checkConvertType(typeSource);

				if (input != null) {

					inputAxiomsByThread.get(threadIndex).add(input);
				}

				return true;
			}

			return false;
		}

		Iterable<I> getInputAxioms() {

			CompoundIterable<I> all = new CompoundIterable<I>();

			for (List<I> threadAxs : inputAxiomsByThread) {

				all.addComponent(threadAxs);
			}

			return all;
		}

		abstract Class<S> getSourceAxiomType();

		abstract I checkConvertType(S source);
	}

	abstract void addTypeAxiomResolver(TypeAxiomResolver<?> resolver);

	abstract void addTypeAxiomConverter(TypeAxiomConverter<?, ?> converter);

	private <T extends OWLAxiom>T checkCastTo(OWLAxiom source, Class<T> type) {

		return type.isAssignableFrom(source.getClass()) ? type.cast(source) : null;
	}
}
