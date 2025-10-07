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

package rekon.core;

import java.util.*;

/**
 * @author Colin Puleston
 */
public abstract class IndividualNode extends NodeX {

	private abstract class ValueRetriever<V> {

		private PropertyX prop;

		ValueRetriever(PropertyX prop) {

			this.prop = prop;
		}

		List<V> retrieve() {

			PatternMatcher p = getProfileMatcher();

			return p != null ? retrieve(p) : Collections.emptyList();
		}

		abstract V toValueOrNull(Value target);

		private List<V> retrieve(PatternMatcher profile) {

			List<V> values = new ArrayList<V>();

			for (Relation r : profile.getPattern().getDirectRelations()) {

				if (r.getProperty() == prop) {

					V v = toValueOrNull(r.getTarget());

					if (v != null) {

						values.add(v);
					}
				}
			}

			return values;
		}
	}

	private class IndividualValueRetriever extends ValueRetriever<IndividualNode> {

		IndividualValueRetriever(PropertyX prop) {

			super(prop);
		}

		IndividualNode toValueOrNull(Value target) {

			NodeValue nv = target.asNodeValue();

			if (nv != null && nv.singleValueNode()) {

				NodeX vn = nv.getSingleValueNode();

				if (vn instanceof IndividualNode) {

					return (IndividualNode)vn;
				}
			}

			return null;
		}
	}

	private class DataValueRetriever extends ValueRetriever<DataValue> {

		DataValueRetriever(PropertyX prop) {

			super(prop);
		}

		DataValue toValueOrNull(Value target) {

			return target instanceof DataValue ? (DataValue)target : null;
		}
	}

	public Names getIndividualValues(PropertyX prop) {

		return new NameList(new IndividualValueRetriever(prop).retrieve());
	}

	public List<DataValue> getDataValues(PropertyX prop) {

		return null;
	}
}
