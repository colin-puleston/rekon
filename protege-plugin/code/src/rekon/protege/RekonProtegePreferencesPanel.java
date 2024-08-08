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

import java.awt.BorderLayout;
import java.util.*;

import javax.swing.*;

import org.protege.editor.core.prefs.*;
import org.protege.editor.core.prefs.*;
import org.protege.editor.core.ui.preferences.*;
import org.protege.editor.owl.ui.preferences.*;

import rekon.owl.*;
import rekon.util.*;

/**
 * @author Colin Puleston
 */
public class RekonProtegePreferencesPanel extends OWLPreferencesPanel {

	static private final long serialVersionUID = -1;

	static private final String PANEL_TITLE = "Rekon Options";

	static private final String FUNCTIONALITY_GROUP_LABEL = "Functionality";
	static private final String PERFORMANCE_GROUP_LABEL = "Performance";
	static private final String LOGGING_GROUP_LABEL = "Reporting";

	static private final String MULTI_THREAD_LABEL = "multi-threading";
	static private final String NO_VALUE_SUBSTS_LABEL = "no-value substitutions";
	static private final String WARNING_LOGGING_LABEL = "warnings";
	static private final String PROGRESS_LOGGING_LABEL = "progress";

	static private final String PREFERENCES_STORE_KEY = "REKON-PREFERENCES";

	private RekonConfig config = RekonReasoner.CONFIG;

	private List<OptionSelector> optionSelectors = new ArrayList<OptionSelector>();
	private List<OptionGroup> optionGroups = new ArrayList<OptionGroup>();

	private class OptionSelector extends JCheckBox {

		static private final long serialVersionUID = -1;

		private String label;
		private Option option;

		OptionSelector(String label, Option option) {

			super(label, option.enabled());

			this.label = label;
			this.option = option;

			optionSelectors.add(this);
		}

		void initSelection(Preferences prefs) {

			option.setEnabled(prefs.getBoolean(label, option.enabled()));
		}

		void applySelection() {

			option.setEnabled(isSelected());
		}

		void saveSelection(Preferences prefs) {

			prefs.putBoolean(label, isSelected());
		}
	}

	private class OptionGroup {

		private String label;
		private List<OptionSelector> selectors = new ArrayList<OptionSelector>();

		OptionGroup(String label) {

			this.label = label;

			optionGroups.add(this);
		}

		void addOption(String label, Option option) {

			selectors.add(new OptionSelector(label, option));
		}

		void addGroupToLayout(PreferencesLayoutPanel layoutPanel) {

			if (firstGroup()) {

				layoutPanel.addSeparator();
			}

			layoutPanel.addGroup(label);

			for (OptionSelector sel : selectors) {

				layoutPanel.addGroupComponent(sel);
			}

			layoutPanel.addSeparator();
		}

		private boolean firstGroup() {

			return this == optionGroups.get(0);
		}
	}

	public RekonProtegePreferencesPanel() {

		OptionGroup funcGroup = new OptionGroup(FUNCTIONALITY_GROUP_LABEL);
		OptionGroup perfGroup = new OptionGroup(PERFORMANCE_GROUP_LABEL);
		OptionGroup loggGroup = new OptionGroup(LOGGING_GROUP_LABEL);

		funcGroup.addOption(NO_VALUE_SUBSTS_LABEL, config.noValueSubstitutions);
		perfGroup.addOption(MULTI_THREAD_LABEL, config.multiThread);
		loggGroup.addOption(WARNING_LOGGING_LABEL, config.warningLogger);
		loggGroup.addOption(PROGRESS_LOGGING_LABEL, config.progressLogger);
	}

	public void initialise() throws Exception {

		Preferences prefs = getStoredPreferences();

		for (OptionSelector sel : optionSelectors) {

			sel.initSelection(prefs);
		}

		populate();
	}

	public void applyChanges() {

		for (OptionSelector sel : optionSelectors) {

			sel.applySelection();
		}
	}

	public void dispose() throws Exception {

		Preferences prefs = getStoredPreferences();

		for (OptionSelector sel : optionSelectors) {

			sel.saveSelection(prefs);
		}
	}

	private void populate() {

		setLayout(new BorderLayout());

		PreferencesLayoutPanel layoutPanel = new PreferencesLayoutPanel();

		add(layoutPanel, BorderLayout.NORTH);

		for (OptionGroup group : optionGroups) {

			group.addGroupToLayout(layoutPanel);
		}
	}

	private Preferences getStoredPreferences() {

		return PreferencesManager
				.getInstance()
				.getApplicationPreferences(PREFERENCES_STORE_KEY);
	}
}
