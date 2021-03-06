/*******************************************************************************
 * Copyright (c) MOBAC developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.gui.actions;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import mobac.gui.MainGUI;
import mobac.program.model.AtlasOutputFormat;
import mobac.utilities.I18nUtils;

public class AtlasConvert implements ActionListener {

	public void actionPerformed(ActionEvent event) {
		MainGUI mg = MainGUI.getMainGUI();
		JPanel panel = new JPanel();
		BorderLayout layout = new BorderLayout();
		layout.setVgap(4);
		panel.setLayout(layout);

		JPanel formatPanel = new JPanel(new BorderLayout());

		formatPanel.setPreferredSize(new Dimension(250, 300));
		
		formatPanel.add(new JLabel(I18nUtils.localizedStringForKey("dlg_new_atlas_select_format_title")), BorderLayout.NORTH);
		JList<AtlasOutputFormat> atlasFormatList = new JList<>(AtlasOutputFormat.getFormatsAsVector());
		atlasFormatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scroller = new JScrollPane(atlasFormatList);
		scroller.setPreferredSize(new Dimension(100, 200));
		formatPanel.add(scroller, BorderLayout.CENTER);

		panel.add(formatPanel, BorderLayout.CENTER);
		AtlasOutputFormat currentAOF = null;
		try {
			currentAOF = mg.getAtlas().getOutputFormat();
		} catch (Exception e) {
		}
		if (currentAOF != null)
			atlasFormatList.setSelectedValue(currentAOF, true);
		else
			atlasFormatList.setSelectedIndex(1);
		int result = JOptionPane.showConfirmDialog(MainGUI.getMainGUI(), panel, I18nUtils.localizedStringForKey("msg_convert_atlas_format"),
				JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION)
			return;

		AtlasOutputFormat format = (AtlasOutputFormat) atlasFormatList.getSelectedValue();
		mg.jAtlasTree.convertAtlas(format);
		mg.getParametersPanel().atlasFormatChanged(format);
	}
}
