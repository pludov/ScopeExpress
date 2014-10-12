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
import javax.swing.JToolBar;
import javax.swing.JToggleButton;
import javax.swing.ImageIcon;

public class ReCenterDialogDesign extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JToggleButton tglbtnSpeak;
	private JButton btnCenter;
	private JLabel lblDiffDec;
	private JLabel lblDiffRa;
	private JLabel lblCurImage;
	private JLabel lblDiffRotation;
	private JLabel lblRefImage;
	private JButton cancelButton;

	/**
	 * Create the dialog.
	 */
	public ReCenterDialogDesign(Window parent) {
		super(parent);
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JPanel panel = new JPanel();
			contentPanel.add(panel, BorderLayout.CENTER);
			panel.setLayout(new MigLayout("", "[][grow]", "[][][][][]"));
			{
				JLabel lblTitleRefImage = new JLabel("Image de r\u00E9f\u00E9rence :");
				panel.add(lblTitleRefImage, "cell 0 0,alignx right,aligny center");
			}
			{
				lblRefImage = new JLabel("img00000.tiff");
				panel.add(lblRefImage, "cell 1 0,alignx left,aligny center");
			}
			{
				JLabel lblNewLabel = new JLabel("Image en cours :");
				panel.add(lblNewLabel, "cell 0 1,alignx right,aligny center");
			}
			{
				lblCurImage = new JLabel("image000001.tiff");
				panel.add(lblCurImage, "cell 1 1,alignx left,aligny center");
			}
			{
				JLabel lblTitleRA = new JLabel("Ecart AD :");
				panel.add(lblTitleRA, "cell 0 2,alignx right,aligny center");
			}
			{
				lblDiffRa = new JLabel("000000");
				panel.add(lblDiffRa, "cell 1 2,alignx left,aligny center");
			}
			{
				JLabel lblTitleDec = new JLabel("Ecart Dec :");
				panel.add(lblTitleDec, "cell 0 3,alignx right,aligny center");
			}
			{
				lblDiffDec = new JLabel("000000");
				panel.add(lblDiffDec, "cell 1 3,alignx left,aligny center");
			}
			{
				JLabel lblTitleRotation = new JLabel("Ecart Rotation :");
				panel.add(lblTitleRotation, "cell 0 4,alignx right,aligny center");
			}
			{
				lblDiffRotation = new JLabel("000000");
				panel.add(lblDiffRotation, "cell 1 4,alignx left,aligny center");
			}
		}
		{
			JToolBar toolBar = new JToolBar();
			contentPanel.add(toolBar, BorderLayout.NORTH);
			{
				tglbtnSpeak = new JToggleButton("");
				tglbtnSpeak.setIcon(new ImageIcon(ReCenterDialogDesign.class.getResource("/fr/pludov/scopeexpress/ui/resources/icons/text-speak.png")));
				tglbtnSpeak.setToolTipText("Indiquer les corrections \u00E0 apporter en utilisant la synth\u00E8se vocale");
				toolBar.add(tglbtnSpeak);
			}
			{
				btnCenter = new JButton("Goto");
				toolBar.add(btnCenter);
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				cancelButton = new JButton("Fermer");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	protected JToggleButton getTglbtnSpeak() {
		return this.tglbtnSpeak;
	}
	protected JButton getBtnCenter() {
		return this.btnCenter;
	}
	protected JLabel getLblDiffDec() {
		return this.lblDiffDec;
	}
	protected JLabel getLblDiffRa() {
		return this.lblDiffRa;
	}
	protected JLabel getLblCurImage() {
		return this.lblCurImage;
	}
	protected JLabel getLblDiffRotation() {
		return this.lblDiffRotation;
	}
	protected JLabel getLblRefImage() {
		return this.lblRefImage;
	}
	protected JButton getCancelButton() {
		return this.cancelButton;
	}
}
