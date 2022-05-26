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
public abstract class Relation extends Expression {

	private PropertyName property;
	private Value target;

	Relation(PropertyName property, Value target) {

		this.property = property;
		this.target = target;
	}

	PropertyName getProperty() {

		return property;
	}

	Value getTarget() {

		return target;
	}

	boolean subsumes(Relation r) {

		return r == this || (r.getClass() == getClass() && subsumesOtherOfType(r));
	}

	Collection<Relation> expandForSignature() {

		return Collections.singleton(this);
	}

	void registerDefinitionNames() {

		property.registerAsDefinitionName();
		target.registerDefinitionNames();
	}

	void collectNames(NameCollector collector) {

		collector.collectFor(property);

		target.collectNames(collector.forNextRank());
	}

	void render(PatternRenderer r) {

		r.addLine(property.getLabel());

		target.render(r.nextLevel());
	}

	private boolean subsumesOtherOfType(Relation r) {

		return property.subsumes(r.property) && target.subsumes(r.target);
	}
}
