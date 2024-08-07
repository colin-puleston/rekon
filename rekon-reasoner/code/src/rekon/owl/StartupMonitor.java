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

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class StartupMonitor {

	private List<RekonStartupListener> listeners = new ArrayList<RekonStartupListener>();

	private long processStartTime = 0;

	private class StartupClassifyListener implements OntologyClassifyListener {

		private int phase = 1;
		private int pass = 1;

		public void onPhaseStart() {

			logClassifyStartPoint("Phase " + phase++);

			pass = 1;
		}

		public void onPassStart(int candidateCount) {

			logClassifyStartPoint(
				"  Pass " + pass++
				+ " (" + candidateCount + " candidates)");
		}

		public void onCompletionStart() {

			logClassifyStartPoint("Building hierarchy");
		}

		private void logClassifyStartPoint(String text) {

			logMinorStartupPoint(text + "...");
		}
	}

	void addListener(RekonStartupListener listener) {

		listeners.add(listener);
	}

	void addListeners(Collection<RekonStartupListener> listeners) {

		this.listeners.addAll(listeners);
	}

	OntologyClassifyListener createClassifyListener() {

		return new StartupClassifyListener();
	}

	void onLoadingStart() {

		for (RekonStartupListener listener : listeners) {

			listener.onStartupStart();
		}

		onMajorProcessStart("Loading");
	}

	void onLoadingComplete() {

		onMajorProcessComplete("Loading", false);
	}

	void onClassificationStart() {

		onMajorProcessStart("Classifying");
	}

	void onClassificationComplete() {

		onMajorProcessComplete("Classification", true);

		for (RekonStartupListener listener : listeners) {

			listener.onStartupComplete();
		}
	}

	private void onMajorProcessStart(String text) {

		startProcessTimer();

		logMajorStartupStartPoint(text + "...");
	}

	private void onMajorProcessComplete(String text, boolean startupComplete) {

		String timeText = "(" + getProcessTime() + " seconds)";

		logMajorStartupEndPoint(text + " Complete " + timeText, startupComplete);
	}

	private void logMajorStartupStartPoint(String text) {

		ProgressLogger.SINGLETON.logMajorStartupStartPoint(text);
	}

	private void logMajorStartupEndPoint(String text, boolean startupComplete) {

		ProgressLogger.SINGLETON.logMajorStartupEndPoint(text, startupComplete);
	}

	private void logMinorStartupPoint(String text) {

		ProgressLogger.SINGLETON.logMinorStartupPoint(text);
	}

	private void startProcessTimer() {

		processStartTime = System.currentTimeMillis();
	}

	private long getProcessTime() {

		return (System.currentTimeMillis() - processStartTime) / 1000;
	}
}
