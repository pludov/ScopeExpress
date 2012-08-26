package fr.pludov.cadrage.ui.settings;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import javax.swing.JCheckBox;
import java.awt.Insets;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JComboBox;

public class ImageDisplayParameterPanelDesign extends JPanel {
	protected JLabel lblDureDexpoCible;
	protected JCheckBox checkBox;
	protected JTextField targetExpositionText;
	protected JLabel lblIsoCible;
	protected JCheckBox checkBox_1;
	protected JTextField targetIsoText;
	protected JLabel lblMode;
	protected JComboBox channelModeCombo;
	protected JLabel lblNiveauDeNoir;
	protected JTextField zeroText;

	/**
	 * Create the panel.
	 */
	public ImageDisplayParameterPanelDesign() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 44, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		this.lblMode = new JLabel("Mode :");
		GridBagConstraints gbc_lblMode = new GridBagConstraints();
		gbc_lblMode.anchor = GridBagConstraints.EAST;
		gbc_lblMode.insets = new Insets(0, 0, 5, 5);
		gbc_lblMode.gridx = 0;
		gbc_lblMode.gridy = 0;
		add(this.lblMode, gbc_lblMode);
		
		this.channelModeCombo = new JComboBox();
		GridBagConstraints gbc_channelModeCombo = new GridBagConstraints();
		gbc_channelModeCombo.gridwidth = 2;
		gbc_channelModeCombo.insets = new Insets(0, 0, 5, 0);
		gbc_channelModeCombo.fill = GridBagConstraints.HORIZONTAL;
		gbc_channelModeCombo.gridx = 1;
		gbc_channelModeCombo.gridy = 0;
		add(this.channelModeCombo, gbc_channelModeCombo);
		
		this.lblDureDexpoCible = new JLabel("Dur\u00E9e d'expo cible :");
		GridBagConstraints gbc_lblDureDexpoCible = new GridBagConstraints();
		gbc_lblDureDexpoCible.anchor = GridBagConstraints.EAST;
		gbc_lblDureDexpoCible.insets = new Insets(0, 0, 5, 5);
		gbc_lblDureDexpoCible.gridx = 0;
		gbc_lblDureDexpoCible.gridy = 1;
		add(this.lblDureDexpoCible, gbc_lblDureDexpoCible);
		
		this.checkBox = new JCheckBox("");
		GridBagConstraints gbc_checkBox = new GridBagConstraints();
		gbc_checkBox.insets = new Insets(0, 0, 5, 5);
		gbc_checkBox.gridx = 1;
		gbc_checkBox.gridy = 1;
		add(this.checkBox, gbc_checkBox);
		
		this.targetExpositionText = new JTextField();
		GridBagConstraints gbc_targetExpositionText = new GridBagConstraints();
		gbc_targetExpositionText.insets = new Insets(0, 0, 5, 0);
		gbc_targetExpositionText.fill = GridBagConstraints.HORIZONTAL;
		gbc_targetExpositionText.gridx = 2;
		gbc_targetExpositionText.gridy = 1;
		add(this.targetExpositionText, gbc_targetExpositionText);
		this.targetExpositionText.setColumns(10);
		
		this.lblIsoCible = new JLabel("ISO cible :");
		GridBagConstraints gbc_lblIsoCible = new GridBagConstraints();
		gbc_lblIsoCible.anchor = GridBagConstraints.EAST;
		gbc_lblIsoCible.insets = new Insets(0, 0, 5, 5);
		gbc_lblIsoCible.gridx = 0;
		gbc_lblIsoCible.gridy = 2;
		add(this.lblIsoCible, gbc_lblIsoCible);
		
		this.checkBox_1 = new JCheckBox("");
		GridBagConstraints gbc_checkBox_1 = new GridBagConstraints();
		gbc_checkBox_1.insets = new Insets(0, 0, 5, 5);
		gbc_checkBox_1.gridx = 1;
		gbc_checkBox_1.gridy = 2;
		add(this.checkBox_1, gbc_checkBox_1);
		
		this.targetIsoText = new JTextField();
		GridBagConstraints gbc_targetIsoText = new GridBagConstraints();
		gbc_targetIsoText.insets = new Insets(0, 0, 5, 0);
		gbc_targetIsoText.fill = GridBagConstraints.HORIZONTAL;
		gbc_targetIsoText.gridx = 2;
		gbc_targetIsoText.gridy = 2;
		add(this.targetIsoText, gbc_targetIsoText);
		this.targetIsoText.setColumns(10);
		
		this.lblNiveauDeNoir = new JLabel("Niveau de noir :");
		GridBagConstraints gbc_lblNiveauDeNoir = new GridBagConstraints();
		gbc_lblNiveauDeNoir.anchor = GridBagConstraints.EAST;
		gbc_lblNiveauDeNoir.insets = new Insets(0, 0, 0, 5);
		gbc_lblNiveauDeNoir.gridx = 0;
		gbc_lblNiveauDeNoir.gridy = 3;
		add(this.lblNiveauDeNoir, gbc_lblNiveauDeNoir);
		
		this.zeroText = new JTextField();
		GridBagConstraints gbc_zeroText = new GridBagConstraints();
		gbc_zeroText.gridwidth = 2;
		gbc_zeroText.insets = new Insets(0, 0, 0, 5);
		gbc_zeroText.fill = GridBagConstraints.HORIZONTAL;
		gbc_zeroText.gridx = 1;
		gbc_zeroText.gridy = 3;
		add(this.zeroText, gbc_zeroText);
		this.zeroText.setColumns(10);

	}

}
