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

package rekon.util;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @author Colin Puleston
 */
class OptionConfigFile {

	static private final String FILENAME = "rekon.options";

	static Properties checkLoad() {

		String f = lookForFile();

		return f != null ? loadProperties(f) : null;
	}

	static private String lookForFile() {

		URL url = getClassLoader().getResource(FILENAME);

		return url != null ? url.getFile() : null;
	}

	static private Properties loadProperties(String file) {

		Properties p = new Properties();

		try {

			BufferedReader r = new BufferedReader(new FileReader(file));

			p.load(r);
			r.close();
		}
		catch (IOException e) {

			throw new RuntimeException(e);
		}

		return p;
	}

	static private ClassLoader getClassLoader() {

		return Thread.currentThread().getContextClassLoader();
	}
}
