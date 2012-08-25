package fr.pludov.cadrage.correlation;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import javax.swing.JTextField;
import java.awt.Insets;
import javax.swing.JComboBox;

public class StarDetectionParameterPanelDesign extends JPanel {
	protected JTextField nbStarMaxText;
	protected JTextField backgroundEvaluationPctText;
	protected JTextField backgroundSquareText;
	protected JTextField absoluteAduSeuilText;
	protected JTextField starGrowIntensityRatioText;
	protected JTextField starMaxSizeText;
	protected JTextField binFactorCombo;
	
	/**
	 * Create the panel.
	 */
	public StarDetectionParameterPanelDesign() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JLabel lblNewLabel = new JLabel("Nombre d'\u00E9toiles maxi");
		lblNewLabel.setToolTipText("Limite le nombre d'\u00E9toiles d\u00E9t\u00E9ct\u00E9es pour une image");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);
		
		nbStarMaxText = new JTextField();
		lblNewLabel.setLabelFor(nbStarMaxText);
		GridBagConstraints gbc_nbStarMaxText = new GridBagConstraints();
		gbc_nbStarMaxText.insets = new Insets(0, 0, 5, 0);
		gbc_nbStarMaxText.fill = GridBagConstraints.HORIZONTAL;
		gbc_nbStarMaxText.gridx = 1;
		gbc_nbStarMaxText.gridy = 0;
		add(nbStarMaxText, gbc_nbStarMaxText);
		nbStarMaxText.setColumns(10);
		
		JLabel lblRductionbin = new JLabel("R\u00E9duction/Bin");
		GridBagConstraints gbc_lblRductionbin = new GridBagConstraints();
		gbc_lblRductionbin.anchor = GridBagConstraints.EAST;
		gbc_lblRductionbin.insets = new Insets(0, 0, 5, 5);
		gbc_lblRductionbin.gridx = 0;
		gbc_lblRductionbin.gridy = 1;
		add(lblRductionbin, gbc_lblRductionbin);
		
		binFactorCombo = new JTextField();
		lblRductionbin.setLabelFor(binFactorCombo);
		GridBagConstraints gbc_binFactorCombo = new GridBagConstraints();
		gbc_binFactorCombo.insets = new Insets(0, 0, 5, 0);
		gbc_binFactorCombo.fill = GridBagConstraints.HORIZONTAL;
		gbc_binFactorCombo.gridx = 1;
		gbc_binFactorCombo.gridy = 1;
		add(binFactorCombo, gbc_binFactorCombo);
		
		JLabel lblNewLabel_1 = new JLabel("Pourcentage du fond");
		lblNewLabel_1.setToolTipText("Pourcentage des pixels les plus sombre qui servent \u00E0 calculer le fond");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 2;
		add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		backgroundEvaluationPctText = new JTextField();
		lblNewLabel_1.setLabelFor(backgroundEvaluationPctText);
		backgroundEvaluationPctText.setColumns(10);
		GridBagConstraints gbc_backgroundEvaluationPctText = new GridBagConstraints();
		gbc_backgroundEvaluationPctText.insets = new Insets(0, 0, 5, 0);
		gbc_backgroundEvaluationPctText.fill = GridBagConstraints.HORIZONTAL;
		gbc_backgroundEvaluationPctText.gridx = 1;
		gbc_backgroundEvaluationPctText.gridy = 2;
		add(backgroundEvaluationPctText, gbc_backgroundEvaluationPctText);
		
		JLabel lblTailleDesBriques = new JLabel("Taille des briques");
		lblTailleDesBriques.setToolTipText("Taille en pixel (apr\u00E8s bin) des briques utilis\u00E9es pour l'\u00E9valuation du fond");
		GridBagConstraints gbc_lblTailleDesBriques = new GridBagConstraints();
		gbc_lblTailleDesBriques.anchor = GridBagConstraints.EAST;
		gbc_lblTailleDesBriques.insets = new Insets(0, 0, 5, 5);
		gbc_lblTailleDesBriques.gridx = 0;
		gbc_lblTailleDesBriques.gridy = 3;
		add(lblTailleDesBriques, gbc_lblTailleDesBriques);
		
		backgroundSquareText = new JTextField();
		backgroundSquareText.setColumns(10);
		GridBagConstraints gbc_backgroundSquareText = new GridBagConstraints();
		gbc_backgroundSquareText.insets = new Insets(0, 0, 5, 0);
		gbc_backgroundSquareText.fill = GridBagConstraints.HORIZONTAL;
		gbc_backgroundSquareText.gridx = 1;
		gbc_backgroundSquareText.gridy = 3;
		add(backgroundSquareText, gbc_backgroundSquareText);
		
		JLabel lblAduMinimum = new JLabel("ADU minimum");
		lblAduMinimum.setToolTipText("Valeur ADU minimale pour d\u00E9tecter une \u00E9toiles (au dessus du bruit, apr\u00E8s bin)");
		GridBagConstraints gbc_lblAduMinimum = new GridBagConstraints();
		gbc_lblAduMinimum.anchor = GridBagConstraints.EAST;
		gbc_lblAduMinimum.insets = new Insets(0, 0, 5, 5);
		gbc_lblAduMinimum.gridx = 0;
		gbc_lblAduMinimum.gridy = 4;
		add(lblAduMinimum, gbc_lblAduMinimum);
		
		absoluteAduSeuilText = new JTextField();
		lblAduMinimum.setLabelFor(absoluteAduSeuilText);
		absoluteAduSeuilText.setColumns(10);
		GridBagConstraints gbc_absoluteAduSeuilText = new GridBagConstraints();
		gbc_absoluteAduSeuilText.insets = new Insets(0, 0, 5, 0);
		gbc_absoluteAduSeuilText.fill = GridBagConstraints.HORIZONTAL;
		gbc_absoluteAduSeuilText.gridx = 1;
		gbc_absoluteAduSeuilText.gridy = 4;
		add(absoluteAduSeuilText, gbc_absoluteAduSeuilText);
		
		JLabel lblSeuilDeCroissance = new JLabel("Ratio du pic");
		lblSeuilDeCroissance.setToolTipText("Inclu dans le \"pic\" d'\u00E9toile les pixels adjacents sont superieurs \u00E0 ce pourcentage du pic");
		GridBagConstraints gbc_lblSeuilDeCroissance = new GridBagConstraints();
		gbc_lblSeuilDeCroissance.anchor = GridBagConstraints.EAST;
		gbc_lblSeuilDeCroissance.insets = new Insets(0, 0, 5, 5);
		gbc_lblSeuilDeCroissance.gridx = 0;
		gbc_lblSeuilDeCroissance.gridy = 5;
		add(lblSeuilDeCroissance, gbc_lblSeuilDeCroissance);
		
		starGrowIntensityRatioText = new JTextField();
		lblSeuilDeCroissance.setLabelFor(starGrowIntensityRatioText);
		starGrowIntensityRatioText.setColumns(10);
		GridBagConstraints gbc_starGrowIntensityRatioText = new GridBagConstraints();
		gbc_starGrowIntensityRatioText.insets = new Insets(0, 0, 5, 0);
		gbc_starGrowIntensityRatioText.fill = GridBagConstraints.HORIZONTAL;
		gbc_starGrowIntensityRatioText.gridx = 1;
		gbc_starGrowIntensityRatioText.gridy = 5;
		add(starGrowIntensityRatioText, gbc_starGrowIntensityRatioText);
		
		JLabel lblTailleMaxiDu = new JLabel("Taille maxi du pic");
		lblTailleMaxiDu.setToolTipText("Inclu dans une \u00E9toile les pixels qui sont superieurs \u00E0 ce pourcentage du pic");
		GridBagConstraints gbc_lblTailleMaxiDu = new GridBagConstraints();
		gbc_lblTailleMaxiDu.anchor = GridBagConstraints.EAST;
		gbc_lblTailleMaxiDu.insets = new Insets(0, 0, 0, 5);
		gbc_lblTailleMaxiDu.gridx = 0;
		gbc_lblTailleMaxiDu.gridy = 6;
		add(lblTailleMaxiDu, gbc_lblTailleMaxiDu);
		
		starMaxSizeText = new JTextField();
		lblTailleMaxiDu.setLabelFor(starMaxSizeText);
		starMaxSizeText.setColumns(10);
		GridBagConstraints gbc_starMaxSizeText = new GridBagConstraints();
		gbc_starMaxSizeText.fill = GridBagConstraints.HORIZONTAL;
		gbc_starMaxSizeText.gridx = 1;
		gbc_starMaxSizeText.gridy = 6;
		add(starMaxSizeText, gbc_starMaxSizeText);

	}

}
