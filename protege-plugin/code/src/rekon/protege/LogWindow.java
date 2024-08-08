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

package rekon.protege;

import java.awt.*;
import javax.swing.*;

/**
 * @author Colin Puleston
 */
class LogWindow extends JFrame {

	static private final long serialVersionUID = -1;

	static private final String TITLE = "Rekon Reasoner Log";

	static private final int WIDTH = 700;
	static private final int HEIGHT = 500;

	private JTextArea textArea = new JTextArea();

	LogWindow() {

		super(TITLE);

		textArea.setEditable(false);
		textArea.setBackground(Color.WHITE);

		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		getContentPane().add(new JScrollPane(textArea));

		pack();
		centreOnScreen();
		setVisible(true);
	}

	void write(String text) {

		textArea.append(text);
		textArea.validate();

		scrollToBottom();
	}

	private void scrollToBottom() {

		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	private void centreOnScreen() {

		Dimension d = getSize();
		Dimension sd = Toolkit.getDefaultToolkit().getScreenSize();

		double w = (sd.getWidth() - d.getWidth()) / 2;
		double h = (sd.getHeight() - d.getHeight()) / 2;

		setLocation((int)w, (int)h);
	}
}
