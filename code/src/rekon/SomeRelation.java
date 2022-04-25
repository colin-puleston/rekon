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

package rekon;

import java.util.*;

/**
 * @author Colin Puleston
 */
class SomeRelation extends ObjectRelation {

	static private class Expander {

		private Set<Relation> allExpansions = new HashSet<Relation>();

		private List<Relation> passExpansions = new ArrayList<Relation>();
		private List<Relation> lastPassExpansions = new ArrayList<Relation>();

		private abstract class ExpansionCollector {

			void collectFromNested(Relation current) {

				for (Relation r : current.getTarget().getSignatureRelations()) {

					collectFrom(r);
				}
			}

			abstract void collectFrom(Relation current);
		}

		private class TransitivityBasedCollector extends ExpansionCollector {

			private PropertyName transProp;

			TransitivityBasedCollector(PropertyName transProp) {

				this.transProp = transProp;
			}

			void collectFrom(Relation current) {

				if (transProp.subsumes(current.getProperty())) {

					addExpansion(current);
				}
			}
		}

		private class ChainBasedCollector extends ExpansionCollector {

			private PropertyChain chain;
			private int tailsSubsIndex = 0;

			ChainBasedCollector(PropertyChain chain) {

				this.chain = chain;
			}

			void collectFrom(Relation current) {

				if (chain.hasTailSub(current.getProperty(), tailsSubsIndex)) {

					if (chain.lastTailSub(tailsSubsIndex)) {

						addExpansion(createLinkRelation(current));
					}
					else {

						tailsSubsIndex++;
						collectFromNested(current);
						tailsSubsIndex--;
					}
				}
			}

			private SomeRelation createLinkRelation(Relation endSub) {

				return chain.createLinkRelation((ObjectValue)endSub.getTarget());
			}
		}

		Expander(SomeRelation relation) {

			allExpansions.add(relation);
			lastPassExpansions.add(relation);

			List<ExpansionCollector> collectors = getExpansionCollectors(relation);

			if (!collectors.isEmpty()) {

				collectExpansions(collectors);
			}
		}

		Collection<Relation> expand() {

			return allExpansions;
		}

		private void collectExpansions(List<ExpansionCollector> collectors) {

			while (true) {

				for (ExpansionCollector collector : collectors) {

					for (Relation r : lastPassExpansions) {

						collector.collectFromNested(r);
					}
				}

				if (passExpansions.isEmpty()) {

					break;
				}

				lastPassExpansions = passExpansions;
				passExpansions = new ArrayList<Relation>();
			}
		}

		private List<ExpansionCollector> getExpansionCollectors(SomeRelation relation) {

			List<ExpansionCollector> collectors = new ArrayList<ExpansionCollector>();
			ObjectPropertyName transProp = relation.lookForMostGeneralTransitiveProperty();

			if (transProp != null) {

				collectors.add(new TransitivityBasedCollector(transProp));
			}

			for (PropertyChain chain : relation.getAllChains()) {

				collectors.add(new ChainBasedCollector(chain));
			}

			return collectors;
		}

		private void addExpansion(Relation relation) {

			if (allExpansions.add(relation)) {

				passExpansions.add(relation);
			}
		}
	}

	SomeRelation(ObjectPropertyName property, ObjectValue target) {

		super(property, target);
	}

	Collection<Relation> expandForSignature() {

		return new Expander(this).expand();
	}

	private ObjectPropertyName lookForMostGeneralTransitiveProperty() {

		return getObjectProperty().lookForMostGeneralTransitiveProperty();
	}

	private List<PropertyChain> getAllChains() {

		return getObjectProperty().getAllChains();
	}

	private ObjectPropertyName getObjectProperty() {

		return (ObjectPropertyName)getProperty();
	}
}
