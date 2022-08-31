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
abstract class IndexMap<V> {

	static private final int BASE = 2;

	static private abstract class DigitPosition {

		DigitPosition checkExtendDigits(List<Integer> digits) {

			return digits.size() > position() + 1 ? new HigherPosition(this) : null;
		}

		abstract boolean add(Object value, List<Integer> digits);

		abstract boolean find(List<Integer> digits, boolean remove);

		abstract int addAll(DigitPosition template);

		abstract int removeAll(DigitPosition template);

		abstract int retainAll(DigitPosition template);

		abstract <V>void collectValues(List<V> collected, Class<V> type);

		abstract DigitPosition copy();

		abstract int position();

		abstract DigitPosition findPosition(int find);

		DigitPosition insertPositions(int required) {

			HigherPosition insert = new HigherPosition(this);

			return required == position() + 1 ? insert : insert.insertPositions(required);
		}

		int positionDigit(List<Integer> digits) {

			int i = digits.size() - position() - 1;

			return i < 0 ? 0 : digits.get(i);
		}

		boolean isEmpty() {

			for (int i = 0 ; i < BASE ; i++) {

				if (getValue(i) != null) {

					return false;
				}
			}

			return true;
		}

		abstract Object getValue(int i);
	}

	static private class Units extends DigitPosition {

		private Object[] values;

		Units() {

			values = new Object[BASE];
		}

		boolean add(Object value, List<Integer> digits) {

			int d = positionDigit(digits);

			if (values[d] != null) {

				return false;
			}

			values[d] = value;

			return true;
		}

		boolean find(List<Integer> digits, boolean remove) {

			int d = positionDigit(digits);

			if (values[d] == null) {

				return false;
			}

			if (remove) {

				values[d] = null;
			}

			return true;
		}

		int addAll(DigitPosition template) {

			Units t = (Units)template;
			int added = 0;

			for (int i = 0 ; i < values.length ; i++) {

				if (values[i] == null && t.values[i] != null) {

					values[i] = t.values[i];

					added++;
				}
			}

			return added;
		}

		int removeAll(DigitPosition template) {

			Units t = (Units)template;
			int removed = 0;

			for (int i = 0 ; i < values.length ; i++) {

				if (values[i] != null && t.values[i] != null) {

					values[i] = null;

					removed++;
				}
			}

			return removed;
		}

		int retainAll(DigitPosition template) {

			Units t = (Units)template;
			int retained = 0;

			for (int i = 0 ; i < values.length ; i++) {

				if (values[i] != null) {

					if (t.values[i] == null) {

						values[i] = null;
					}
					else {

						retained++;
					}
				}
			}

			return retained;
		}

		<V>void collectValues(List<V> collected, Class<V> type) {

			for (Object v : values) {

				if (v != null) {

					collected.add(type.cast(v));
				}
			}
		}

		DigitPosition copy() {

			return new Units(this);
		}

		int position() {

			return 0;
		}

		DigitPosition findPosition(int find) {

			if (find != 0) {

				throw new Error("Unexpected position: " + find);
			}

			return this;
		}

		Object getValue(int i) {

			return values[i];
		}

		private Units(Units template) {

			values = Arrays.copyOf(template.values, BASE);
		}
	}

	static private class HigherPosition extends DigitPosition {

		private int position;
		private DigitPosition[] nextRights = new DigitPosition[BASE];

		HigherPosition(int position) {

			this.position = position;
		}

		HigherPosition(DigitPosition firstNextRight) {

			this(firstNextRight.position() + 1);

			nextRights[0] = firstNextRight;
		}

		boolean add(Object value, List<Integer> digits) {

			int d = positionDigit(digits);

			if (nextRights[d] == null) {

				nextRights[d] = createNextRight();
			}

			return nextRights[d].add(value, digits);
		}

		boolean find(List<Integer> digits, boolean remove) {

			int d = positionDigit(digits);

			return nextRights[d] != null && nextRights[d].find(digits, remove);
		}

		int addAll(DigitPosition template) {

			HigherPosition t = (HigherPosition)template;
			int added = 0;

			for (int i = 0 ; i < nextRights.length ; i++) {

				if (t.nextRights[i] != null) {

					if (nextRights[i] == null) {

						nextRights[i] = createNextRight();
					}

					added += nextRights[i].addAll(t.nextRights[i]);
				}
			}

			return added;
		}

		int removeAll(DigitPosition template) {

			HigherPosition t = (HigherPosition)template;
			int removed = 0;

			for (int i = 0 ; i < nextRights.length ; i++) {

				if (nextRights[i] != null && t.nextRights[i] != null) {

					int r = nextRights[i].removeAll(t.nextRights[i]);

					if (r != 0) {

						if (nextRights[i].isEmpty()) {

							nextRights[i] = null;
						}

						removed += r;
					}
				}
			}

			return removed;
		}

		int retainAll(DigitPosition template) {

			HigherPosition t = (HigherPosition)template;
			int retained = 0;

			for (int i = 0 ; i < nextRights.length ; i++) {

				if (nextRights[i] != null) {

					if (t.nextRights[i] != null) {

						int r = nextRights[i].retainAll(t.nextRights[i]);

						if (r != 0) {

							retained += r;
						}
						else {

							nextRights[i] = null;
						}
					}
					else {

						nextRights[i] = null;
					}
				}
			}

			return retained;
		}

		<V>void collectValues(List<V> collected, Class<V> type) {

			for (DigitPosition n : nextRights) {

				if (n != null) {

					n.collectValues(collected, type);
				}
			}
		}

		DigitPosition copy() {

			HigherPosition c = new HigherPosition(position);
			int i = 0;

			for (DigitPosition n : nextRights) {

				if (n != null) {

					c.nextRights[i] = n.copy();
				}

				i++;
			}

			return c;
		}

		int position() {

			return position;
		}

		DigitPosition findPosition(int find) {

			if (find > position) {

				throw new Error("Unexpected position: " + find);
			}

			if (find == position) {

				return this;
			}

			return nextRights[0] != null ? nextRights[0].findPosition(find) : null;
		}

		Object getValue(int i) {

			return nextRights[i];
		}

		private DigitPosition createNextRight() {

			return position == 1 ? new Units() : new HigherPosition(position - 1);
		}
	}

	private int size = 0;
	private DigitPosition rootPosition;

	private abstract class AlignmentProcess {

		DigitPosition targetStart;
		DigitPosition templateStart;

		AlignmentProcess(IndexMap<?> template) {

			targetStart = rootPosition;
			templateStart = template.rootPosition;

			alignStarts();

			if (targetStart != null) {

				alignContent();
			}

			rootPosition = newRootPosition();
			size = newSize();
		}

		abstract void alignForLesserTemplateStartPos();

		abstract void alignForGreaterTemplateStartPos();

		abstract void alignContent();

		abstract DigitPosition newRootPosition();

		abstract int newSize();

		private void alignStarts() {

			if (targetStart.position() > templateStart.position()) {

				alignForLesserTemplateStartPos();
			}
			else if (templateStart.position() > targetStart.position()) {

				alignForGreaterTemplateStartPos();
			}
		}
	}

	private class AddAllProcess extends AlignmentProcess {

		private int added;

		AddAllProcess(IndexMap<?> template) {

			super(template);
		}

		void alignForLesserTemplateStartPos() {

			targetStart = targetStart.findPosition(templateStart.position());
		}

		void alignForGreaterTemplateStartPos() {

			targetStart = targetStart.insertPositions(templateStart.position());
		}

		void alignContent() {

			added = targetStart.addAll(templateStart);
		}

		DigitPosition newRootPosition() {

			return targetStart.position() > rootPosition.position() ? targetStart : rootPosition;
		}

		int newSize() {

			return size + added;
		}
	}

	private abstract class RemovalsProcess extends AlignmentProcess {

		RemovalsProcess(IndexMap<?> template) {

			super(template);
		}

		void alignForLesserTemplateStartPos() {

			targetStart = targetStart.findPosition(templateStart.position());
		}

		void alignForGreaterTemplateStartPos() {

			templateStart = templateStart.findPosition(targetStart.position());

			if (templateStart == null) {

				targetStart = null;
			}
		}
	}

	private class RemoveAllProcess extends RemovalsProcess {

		private int removed = 0;

		RemoveAllProcess(IndexMap<?> template) {

			super(template);
		}

		void alignContent() {

			removed = targetStart.removeAll(templateStart);
		}

		DigitPosition newRootPosition() {

			return rootPosition;
		}

		int newSize() {

			return size - removed;
		}
	}

	private class RetainAllProcess extends RemovalsProcess {

		private int retained = 0;

		RetainAllProcess(IndexMap<?> template) {

			super(template);
		}

		void alignContent() {

			retained = targetStart.retainAll(templateStart);
		}

		DigitPosition newRootPosition() {

			return targetStart != null ? targetStart : new Units();
		}

		int newSize() {

			return retained;
		}
	}

	IndexMap() {

		rootPosition = new Units();
	}

	IndexMap(V value) {

		this();

		add(value);
	}

	IndexMap(Collection<? extends V> values) {

		this();

		addAll(values);
	}

	IndexMap(IndexMap<V> template) {

		rootPosition = template.rootPosition.copy();
		size = template.size;
	}

	boolean add(V value) {

		List<Integer> digits = toDigits(value);

		while (true) {

			DigitPosition newRootPos = rootPosition.checkExtendDigits(digits);

			if (newRootPos == null) {

				break;
			}

			rootPosition = newRootPos;
		}

		if (rootPosition.add(value, digits)) {

			size++;

			return true;
		}

		return false;
	}

	void addAll(Collection<? extends V> values) {

		for (V v : values) {

			add(v);
		}
	}

	void addAll(IndexMap<V> template) {

		new AddAllProcess(template);
	}

	boolean remove(V value) {

		if (find(value, true)) {

			size--;

			return true;
		}

		return false;
	}

	void removeAll(Collection<? extends V> values) {

		for (V v : values) {

			remove(v);
		}
	}

	void removeAll(IndexMap<V> template) {

		new RemoveAllProcess(template);
	}

	void retainAll(IndexMap<V> template) {

		new RetainAllProcess(template);
	}

	void clear() {

		rootPosition = new Units();
		size = 0;
	}

	int size() {

		return size;
	}

	boolean contains(V value) {

		return find(value, false);
	}

	Collection<V> getValues() {

		List<V> values = new ArrayList<V>();

		rootPosition.collectValues(values, getValueType());

		return values;
	}

	abstract Class<V> getValueType();

	abstract int getValueIndex(V value);

	private boolean find(V value, boolean remove) {

		List<Integer> digits = toDigits(value);

		if (rootPosition.position() < digits.size() - 1) {

			return false;
		}

		return rootPosition.find(digits, remove);
	}

	private List<Integer> toDigits(V value) {

		List<Integer> digits = new ArrayList<Integer>();
		int i1 = getValueIndex(value);

		do {

			int i2 = i1 / BASE;

			digits.add(0, i1 - (i2 * BASE));

			i1 = i2;
		}
		while (i1 > 0);

		return digits;
	}
}
