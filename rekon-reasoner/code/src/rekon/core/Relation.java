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
public abstract class Relation extends PatternComponent {

	static void collectNamesFromNextRankRelations(NameCollector collector, Collection<Relation> rels) {

		if (!rels.isEmpty()) {

			collector = collector.forNextRank();

			for (Relation r : rels) {

				r.collectNames(collector);
			}
		}
	}

	private PropertyX property;
	private Value target;

	Relation(PropertyX property, Value target) {

		this.property = property;
		this.target = target;
	}

	boolean chainExpandable() {

		return false;
	}

	PropertyX getProperty() {

		return property;
	}

	Value getTarget() {

		return target;
	}

	boolean subsumes(Relation r) {

		return r == this || (r.getClass() == getClass() && subsumesOtherOfType(r));
	}

	boolean propertySubsumption(PropertyX otherProperty) {

		return property.subsumes(otherProperty);
	}

	boolean inversePropertyMatch() {

		return false;
	}

	void collectNames(NameCollector collector) {

		collectNamesForProperty(collector);

		target.collectNames(collector.forNextRank());
	}

	void collectNamesForProperty(NameCollector collector) {

		collector.collectName(property);
	}

	Names getReferencedNodes() {

		return Names.NO_NAMES;
	}

	void registerAsDefinitionRefed() {

		property.registerAsDefinitionRefed(MatchRole.RELATION);
	}

	void render(PatternRenderer r) {

		r.addLine(property.getLabel() + " (" + renderRelationType() + ")");

		target.render(r.nextLevel());
	}

	String renderRelationType() {

		return "some";
	}

	private boolean subsumesOtherOfType(Relation r) {

		return propertySubsumption(r.getProperty()) && target.subsumes(r.target);
	}
}
