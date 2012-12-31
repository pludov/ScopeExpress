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
	protected JCheckBox targetExpositionCheckBox;
	protected JTextField targetExpositionText;
	protected JLabel lblIsoCible;
	protected JCheckBox targetIsoCheckBox;
	protected JTextField targetIsoText;
	protected JLabel lblMode;
	protected JComboBox channelModeCombo;
	protected JLabel lblNiveauDeNoir;
	protected JTextField zeroText;
	protected JLabel lblTransfert;
	protected JComboBox transfertComboBox;
	protected JCheckBox autoHistogramCheckBox;
	protected JLabel label;

	/**
	 * Create the panel.
	 */
	public ImageDisplayParameterPanelDesign() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {0, 31, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
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
		
		this.lblTransfert = new JLabel("Transfert :");
		GridBagConstraints gbc_lblTransfert = new GridBagConstraints();
		gbc_lblTransfert.anchor = GridBagConstraints.EAST;
		gbc_lblTransfert.insets = new Insets(0, 0, 5, 5);
		gbc_lblTransfert.gridx = 0;
		gbc_lblTransfert.gridy = 1;
		add(this.lblTransfert, gbc_lblTransfert);
		
		this.transfertComboBox = new JComboBox();
		GridBagConstraints gbc_transfertComboBox = new GridBagConstraints();
		gbc_transfertComboBox.gridwidth = 2;
		gbc_transfertComboBox.insets = new Insets(0, 0, 5, 0);
		gbc_transfertComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_transfertComboBox.gridx = 1;
		gbc_transfertComboBox.gridy = 1;
		add(this.transfertComboBox, gbc_transfertComboBox);
		
		this.autoHistogramCheckBox = new JCheckBox("");
		GridBagConstraints gbc_autoHistogramCheckBox = new GridBagConstraints();
		gbc_autoHistogramCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_autoHistogramCheckBox.gridx = 1;
		gbc_autoHistogramCheckBox.gridy = 2;
		add(this.autoHistogramCheckBox, gbc_autoHistogramCheckBox);
		
		this.label = new JLabel("R\u00E9glage automatique");
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.anchor = GridBagConstraints.WEST;
		gbc_label.insets = new Insets(0, 0, 5, 0);
		gbc_label.gridx = 2;
		gbc_label.gridy = 2;
		add(this.label, gbc_label);
		
		this.lblDureDexpoCible = new JLabel("Dur\u00E9e d'expo cible :");
		GridBagConstraints gbc_lblDureDexpoCible = new GridBagConstraints();
		gbc_lblDureDexpoCible.anchor = GridBagConstraints.EAST;
		gbc_lblDureDexpoCible.insets = new Insets(0, 0, 5, 5);
		gbc_lblDureDexpoCible.gridx = 0;
		gbc_lblDureDexpoCible.gridy = 3;
		add(this.lblDureDexpoCible, gbc_lblDureDexpoCible);
		
		this.targetExpositionCheckBox = new JCheckBox("");
		GridBagConstraints gbc_targetExpositionCheckBox = new GridBagConstraints();
		gbc_targetExpositionCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_targetExpositionCheckBox.gridx = 1;
		gbc_targetExpositionCheckBox.gridy = 3;
		add(this.targetExpositionCheckBox, gbc_targetExpositionCheckBox);
		
		this.targetExpositionText = new JTextField();
		GridBagConstraints gbc_targetExpositionText = new GridBagConstraints();
		gbc_targetExpositionText.insets = new Insets(0, 0, 5, 0);
		gbc_targetExpositionText.fill = GridBagConstraints.HORIZONTAL;
		gbc_targetExpositionText.gridx = 2;
		gbc_targetExpositionText.gridy = 3;
		add(this.targetExpositionText, gbc_targetExpositionText);
		this.targetExpositionText.setColumns(10);
		
		this.lblIsoCible = new JLabel("ISO cible :");
		GridBagConstraints gbc_lblIsoCible = new GridBagConstraints();
		gbc_lblIsoCible.anchor = GridBagConstraints.EAST;
		gbc_lblIsoCible.insets = new Insets(0, 0, 5, 5);
		gbc_lblIsoCible.gridx = 0;
		gbc_lblIsoCible.gridy = 4;
		add(this.lblIsoCible, gbc_lblIsoCible);
		
		this.targetIsoCheckBox = new JCheckBox("");
		GridBagConstraints gbc_targetIsoCheckBox = new GridBagConstraints();
		gbc_targetIsoCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_targetIsoCheckBox.gridx = 1;
		gbc_targetIsoCheckBox.gridy = 4;
		add(this.targetIsoCheckBox, gbc_targetIsoCheckBox);
		
		this.targetIsoText = new JTextField();
		GridBagConstraints gbc_targetIsoText = new GridBagConstraints();
		gbc_targetIsoText.insets = new Insets(0, 0, 5, 0);
		gbc_targetIsoText.fill = GridBagConstraints.HORIZONTAL;
		gbc_targetIsoText.gridx = 2;
		gbc_targetIsoText.gridy = 4;
		add(this.targetIsoText, gbc_targetIsoText);
		this.targetIsoText.setColumns(10);
		
		this.lblNiveauDeNoir = new JLabel("Niveau de noir :");
		GridBagConstraints gbc_lblNiveauDeNoir = new GridBagConstraints();
		gbc_lblNiveauDeNoir.anchor = GridBagConstraints.EAST;
		gbc_lblNiveauDeNoir.insets = new Insets(0, 0, 0, 5);
		gbc_lblNiveauDeNoir.gridx = 0;
		gbc_lblNiveauDeNoir.gridy = 5;
		add(this.lblNiveauDeNoir, gbc_lblNiveauDeNoir);
		
		this.zeroText = new JTextField();
		GridBagConstraints gbc_zeroText = new GridBagConstraints();
		gbc_zeroText.gridwidth = 2;
		gbc_zeroText.fill = GridBagConstraints.HORIZONTAL;
		gbc_zeroText.gridx = 1;
		gbc_zeroText.gridy = 5;
		add(this.zeroText, gbc_zeroText);
		this.zeroText.setColumns(10);

	}

}
