package fr.pludov.cadrage.ui.focus;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import java.awt.Font;

public class StarDetailDesign extends JPanel {
	protected JPanel graphPanel;
	protected JLabel lblTxtFwhm;
	protected JLabel lblTxtStddev;
	protected JLabel lblTxtAduMax;
	protected JLabel lblTxtAduTotal;
	protected JLabel lblFWHM;
	protected JLabel lblStdDev;
	protected JLabel lblValAduMaxGrey;
	protected JLabel lblValAduSumGrey;
	protected JLabel lblBlack;
	protected JLabel lblValBlackGrey;
	protected JLabel lblGrey;
	protected JLabel lblRed;
	protected JLabel lblGreen;
	protected JLabel lblBlue;
	protected JLabel lblValBlackRed;
	protected JLabel lblValBlackGreen;
	protected JLabel lblValBlackBlue;
	protected JLabel lblValAduMaxRed;
	protected JLabel lblValAduMaxGreen;
	protected JLabel lblValAduMaxBlue;
	protected JLabel lblValAduSumRed;
	protected JLabel lblValAduSumGreen;
	protected JLabel lblValAduSumBlue;

	/**
	 * Create the panel.
	 */
	public StarDetailDesign() {
		setLayout(new MigLayout("", "[right][grow][grow][grow][grow]", "[280px:n:280px,fill][][][][][][]"));
		
		this.graphPanel = new JPanel();
		add(this.graphPanel, "cell 0 0 5 1,grow");
		this.graphPanel.setLayout(new BoxLayout(this.graphPanel, BoxLayout.X_AXIS));
		
		this.lblTxtFwhm = new JLabel("FWHM:");
		add(this.lblTxtFwhm, "cell 0 1");
		
		this.lblFWHM = new JLabel("New label");
		this.lblFWHM.setFont(new Font("Tahoma", Font.PLAIN, 10));
		add(this.lblFWHM, "cell 1 1 4 1");
		
		this.lblTxtStddev = new JLabel("STDDEV:");
		add(this.lblTxtStddev, "cell 0 2");
		
		this.lblStdDev = new JLabel("New label");
		this.lblStdDev.setFont(new Font("Tahoma", Font.PLAIN, 10));
		add(this.lblStdDev, "cell 1 2 4 1");
		
		this.lblGrey = new JLabel("Grey");
		add(this.lblGrey, "cell 1 3");
		
		this.lblRed = new JLabel("Red");
		add(this.lblRed, "cell 2 3");
		
		this.lblGreen = new JLabel("Green");
		add(this.lblGreen, "cell 3 3");
		
		this.lblBlue = new JLabel("Blue");
		add(this.lblBlue, "cell 4 3");
		
		this.lblBlack = new JLabel("Black:");
		add(this.lblBlack, "cell 0 4");
		
		this.lblValBlackGrey = new JLabel("New label");
		this.lblValBlackGrey.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValBlackGrey, "cell 1 4");
		
		this.lblValBlackRed = new JLabel("New label");
		this.lblValBlackRed.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValBlackRed, "cell 2 4");
		
		this.lblValBlackGreen = new JLabel("New label");
		this.lblValBlackGreen.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValBlackGreen, "cell 3 4");
		
		this.lblValBlackBlue = new JLabel("New label");
		this.lblValBlackBlue.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValBlackBlue, "cell 4 4");
		
		this.lblTxtAduMax = new JLabel("ADU Max:");
		add(this.lblTxtAduMax, "cell 0 5");
		
		this.lblValAduMaxGrey = new JLabel("New label");
		this.lblValAduMaxGrey.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduMaxGrey, "cell 1 5");
		
		this.lblValAduMaxRed = new JLabel("New label");
		this.lblValAduMaxRed.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduMaxRed, "cell 2 5");
		
		this.lblValAduMaxGreen = new JLabel("New label");
		this.lblValAduMaxGreen.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduMaxGreen, "cell 3 5");
		
		this.lblValAduMaxBlue = new JLabel("New label");
		this.lblValAduMaxBlue.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduMaxBlue, "cell 4 5");
		
		this.lblTxtAduTotal = new JLabel("ADU Total:");
		add(this.lblTxtAduTotal, "cell 0 6");
		
		this.lblValAduSumGrey = new JLabel("New label");
		this.lblValAduSumGrey.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduSumGrey, "cell 1 6");
		
		this.lblValAduSumRed = new JLabel("New label");
		this.lblValAduSumRed.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduSumRed, "cell 2 6");
		
		this.lblValAduSumGreen = new JLabel("New label");
		this.lblValAduSumGreen.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduSumGreen, "cell 3 6");
		
		this.lblValAduSumBlue = new JLabel("New label");
		this.lblValAduSumBlue.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduSumBlue, "cell 4 6");

	}
}
