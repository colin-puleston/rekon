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
class DataTypeConverter {

	static DataValue toDataValue(OWLLiteral value) {

		if (value.isBoolean()) {

			return BooleanValue.valueFor(value.parseBoolean());
		}

		Number n = toNumber(value);

		return n != null ? NumberValue.create(n) : null;
	}

	static private Number toNumber(OWLLiteral value) {

		if (value.isInteger()) {

			return value.parseInteger();
		}

		if (value.isFloat()) {

			return value.parseFloat();
		}

		if (value.isDouble()) {

			return value.parseDouble();
		}

		return null;
	}

	private boolean dynamic;
	private Set<TypeHandler> typeHandlers = new HashSet<TypeHandler>();

	private abstract class TypeHandler {

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

	private abstract class NumberRangeHandler<N extends Number> extends TypeHandler {

		private Map<OWLDatatypeRestriction, NumberValue> cache
					= new HashMap<OWLDatatypeRestriction, NumberValue>();

		NumberValue getUnconstrained() {

			return NumberValue.create(getAbsoluteMin(), getAbsoluteMax());
		}

		DataValue get(OWLDatatypeRestriction source) {

			return dynamic ? create(source) : getViaCache(source);
		}

		abstract N parseValue(String value);

		abstract N getAbsoluteMin();

		abstract N getAbsoluteMax();

		private DataValue getViaCache(OWLDatatypeRestriction source) {

			NumberValue r = cache.get(source);

			if (r == null) {

				r = create(source);

				cache.put(source, r);
			}

			return r;
		}

		private NumberValue create(OWLDatatypeRestriction source) {

			N min = getLimit(source, OWLFacet.MIN_INCLUSIVE, getAbsoluteMin());
			N max = getLimit(source, OWLFacet.MAX_INCLUSIVE, getAbsoluteMax());

			return NumberValue.create(min, max);
		}

		private N getLimit(
					OWLDatatypeRestriction source,
					OWLFacet facet,
					N defaultLimit) {

			for (OWLFacetRestriction fr : source.getFacetRestrictions()) {

				if (fr.getFacet() == facet) {

					return parseValue(fr.getFacetValue().getLiteral());
				}
			}

			return defaultLimit;
		}
	}

	private class IntegerRanges extends NumberRangeHandler<Integer> {

		List<OWL2Datatype> getBuiltInTypes() {

			return Arrays.asList(OWL2Datatype.XSD_INTEGER, OWL2Datatype.XSD_INT);
		}

		Integer parseValue(String value) {

			return Integer.parseInt(value);
		}

		Integer getAbsoluteMin() {

			return Integer.MIN_VALUE;
		}

		Integer getAbsoluteMax() {

			return Integer.MAX_VALUE;
		}
	}

	private class FloatRanges extends NumberRangeHandler<Float> {

		List<OWL2Datatype> getBuiltInTypes() {

			return Arrays.asList(OWL2Datatype.XSD_FLOAT);
		}

		Float parseValue(String value) {

			return Float.parseFloat(value);
		}

		Float getAbsoluteMin() {

			return Float.MIN_VALUE;
		}

		Float getAbsoluteMax() {

			return Float.MAX_VALUE;
		}
	}

	private class DoubleRanges extends NumberRangeHandler<Double> {

		List<OWL2Datatype> getBuiltInTypes() {

			return Arrays.asList(OWL2Datatype.XSD_DOUBLE);
		}

		Double parseValue(String value) {

			return Double.parseDouble(value);
		}

		Double getAbsoluteMin() {

			return Double.MIN_VALUE;
		}

		Double getAbsoluteMax() {

			return Double.MAX_VALUE;
		}
	}

	private class Booleans extends TypeHandler {

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

	DataTypeConverter(boolean dynamic) {

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

		if (source instanceof OWLDataUnionOf) {

			return get((OWLDataUnionOf)source);
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

	private DataValue get(OWLDataUnionOf source) {

		List<NumberValue> disjuncts = new ArrayList<NumberValue>();

		for (OWLDataRange r : source.getOperands()) {

			DataValue d = get(r);

			if (d instanceof NumberValue) {

				disjuncts.add((NumberValue)d);
			}
			else {

				return null;
			}
		}

		return NumberValue.create(disjuncts);
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
