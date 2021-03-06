package fr.pludov.scopeexpress.ui;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import java.awt.Font;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JComboBox;
import javax.swing.JButton;
import java.awt.Insets;

public class GuideStarFinderDesign extends JPanel {
	protected JPanel panel;
	protected JLabel lblRa;
	protected JTextField raTextField;
	protected JLabel lblDec;
	protected JTextField decTextField;
	protected JPanel panel_1;
	protected JCheckBox chckbxEstTop;
	protected JPanel panel_2;
	protected JLabel lblDistanceMiniAu;
	protected JLabel lblDistanceMaxiAu;
	protected JTextField textFieldMinDist;
	protected JTextField textFieldMaxDist;
	protected JLabel lblAngleMini;
	protected JTextField textFieldMinAngle;
	protected JLabel lblAngleMaxi;
	protected JTextField textFieldMaxAngle;
	protected JPanel resultPanel;
	protected JLabel raTextFieldError;
	protected JLabel decTextFieldError;
	protected JLabel textFieldMinAngleError;
	protected JLabel textFieldMaxAngleError;
	protected JLabel textFieldMinDistError;
	protected JLabel textFieldMaxDistError;
	protected JPanel panel_3;
	protected JComboBox setupSelector;
	protected JButton btnSetupSave;
	protected JButton btnSetupDel;
	protected JButton btnSetupAdd;
	protected JPanel panel_4;
	protected JLabel lblMagLimit;
	protected JTextField textFieldMagLimite;
	protected JLabel textFieldMagLimiteError;

	/**
	 * Create the panel.
	 */
	public GuideStarFinderDesign() {
		setLayout(new MigLayout("", "[][grow]", "[][][][][][grow]"));
		
		this.panel = new JPanel();
		this.panel.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Centre", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		add(this.panel, "cell 0 0,grow");
		this.panel.setLayout(new MigLayout("", "[][grow]", "[][][]"));
		
		this.lblRa = new JLabel("RA:");
		this.panel.add(this.lblRa, "cell 0 0,alignx trailing");
		
		this.raTextField = new JTextField();
		this.panel.add(this.raTextField, "flowx,cell 1 0,growx");
		this.raTextField.setColumns(10);
		
		this.lblDec = new JLabel("Dec:");
		this.panel.add(this.lblDec, "cell 0 1,alignx trailing");
		
		this.decTextField = new JTextField();
		this.panel.add(this.decTextField, "flowx,cell 1 1,growx");
		this.decTextField.setColumns(10);
		
		this.raTextFieldError = new JLabel("!");
		this.raTextFieldError.setForeground(Color.RED);
		this.raTextFieldError.setFont(new Font("Tahoma", Font.BOLD, 11));
		this.panel.add(this.raTextFieldError, "cell 1 0");
		
		this.decTextFieldError = new JLabel("!");
		this.decTextFieldError.setForeground(Color.RED);
		this.decTextFieldError.setFont(new Font("Tahoma", Font.BOLD, 11));
		this.panel.add(this.decTextFieldError, "cell 1 1");
		
		this.chckbxEstTop = new JCheckBox("Pointage vers l'EST");
		this.panel.add(this.chckbxEstTop, "cell 0 2 2 1");
		
		this.resultPanel = new JPanel();
		add(this.resultPanel, "cell 1 0 1 6,grow");
		this.resultPanel.setLayout(new GridLayout(1, 1, 0, 0));
		
		this.panel_3 = new JPanel();
		this.panel_3.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Setup", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		add(this.panel_3, "cell 0 1,grow");
		this.panel_3.setLayout(new MigLayout("", "[grow][][]", "[][]"));
		
		this.setupSelector = new JComboBox();
		this.panel_3.add(this.setupSelector, "cell 0 0 3 1,growx");
		
		this.btnSetupAdd = new JButton("Nouveau");
		this.btnSetupAdd.setMargin(new Insets(2, 4, 2, 4));
		this.panel_3.add(this.btnSetupAdd, "flowx,cell 0 1,alignx right");
		
		this.btnSetupSave = new JButton("Sauver");
		this.btnSetupSave.setMargin(new Insets(2, 4, 2, 4));
		this.panel_3.add(this.btnSetupSave, "cell 1 1");
		
		this.btnSetupDel = new JButton("Supprimer");
		this.btnSetupDel.setMargin(new Insets(2, 4, 2, 4));
		this.panel_3.add(this.btnSetupDel, "cell 2 1");
		
		this.panel_1 = new JPanel();
		this.panel_1.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Rotation", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		add(this.panel_1, "cell 0 2,grow");
		this.panel_1.setLayout(new MigLayout("", "[][grow]", "[][]"));
		
		this.lblAngleMini = new JLabel("Angle mini:");
		this.panel_1.add(this.lblAngleMini, "cell 0 0");
		
		this.textFieldMinAngle = new JTextField();
		this.panel_1.add(this.textFieldMinAngle, "flowx,cell 1 0,growx");
		this.textFieldMinAngle.setColumns(10);
		
		this.lblAngleMaxi = new JLabel("Angle maxi:");
		this.panel_1.add(this.lblAngleMaxi, "cell 0 1");
		
		this.textFieldMaxAngle = new JTextField();
		this.panel_1.add(this.textFieldMaxAngle, "flowx,cell 1 1,growx");
		this.textFieldMaxAngle.setColumns(10);
		
		this.textFieldMinAngleError = new JLabel("!");
		this.textFieldMinAngleError.setForeground(Color.RED);
		this.textFieldMinAngleError.setFont(new Font("Tahoma", Font.BOLD, 11));
		this.panel_1.add(this.textFieldMinAngleError, "cell 1 0");
		
		this.textFieldMaxAngleError = new JLabel("!");
		this.textFieldMaxAngleError.setForeground(Color.RED);
		this.textFieldMaxAngleError.setFont(new Font("Tahoma", Font.BOLD, 11));
		this.panel_1.add(this.textFieldMaxAngleError, "cell 1 1");
		
		this.panel_2 = new JPanel();
		this.panel_2.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Distance au centre", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		add(this.panel_2, "cell 0 3,grow");
		this.panel_2.setLayout(new MigLayout("", "[right][grow]", "[][]"));
		
		this.lblDistanceMiniAu = new JLabel("Mini:");
		this.panel_2.add(this.lblDistanceMiniAu, "cell 0 0,alignx trailing");
		
		this.textFieldMinDist = new JTextField();
		this.panel_2.add(this.textFieldMinDist, "flowx,cell 1 0,growx");
		this.textFieldMinDist.setColumns(10);
		
		this.lblDistanceMaxiAu = new JLabel("Maxi:");
		this.panel_2.add(this.lblDistanceMaxiAu, "cell 0 1,alignx trailing");
		
		this.textFieldMaxDist = new JTextField();
		this.panel_2.add(this.textFieldMaxDist, "flowx,cell 1 1,growx");
		this.textFieldMaxDist.setColumns(10);
		
		this.textFieldMinDistError = new JLabel("!");
		this.textFieldMinDistError.setForeground(Color.RED);
		this.textFieldMinDistError.setFont(new Font("Tahoma", Font.BOLD, 11));
		this.panel_2.add(this.textFieldMinDistError, "cell 1 0");
		
		this.textFieldMaxDistError = new JLabel("!");
		this.textFieldMaxDistError.setForeground(Color.RED);
		this.textFieldMaxDistError.setFont(new Font("Tahoma", Font.BOLD, 11));
		this.panel_2.add(this.textFieldMaxDistError, "cell 1 1");
		
		this.panel_4 = new JPanel();
		this.panel_4.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Etoile", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		add(this.panel_4, "cell 0 4,grow");
		this.panel_4.setLayout(new MigLayout("", "[][grow][]", "[]"));
		
		this.lblMagLimit = new JLabel("Magnitude limite:");
		this.panel_4.add(this.lblMagLimit, "cell 0 0");
		
		this.textFieldMagLimite = new JTextField();
		this.textFieldMagLimite.setColumns(10);
		this.panel_4.add(this.textFieldMagLimite, "cell 1 0,growx");
		
		this.textFieldMagLimiteError = new JLabel("!");
		this.textFieldMagLimiteError.setForeground(Color.RED);
		this.textFieldMagLimiteError.setFont(new Font("Tahoma", Font.BOLD, 11));
		this.panel_4.add(this.textFieldMagLimiteError, "cell 2 0");

	}

}
