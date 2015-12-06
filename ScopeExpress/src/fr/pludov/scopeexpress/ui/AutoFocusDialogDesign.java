package fr.pludov.scopeexpress.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.border.LineBorder;

import java.awt.Color;

import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.Dimension;
import javax.swing.JTabbedPane;
import javax.swing.BoxLayout;

public class AutoFocusDialogDesign extends JDialog {

	private final JPanel contentPanel = new JPanel();
	protected JTextField startPositionTextField;
	protected JTextField pass1WidthTextField;
	protected JTextField pass1StepCountTextField;
	protected JTextField pass2WidthTextField;
	protected JTextField pass2StepCountTextField;
	protected JTextField pass3StepCountTextField;
	protected JTextField pass3WidthTextField;
	protected JTextField photoDurationTextField;
	protected JTextField photoCountTextField;
	protected JTextField backlashTextField;
	protected JLabel pass3StepCountErrorLabel;
	protected JLabel startPositionErrorLabel;
	protected JLabel pass3WidthErrorLabel;
	protected JLabel photoDurationErrorLabel;
	protected JLabel photoCountErrorLabel;
	protected JLabel pass2WidthErrorLabel;
	protected JLabel pass1StepCountErrorLabel;
	protected JLabel pass1WidthErrorLabel;
	protected JLabel pass2StepCountErrorLabel;
	protected JLabel backlashErrorLabel;
	protected JPanel panel_1;
	protected JCheckBox pass1Checkbox;
	protected JCheckBox pass2Checkbox;
	protected JCheckBox pass3Checkbox;
	protected JPanel panel_2;
	protected JLabel statusLabel;
	protected JLabel lblPasseEnCours;
	protected JLabel lblImageEnCours;
	protected JLabel lblCurrentPosition;
	protected JTextField currentPassTextField;
	protected JTextField currentImageTextField;
	protected JTextField currentPositionTextField;
	protected JButton btnInterrompre;
	protected JButton btnStart;
	protected JLabel lblCurrentStep;
	protected JTextField currentStepTextField;
	protected JTabbedPane graphTab;
	protected JPanel pass1GraphPanelParent;
	protected JPanel pass2GraphPanelParent;
	protected JPanel pass3GraphPanelParent;
	protected JButton btnClose;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			AutoFocusDialogDesign dialog = new AutoFocusDialogDesign(null);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public AutoFocusDialogDesign(Window window) {
		super(window);
		setBounds(100, 100, 793, 530);
		getContentPane().setLayout(new BorderLayout());
		{
			this.panel_1 = new JPanel();
			getContentPane().add(this.panel_1, BorderLayout.CENTER);
			this.panel_1.setLayout(new MigLayout("", "[226px][320px:n,grow]", "[grow]"));
			this.panel_1.add(contentPanel, "cell 0 0,alignx left,aligny top");
			this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			contentPanel.setLayout(new MigLayout("", "[][100px:n,grow][]", "[][][][][][][][][][]"));
			{
				JLabel lblPositionCible = new JLabel("Position cible :");
				contentPanel.add(lblPositionCible, "cell 0 0,alignx trailing");
			}
			{
				this.startPositionTextField = new JTextField();
				contentPanel.add(this.startPositionTextField, "cell 1 0,growx");
				this.startPositionTextField.setColumns(10);
			}
			{
				startPositionErrorLabel = new JLabel("(!)");
				startPositionErrorLabel.setForeground(Color.RED);
				startPositionErrorLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
				contentPanel.add(startPositionErrorLabel, "cell 2 0");
			}
			{
				JLabel lblDureExposition = new JLabel("Dur\u00E9e exposition :");
				contentPanel.add(lblDureExposition, "cell 0 1,alignx trailing");
			}
			{
				this.photoDurationTextField = new JTextField();
				this.photoDurationTextField.setColumns(10);
				contentPanel.add(this.photoDurationTextField, "cell 1 1,growx");
			}
			{
				photoDurationErrorLabel = new JLabel("(!)");
				photoDurationErrorLabel.setForeground(Color.RED);
				photoDurationErrorLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
				contentPanel.add(photoDurationErrorLabel, "cell 2 1");
			}
			{
				JLabel lblImagesParPas = new JLabel("Images par pas :");
				contentPanel.add(lblImagesParPas, "cell 0 2,alignx trailing");
			}
			{
				this.photoCountTextField = new JTextField();
				contentPanel.add(this.photoCountTextField, "cell 1 2,growx");
				this.photoCountTextField.setColumns(10);
			}
			{
				photoCountErrorLabel = new JLabel("(!)");
				photoCountErrorLabel.setForeground(Color.RED);
				photoCountErrorLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
				contentPanel.add(photoCountErrorLabel, "cell 2 2");
			}
			{
				JLabel lblBackslash = new JLabel("Backlash :");
				contentPanel.add(lblBackslash, "cell 0 3,alignx trailing");
			}
			{
				this.backlashTextField = new JTextField();
				contentPanel.add(this.backlashTextField, "cell 1 3,growx");
				this.backlashTextField.setColumns(10);
			}
			{
				backlashErrorLabel = new JLabel("(!)");
				backlashErrorLabel.setForeground(Color.RED);
				backlashErrorLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
				contentPanel.add(backlashErrorLabel, "cell 2 3");
			}
			{
				pass1Checkbox = new JCheckBox("Passe n\u00B01");
				pass1Checkbox.setVerticalAlignment(SwingConstants.BOTTOM);
				contentPanel.add(pass1Checkbox, "cell 0 4 3 1");
			}
			{
				JPanel panel = new JPanel();
				panel.setBorder(new LineBorder(new Color(0, 0, 0)));
				contentPanel.add(panel, "cell 0 5 3 1,grow");
				panel.setLayout(new MigLayout("", "[][grow][]", "[][]"));
				{
					JLabel lblLargeurEnPas = new JLabel("Largeur en pas :");
					panel.add(lblLargeurEnPas, "cell 0 0,alignx trailing");
				}
				{
					this.pass1WidthTextField = new JTextField();
					panel.add(this.pass1WidthTextField, "cell 1 0,growx");
					this.pass1WidthTextField.setColumns(10);
				}
				{
					pass1WidthErrorLabel = new JLabel("(!)");
					pass1WidthErrorLabel.setForeground(Color.RED);
					pass1WidthErrorLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
					panel.add(pass1WidthErrorLabel, "cell 2 0");
				}
				{
					JLabel lblNombreDePas = new JLabel("Nombre de pas :");
					panel.add(lblNombreDePas, "cell 0 1,alignx trailing");
				}
				{
					this.pass1StepCountTextField = new JTextField();
					panel.add(this.pass1StepCountTextField, "cell 1 1,growx");
					this.pass1StepCountTextField.setColumns(10);
				}
				{
					pass1StepCountErrorLabel = new JLabel("(!)");
					pass1StepCountErrorLabel.setForeground(Color.RED);
					pass1StepCountErrorLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
					panel.add(pass1StepCountErrorLabel, "cell 2 1");
				}
			}
			{
				pass2Checkbox = new JCheckBox("Passe n\u00B02");
				pass2Checkbox.setVerticalAlignment(SwingConstants.BOTTOM);
				contentPanel.add(pass2Checkbox, "cell 0 6 3 1");
			}
			{
				JPanel panel = new JPanel();
				panel.setBorder(new LineBorder(new Color(0, 0, 0)));
				contentPanel.add(panel, "cell 0 7 3 1,grow");
				panel.setLayout(new MigLayout("", "[][grow][]", "[][]"));
				{
					JLabel label = new JLabel("Largeur en pas :");
					panel.add(label, "cell 0 0,alignx trailing");
				}
				{
					this.pass2WidthTextField = new JTextField();
					panel.add(this.pass2WidthTextField, "cell 1 0,growx");
					this.pass2WidthTextField.setColumns(10);
				}
				{
					pass2WidthErrorLabel = new JLabel("(!)");
					pass2WidthErrorLabel.setForeground(Color.RED);
					pass2WidthErrorLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
					panel.add(pass2WidthErrorLabel, "cell 2 0");
				}
				{
					JLabel label = new JLabel("Nombre de pas :");
					panel.add(label, "cell 0 1,alignx trailing");
				}
				{
					this.pass2StepCountTextField = new JTextField();
					panel.add(this.pass2StepCountTextField, "cell 1 1,growx");
					this.pass2StepCountTextField.setColumns(10);
				}
				{
					pass2StepCountErrorLabel = new JLabel("(!)");
					pass2StepCountErrorLabel.setForeground(Color.RED);
					pass2StepCountErrorLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
					panel.add(pass2StepCountErrorLabel, "cell 2 1");
				}
			}
			{
				pass3Checkbox = new JCheckBox("Passe n\u00B03");
				pass3Checkbox.setVerticalAlignment(SwingConstants.BOTTOM);
				contentPanel.add(pass3Checkbox, "flowy,cell 0 8 3 1");
			}
			{
				JPanel panel = new JPanel();
				panel.setBorder(new LineBorder(new Color(0, 0, 0)));
				contentPanel.add(panel, "cell 0 9 3 1,grow");
				panel.setLayout(new MigLayout("", "[][grow][]", "[][]"));
				{
					JLabel label = new JLabel("Largeur en pas :");
					panel.add(label, "flowx,cell 0 0 2 1");
				}
				{
					pass3WidthErrorLabel = new JLabel("(!)");
					pass3WidthErrorLabel.setForeground(Color.RED);
					pass3WidthErrorLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
					panel.add(pass3WidthErrorLabel, "cell 2 0");
				}
				{
					JLabel label = new JLabel("Nombre de pas :");
					panel.add(label, "cell 0 1,alignx trailing");
				}
				{
					this.pass3StepCountTextField = new JTextField();
					panel.add(this.pass3StepCountTextField, "cell 1 1,growx");
					this.pass3StepCountTextField.setColumns(10);
				}
				{
					this.pass3WidthTextField = new JTextField();
					panel.add(this.pass3WidthTextField, "cell 1 0,growx");
					this.pass3WidthTextField.setColumns(10);
				}
				{
					pass3StepCountErrorLabel = new JLabel("(!)");
					pass3StepCountErrorLabel.setForeground(Color.RED);
					pass3StepCountErrorLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
					panel.add(pass3StepCountErrorLabel, "cell 2 1");
				}
			}
			{
				this.panel_2 = new JPanel();
				this.panel_1.add(this.panel_2, "cell 1 0,grow");
				this.panel_2.setLayout(new MigLayout("", "[grow][grow]", "[][][][][][grow]"));
				{
					this.statusLabel = new JLabel("New label");
					this.statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
					this.statusLabel.setMinimumSize(new Dimension(160, 44));
					this.statusLabel.setFont(new Font("DejaVu Sans Condensed", Font.BOLD, 14));
					this.panel_2.add(this.statusLabel, "cell 0 0 2 1,growx");
				}
				{
					this.lblPasseEnCours = new JLabel("Passe:");
					this.lblPasseEnCours.setHorizontalAlignment(SwingConstants.RIGHT);
					this.panel_2.add(this.lblPasseEnCours, "cell 0 1,alignx trailing");
				}
				{
					this.currentPassTextField = new JTextField();
					this.currentPassTextField.setEnabled(false);
					this.currentPassTextField.setEditable(false);
					this.panel_2.add(this.currentPassTextField, "cell 1 1,growx");
					this.currentPassTextField.setColumns(10);
				}
				{
					this.lblCurrentStep = new JLabel("Pas:");
					this.lblCurrentStep.setHorizontalAlignment(SwingConstants.RIGHT);
					this.panel_2.add(this.lblCurrentStep, "cell 0 2,alignx trailing");
				}
				{
					this.currentStepTextField = new JTextField();
					this.currentStepTextField.setEditable(false);
					this.currentStepTextField.setEnabled(false);
					this.panel_2.add(this.currentStepTextField, "cell 1 2,growx");
					this.currentStepTextField.setColumns(10);
				}
				{
					this.lblImageEnCours = new JLabel("Image:");
					this.lblImageEnCours.setHorizontalAlignment(SwingConstants.RIGHT);
					this.panel_2.add(this.lblImageEnCours, "cell 0 3,alignx trailing");
				}
				{
					this.currentImageTextField = new JTextField();
					this.currentImageTextField.setEnabled(false);
					this.currentImageTextField.setEditable(false);
					this.panel_2.add(this.currentImageTextField, "cell 1 3,growx");
					this.currentImageTextField.setColumns(10);
				}
				{
					this.lblCurrentPosition = new JLabel("Position:");
					this.lblCurrentPosition.setHorizontalAlignment(SwingConstants.RIGHT);
					this.panel_2.add(this.lblCurrentPosition, "cell 0 4,alignx trailing");
				}
				{
					this.currentPositionTextField = new JTextField();
					this.currentPositionTextField.setEnabled(false);
					this.currentPositionTextField.setEditable(false);
					this.panel_2.add(this.currentPositionTextField, "cell 1 4,growx");
					this.currentPositionTextField.setColumns(10);
				}
				{
					this.graphTab = new JTabbedPane(JTabbedPane.TOP);
					this.panel_2.add(this.graphTab, "cell 0 5 2 1,grow");
					{
						this.pass1GraphPanelParent = new JPanel();
						this.graphTab.addTab("Passe #1", null, this.pass1GraphPanelParent, null);
						this.pass1GraphPanelParent.setLayout(new BoxLayout(this.pass1GraphPanelParent, BoxLayout.X_AXIS));
					}
					{
						this.pass2GraphPanelParent = new JPanel();
						this.graphTab.addTab("Passe #2", null, this.pass2GraphPanelParent, null);
						this.pass2GraphPanelParent.setLayout(new BoxLayout(this.pass2GraphPanelParent, BoxLayout.X_AXIS));
					}
					{
						this.pass3GraphPanelParent = new JPanel();
						this.graphTab.addTab("Passe #3", null, this.pass3GraphPanelParent, null);
						this.pass3GraphPanelParent.setLayout(new BoxLayout(this.pass3GraphPanelParent, BoxLayout.X_AXIS));
					}
				}
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				btnStart = new JButton("D\u00E9marrer");
				btnStart.setActionCommand("");
				buttonPane.add(btnStart);
				getRootPane().setDefaultButton(btnStart);
			}
			{
				this.btnInterrompre = new JButton("Interrompre");
				buttonPane.add(this.btnInterrompre);
			}
			{
				btnClose = new JButton("Fermer");
				btnClose.setActionCommand("Cancel");
				buttonPane.add(btnClose);
			}
		}
	}

	public JLabel getLabel() {
		return this.pass3StepCountErrorLabel;
	}
	public JLabel getStartPositionErrorLabel() {
		return this.startPositionErrorLabel;
	}
	public JLabel getLabel2() {
		return this.pass3WidthErrorLabel;
	}
	public JLabel getLabel3() {
		return this.photoDurationErrorLabel;
	}
	public JLabel getLabel4() {
		return this.photoCountErrorLabel;
	}
	public JLabel getLabel5() {
		return this.pass2WidthErrorLabel;
	}
	public JLabel getLabel6() {
		return this.pass1StepCountErrorLabel;
	}
	public JLabel getLabel7() {
		return this.pass1WidthErrorLabel;
	}
	public JLabel getLabel8() {
		return this.pass2StepCountErrorLabel;
	}
	public JLabel getLabel9() {
		return this.backlashErrorLabel;
	}
	public JCheckBox getPass1Checkbox() {
		return this.pass1Checkbox;
	}
	public JCheckBox getPass2Checkbox() {
		return this.pass2Checkbox;
	}
	public JCheckBox getPass3Checkbox() {
		return this.pass3Checkbox;
	}
	public JLabel getStatusLabel() {
		return this.statusLabel;
	}
	public JButton getBtnInterrompre() {
		return this.btnInterrompre;
	}
	public JButton getBtnStart() {
		return this.btnStart;
	}
	public JButton getBtnClose() {
		return this.btnClose;
	}
}
