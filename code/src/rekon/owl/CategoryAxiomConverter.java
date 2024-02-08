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

import rekon.core.*;
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
class CategoryAxiomConverter extends AxiomConversionComponent {

	final OWLDataFactory factory;
	final MappedNames names;
	final ExpressionConverter expressions;

	private AxiomConverter parentConverter;

	abstract class ConvertedAxiom implements InputAxiom {

		private OWLAxiom source;

		public String toString() {

			return getClass().getSimpleName() + "[" + source + "]";
		}

		public void notifyAxiomOutOfScope() {

			logOutOfScopeAxiom(source);
		}

		ConvertedAxiom(OWLAxiom source) {

			this.source = source;
		}
	}

	abstract class ConvertedEquivalence<F, S>
								extends ConvertedAxiom
								implements InputEquivalence<F, S> {

		private F first;
		private S second;

		public F getFirst() {

			return first;
		}

		public S getSecond(){

			return second;
		}

		ConvertedEquivalence(OWLAxiom source, F first, S second) {

			super(source);

			this.first = first;
			this.second = second;
		}
	}

	abstract class ConvertedSubSuper<SB, SP>
								extends ConvertedAxiom
								implements InputSubSuper<SB, SP> {

		private SB sub;
		private SP sup;

		public SB getSub() {

			return sub;
		}

		public SP getSuper(){

			return sup;
		}

		ConvertedSubSuper(OWLAxiom source, SB sub, SP sup) {

			super(source);

			this.sub = sub;
			this.sup = sup;
		}
	}

	abstract class ConvertedNameEquivalence<N extends Name>
						extends ConvertedEquivalence<N, N>
						implements InputNameEquivalence<N> {

		ConvertedNameEquivalence(OWLAxiom source, N first, N second) {

			super(source, first, second);
		}
	}

	abstract class ConvertedNameSubSuper<N extends Name>
						extends ConvertedSubSuper<N, N>
						implements InputNameSubSuper<N> {

		ConvertedNameSubSuper(OWLAxiom source, N sub, N sup) {

			super(source, sub, sup);
		}
	}

	abstract class OwlLink<E extends OWLObject, N extends Name> {

		final OWLAxiom source;

		final E firstOrSub;
		final E secondOrSup;

		OwlLink(OWLAxiom source, Set<E> equivs) {

			this.source = source;

			Iterator<E> i = equivs.iterator();

			firstOrSub = i.next();
			secondOrSup = i.next();
		}

		OwlLink(OWLAxiom source, E sub, E sup) {

			this.source = source;

			firstOrSub = sub;
			secondOrSup = sup;
		}

		N firstOrSubAsName() {

			return asName(firstOrSub);
		}

		N secondOrSupAsName() {

			return asName(secondOrSup);
		}

		boolean matches(boolean firstOrSubIsName, boolean secondOrSupIsName) {

			return matches(firstOrSub, firstOrSubIsName)
					&& matches(secondOrSup, secondOrSupIsName);
		}

		abstract Class<? extends E> getNameExprType();

		abstract N asName(E e);

		private boolean matches(E e, boolean isName) {

			return isNameExpr(e) == isName;
		}

		private boolean isNameExpr(E test) {

			return getNameExprType().isAssignableFrom(test.getClass());
		}
	}

	CategoryAxiomConverter(AxiomConverter parentConverter) {

		this.parentConverter = parentConverter;

		factory = parentConverter.factory;
		names = parentConverter.names;
		expressions = parentConverter.expressions;
	}

	void addTypeAxiomResolver(TypeAxiomResolver<?> resolver) {

		parentConverter.addTypeAxiomResolver(resolver);
	}

	void addTypeAxiomConverter(TypeAxiomConverter<?, ?> converter) {

		parentConverter.addTypeAxiomConverter(converter);
	}

	<I extends InputAxiom,
	H extends TypeAxiomConverter<?, I>>
		List<I> getInputAxioms(Class<H> converterType) {

		return parentConverter.getInputAxioms(converterType);
	}

	void logOutOfScopeAxiom(OWLAxiom axiom) {

		Logger logger = Logger.SINGLETON;

		logger.logOutOfScopeWarningLine("Axiom");
		logger.logLine("AXIOM: " + axiom);

		logger.logSeparatorLine();
	}
}
