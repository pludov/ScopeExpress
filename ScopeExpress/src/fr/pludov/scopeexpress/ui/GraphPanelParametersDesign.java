package fr.pludov.scopeexpress.ui;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;

public class GraphPanelParametersDesign extends JPanel {
	protected JCheckBox chckbxCacherLesSatures;
	protected JLabel lblEnergieMini;
	protected JComboBox energyLimitTypeSel;
	protected JTextField energyMinTextField;
	protected JLabel lblEnergieMaxi;
	protected JTextField energyMaxTextField;
	protected JTextField missingMaxCountTextField;
	protected JLabel lblNombresDabsencesPour;
	protected JLabel lblRangeHorizontal;
	protected JTextField minRangeHTextField;
	protected JTextField minRangeVTextField;
	protected JTextField maxRangeHTextField;
	protected JTextField maxRangeVTextField;
	protected JLabel lblFiltrageParnergie;
	protected JLabel lblHor;
	protected JLabel lblVer;
	protected JLabel energyMinErrorLbl;
	protected JLabel energyMaxErrorLbl;
	protected JLabel missingMaxCountErrorLbl;
	protected JLabel rangeHErrorLbl;
	protected JLabel rangeVErrorLbl;

	/**
	 * Create the panel.
	 */
	public GraphPanelParametersDesign() {
		setLayout(new MigLayout("", "[][grow][grow][4px:4px:4px]", "[][][][][][][][][][]"));
		
		this.chckbxCacherLesSatures = new JCheckBox("Cacher les satur\u00E9es");
		add(this.chckbxCacherLesSatures, "cell 0 0 3 1");
		
		this.lblFiltrageParnergie = new JLabel("Filtrage par \u00E9nergie:");
		this.lblFiltrageParnergie.setToolTipText("Appliqu\u00E9 sur la derni\u00E8re image");
		add(this.lblFiltrageParnergie, "cell 0 1 3 1");
		
		this.energyLimitTypeSel = new JComboBox();
		add(this.energyLimitTypeSel, "cell 1 2 2 1,growx");
		
		this.lblEnergieMini = new JLabel("Min:");
		add(this.lblEnergieMini, "cell 1 3,alignx trailing");
		
		this.energyMinTextField = new JTextField();
		add(this.energyMinTextField, "flowx,cell 2 3,growx");
		this.energyMinTextField.setColumns(10);
		
		this.energyMinErrorLbl = new JLabel("!");
		this.energyMinErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 11));
		this.energyMinErrorLbl.setForeground(Color.RED);
		add(this.energyMinErrorLbl, "cell 3 3");
		
		this.lblEnergieMaxi = new JLabel("Max:");
		add(this.lblEnergieMaxi, "cell 1 4,alignx trailing");
		
		this.energyMaxTextField = new JTextField();
		add(this.energyMaxTextField, "cell 2 4,growx");
		this.energyMaxTextField.setColumns(10);
		
		this.energyMaxErrorLbl = new JLabel("!");
		this.energyMaxErrorLbl.setForeground(Color.RED);
		this.energyMaxErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 11));
		add(this.energyMaxErrorLbl, "cell 3 4");
		
		this.lblNombresDabsencesPour = new JLabel("Nombres d'absences pour exclure:");
		add(this.lblNombresDabsencesPour, "cell 0 5 3 1,alignx left");
		
		this.missingMaxCountTextField = new JTextField();
		this.missingMaxCountTextField.setToolTipText("Retirer les \u00E9toiles qui manquent dans au moins ce nombre d'images");
		add(this.missingMaxCountTextField, "cell 1 6 2 1,growx");
		this.missingMaxCountTextField.setColumns(10);
		
		this.missingMaxCountErrorLbl = new JLabel("!");
		this.missingMaxCountErrorLbl.setForeground(Color.RED);
		this.missingMaxCountErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 11));
		add(this.missingMaxCountErrorLbl, "cell 3 6");
		
		this.lblRangeHorizontal = new JLabel("Filtrer sur le centre (%):");
		this.lblRangeHorizontal.setToolTipText("Ne retient que les \u00E9toiles dans un rectangle, sur la derni\u00E8re image ");
		add(this.lblRangeHorizontal, "cell 0 7 3 1,alignx left");
		
		this.lblHor = new JLabel("Hor:");
		this.lblHor.setToolTipText("Plage (min/max) de l'image en horizontal, en pourcent");
		add(this.lblHor, "cell 0 8,alignx trailing");
		
		this.minRangeHTextField = new JTextField();
		add(this.minRangeHTextField, "cell 1 8,growx");
		this.minRangeHTextField.setColumns(10);
		
		this.maxRangeHTextField = new JTextField();
		this.maxRangeHTextField.setColumns(10);
		add(this.maxRangeHTextField, "cell 2 8,growx");
		
		this.rangeHErrorLbl = new JLabel("!");
		this.rangeHErrorLbl.setForeground(Color.RED);
		this.rangeHErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 11));
		add(this.rangeHErrorLbl, "cell 3 8");
		
		this.lblVer = new JLabel("Ver:");
		this.lblVer.setToolTipText("Plage (min/max) de l'image en vertical, en pourcent");
		add(this.lblVer, "cell 0 9,alignx trailing");
		
		this.minRangeVTextField = new JTextField();
		this.minRangeVTextField.setColumns(10);
		add(this.minRangeVTextField, "cell 1 9,growx");
		
		this.maxRangeVTextField = new JTextField();
		this.maxRangeVTextField.setColumns(10);
		add(this.maxRangeVTextField, "cell 2 9,growx");
		
		this.rangeVErrorLbl = new JLabel("!");
		this.rangeVErrorLbl.setForeground(Color.RED);
		this.rangeVErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 11));
		add(this.rangeVErrorLbl, "cell 3 9");

	}

}
