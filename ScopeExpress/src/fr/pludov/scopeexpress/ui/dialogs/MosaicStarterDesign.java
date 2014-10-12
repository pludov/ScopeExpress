package fr.pludov.scopeexpress.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.BevelBorder;
import net.miginfocom.swing.MigLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import org.eclipse.wb.swing.FocusTraversalOnArray;
import java.awt.Component;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import java.awt.Font;
import java.awt.Color;

public class MosaicStarterDesign extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JPanel panel;
	private JCheckBox doStarCheckBox;
	private JLabel lblAscenscionDroite;
	private JLabel lblDclinaison;
	private JLabel lblRayondeg;
	protected JTextField raTextField;
	protected JTextField decTextField;
	protected JTextField radiusTextField;
	protected JLabel lblMagnitudeLimite;
	protected JTextField magTextField;
	private JButton cancelButton;
	private JButton okButton;
	protected JLabel raErrorLbl;
	protected JLabel decErrorLbl;
	protected JLabel radiusErrorLbl;
	protected JLabel magErrorLbl;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			MosaicStarterDesign dialog = new MosaicStarterDesign(null);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public MosaicStarterDesign(Window owner) {
		super(owner);
		setTitle("Nouvelle projection");
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			panel = new JPanel();
			panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Param\u00E8tres de projection", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			contentPanel.add(panel);
			panel.setLayout(new MigLayout("", "[][grow]", "[][][][]"));
			{
				lblAscenscionDroite = new JLabel("Ascenscion droite:");
				panel.add(lblAscenscionDroite, "cell 0 0,alignx trailing");
			}
			{
				this.raTextField = new JTextField();
				this.panel.add(this.raTextField, "flowx,cell 1 0,growx");
				this.raTextField.setColumns(10);
			}
			{
				lblDclinaison = new JLabel("D\u00E9clinaison :");
				panel.add(lblDclinaison, "cell 0 1,alignx trailing");
			}
			{
				this.decTextField = new JTextField();
				this.panel.add(this.decTextField, "flowx,cell 1 1,growx");
				this.decTextField.setColumns(10);
			}
			{
				lblRayondeg = new JLabel("Rayon (deg) :");
				panel.add(lblRayondeg, "cell 0 2,alignx trailing");
			}
			{
				this.radiusTextField = new JTextField();
				this.panel.add(this.radiusTextField, "flowx,cell 1 2,growx");
				this.radiusTextField.setColumns(10);
			}
			{
				this.lblMagnitudeLimite = new JLabel("Magnitude limite :");
				this.panel.add(this.lblMagnitudeLimite, "cell 0 3,alignx trailing");
			}
			{
				this.magTextField = new JTextField();
				this.panel.add(this.magTextField, "flowx,cell 1 3,growx");
				this.magTextField.setColumns(10);
			}
			{
				this.raErrorLbl = new JLabel("(!)");
				this.raErrorLbl.setForeground(Color.RED);
				this.raErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 10));
				this.panel.add(this.raErrorLbl, "cell 1 0");
			}
			{
				this.decErrorLbl = new JLabel("(!)");
				this.decErrorLbl.setForeground(Color.RED);
				this.decErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 10));
				this.panel.add(this.decErrorLbl, "cell 1 1");
			}
			{
				this.radiusErrorLbl = new JLabel("(!)");
				this.radiusErrorLbl.setForeground(Color.RED);
				this.radiusErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 10));
				this.panel.add(this.radiusErrorLbl, "cell 1 2");
			}
			{
				this.magErrorLbl = new JLabel("(!)");
				this.magErrorLbl.setForeground(Color.RED);
				this.magErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 10));
				this.panel.add(this.magErrorLbl, "cell 1 3");
			}
		}
		{
			doStarCheckBox = new JCheckBox("Projection d'\u00E9toiles");
			this.doStarCheckBox.setToolTipText("Cr\u00E9er une projection qui inclu des \u00E9toile et poss\u00E8de des coordonn\u00E9es equatoriales bien d\u00E9finies");
			contentPanel.add(this.doStarCheckBox, BorderLayout.NORTH);
		}
		contentPanel.setFocusTraversalPolicy(new FocusTraversalOnArray(new Component[]{this.panel, this.doStarCheckBox, this.lblAscenscionDroite, this.lblDclinaison, this.lblRayondeg}));
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	protected JCheckBox getDoStarCheckBox() {
		return this.doStarCheckBox;
	}
	protected JTextField getRadiusTextField() {
		return this.radiusTextField;
	}
	protected JTextField getRaTextField() {
		return this.raTextField;
	}
	protected JTextField getDecTextField() {
		return this.decTextField;
	}
	protected JTextField getMagTextField() {
		return this.magTextField;
	}
	public JPanel getProjectionPanel() {
		return this.panel;
	}
	public JButton getCancelButton() {
		return this.cancelButton;
	}
	public JButton getOkButton() {
		return this.okButton;
	}
}
