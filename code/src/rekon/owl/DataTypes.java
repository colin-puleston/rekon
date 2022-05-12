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
import org.semanticweb.owlapi.vocab.*;

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class DataTypes {

	static private Set<TypeHandler> typeHandlers = new HashSet<TypeHandler>();

	static private abstract class TypeHandler {

		TypeHandler() {

			typeHandlers.add(this);
		}

		boolean handlesType(OWLDatatype type) {

			return getBuiltInTypes().contains(type.getBuiltInDatatype());
		}

		abstract List<OWL2Datatype> getBuiltInTypes();

		abstract DataValue getUnconstrained();

		abstract DataValue get(OWLDatatypeRestriction source);
	}

	static private class Booleans extends TypeHandler {

		List<OWL2Datatype> getBuiltInTypes() {

			return Arrays.asList(OWL2Datatype.XSD_BOOLEAN);
		}

		DataValue getUnconstrained() {

			return BooleanValue.BOOLEAN;
		}

		DataValue get(OWLDatatypeRestriction source) {

			return null;
		}
	}

	static DataValue toDataValueExpression(OWLLiteral value) {

		if (value.isBoolean()) {

			return BooleanValue.valueFor(value.parseBoolean());
		}

		if (value.isInteger()) {

			return new IntegerRange(value.parseInteger());
		}

		if (value.isFloat()) {

			return new FloatRange(value.parseFloat());
		}

		if (value.isDouble()) {

			return new DoubleRange(value.parseDouble());
		}

		return null;
	}

	private boolean dynamic;

	private abstract class NumberRangeHandler<N extends Number> extends TypeHandler {

		private Map<OWLDatatypeRestriction, NumberRange> cache
					= new HashMap<OWLDatatypeRestriction, NumberRange>();

		DataValue get(OWLDatatypeRestriction source) {

			return dynamic ? create(source) : getViaCache(source);
		}

		abstract NumberRange create(N min, N max);

		abstract N parseValue(String value);

		private DataValue getViaCache(OWLDatatypeRestriction source) {

			NumberRange r = cache.get(source);

			if (r == null) {

				r = create(source);

				cache.put(source, r);
			}

			return r;
		}

		private NumberRange create(OWLDatatypeRestriction source) {

			N min = getLimit(source, OWLFacet.MIN_INCLUSIVE);
			N max = getLimit(source, OWLFacet.MAX_INCLUSIVE);

			return create(min, max);
		}

		private N getLimit(OWLDatatypeRestriction source, OWLFacet facet) {

			for (OWLFacetRestriction fr : source.getFacetRestrictions()) {

				if (fr.getFacet() == facet) {

					return parseValue(fr.getFacetValue().getLiteral());
				}
			}

			return null;
		}
	}

	private class IntegerRanges extends NumberRangeHandler<Integer> {

		List<OWL2Datatype> getBuiltInTypes() {

			return Arrays.asList(OWL2Datatype.XSD_INTEGER, OWL2Datatype.XSD_INT);
		}

		NumberRange getUnconstrained() {

			return IntegerRange.UNCONSTRAINED;
		}

		NumberRange create(Integer min, Integer max) {

			return new IntegerRange(min, max);
		}

		Integer parseValue(String value) {

			return Integer.parseInt(value);
		}
	}

	private class FloatRanges extends NumberRangeHandler<Float> {

		List<OWL2Datatype> getBuiltInTypes() {

			return Arrays.asList(OWL2Datatype.XSD_FLOAT);
		}

		NumberRange getUnconstrained() {

			return FloatRange.UNCONSTRAINED;
		}

		NumberRange create(Float min, Float max) {

			return new FloatRange(min, max);
		}

		Float parseValue(String value) {

			return Float.parseFloat(value);
		}
	}

	private class DoubleRanges extends NumberRangeHandler<Double> {

		List<OWL2Datatype> getBuiltInTypes() {

			return Arrays.asList(OWL2Datatype.XSD_DOUBLE);
		}

		NumberRange getUnconstrained() {

			return DoubleRange.UNCONSTRAINED;
		}

		NumberRange create(Double min, Double max) {

			return new DoubleRange(min, max);
		}

		Double parseValue(String value) {

			return Double.parseDouble(value);
		}
	}

	DataTypes(boolean dynamic) {

		this.dynamic = dynamic;

		new Booleans();
		new IntegerRanges();
		new FloatRanges();
		new DoubleRanges();
	}

	DataValue get(OWLDataRange source) {

		if (source instanceof OWLDatatype) {

			return get(source.asOWLDatatype());
		}

		if (source instanceof OWLDatatypeRestriction) {

			return get((OWLDatatypeRestriction)source);
		}

		return null;
	}

	private DataValue get(OWLDatatypeRestriction source) {

		OWLDatatype type = source.getDatatype();
		TypeHandler handler = lookForHandler(type);

		return handler != null ? handler.get(source) : null;
	}

	private DataValue get(OWLDatatype source) {

		TypeHandler handler = lookForHandler(source);

		return handler != null ? handler.getUnconstrained() : null;
	}

	private TypeHandler lookForHandler(OWLDatatype type) {

		for (TypeHandler handler : typeHandlers) {

			if (handler.handlesType(type)) {

				return handler;
			}
		}

		return null;
	}
}
