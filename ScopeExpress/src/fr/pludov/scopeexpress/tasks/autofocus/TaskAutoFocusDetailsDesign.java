package fr.pludov.scopeexpress.tasks.autofocus;

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

public class TaskAutoFocusDetailsDesign extends JPanel {
	protected JPanel panel_2;
	protected JLabel lblPasseEnCours;
	protected JLabel lblImageEnCours;
	protected JLabel lblCurrentPosition;
	protected JTextField currentPassTextField;
	protected JTextField currentImageTextField;
	protected JTextField currentPositionTextField;
	protected JLabel lblCurrentStep;
	protected JTextField currentStepTextField;
	protected JPanel pass1GraphPanelParent;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			TaskAutoFocusDetailsDesign dialog = new TaskAutoFocusDetailsDesign();
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public TaskAutoFocusDetailsDesign() {
		super();
		setBounds(100, 100, 793, 530);
		setLayout(new BorderLayout());
		{
			this.panel_2 = new JPanel();
			add(this.panel_2, BorderLayout.CENTER);
			this.panel_2.setLayout(new MigLayout("", "[80px:n,grow 10][grow]", "[][][][][grow]"));
			{
				this.lblPasseEnCours = new JLabel("Passe:");
				this.lblPasseEnCours.setHorizontalAlignment(SwingConstants.RIGHT);
				this.panel_2.add(this.lblPasseEnCours, "cell 0 0,alignx trailing");
			}
			{
				this.currentPassTextField = new JTextField();
				this.currentPassTextField.setEnabled(false);
				this.currentPassTextField.setEditable(false);
				this.panel_2.add(this.currentPassTextField, "cell 1 0,growx");
				this.currentPassTextField.setColumns(10);
			}
			{
				this.lblCurrentStep = new JLabel("Pas:");
				this.lblCurrentStep.setHorizontalAlignment(SwingConstants.RIGHT);
				this.panel_2.add(this.lblCurrentStep, "cell 0 1,alignx trailing");
			}
			{
				this.currentStepTextField = new JTextField();
				this.currentStepTextField.setEditable(false);
				this.currentStepTextField.setEnabled(false);
				this.panel_2.add(this.currentStepTextField, "cell 1 1,growx");
				this.currentStepTextField.setColumns(10);
			}
			{
				this.lblImageEnCours = new JLabel("Image:");
				this.lblImageEnCours.setHorizontalAlignment(SwingConstants.RIGHT);
				this.panel_2.add(this.lblImageEnCours, "cell 0 2,alignx trailing");
			}
			{
				this.currentImageTextField = new JTextField();
				this.currentImageTextField.setEnabled(false);
				this.currentImageTextField.setEditable(false);
				this.panel_2.add(this.currentImageTextField, "cell 1 2,growx");
				this.currentImageTextField.setColumns(10);
			}
			{
				this.lblCurrentPosition = new JLabel("Position:");
				this.lblCurrentPosition.setHorizontalAlignment(SwingConstants.RIGHT);
				this.panel_2.add(this.lblCurrentPosition, "cell 0 3,alignx trailing");
			}
			{
				this.currentPositionTextField = new JTextField();
				this.currentPositionTextField.setEnabled(false);
				this.currentPositionTextField.setEditable(false);
				this.panel_2.add(this.currentPositionTextField, "cell 1 3,growx");
				this.currentPositionTextField.setColumns(10);
			}
			{
				this.pass1GraphPanelParent = new JPanel();
				this.panel_2.add(this.pass1GraphPanelParent, "cell 0 4 2 1,grow");
				this.pass1GraphPanelParent.setLayout(new BoxLayout(this.pass1GraphPanelParent, BoxLayout.X_AXIS));
			}
		}
	}
}
