package fr.pludov.scopeexpress.ui;

import java.awt.Container;
import java.awt.Frame;
import java.awt.image.ImagingOpException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


public class LevelDialog extends JDialog implements ListSelectionListener {
	List<ImageListEntry> images;
	
	final ImageList listenTo;

	
	// Le slider est le log (base 10 de l'expo)/10
	final JSlider expositionSlider;
	int initialExposition;
	int lastExposition;

	final JSlider gammaSlider;
	int initialGamma;
	int lastGamma;
	
	final JSlider blackSlider;
	int initialBlack;
	int lastBlack;

	
	public LevelDialog(Frame frame, ImageList listenTo)
	{
		super(frame, "Levels", false);
		this.images = new ArrayList<ImageListEntry>();
		this.listenTo = listenTo;
		
		
		//JPanel panel = new JPanel();
		// setContentPane(panel);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
//		panel.set
//		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
//		
		
		JLabel label = new JLabel("Noir:");
		getContentPane().add(label);
		
		blackSlider = new JSlider(0, 255, 0);
		getContentPane().add(blackSlider);
		
		
		blackSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				int newValue = blackSlider.getValue();
				
				slideBlack(newValue - lastBlack);
				lastBlack = newValue;
				
			}
		});
		
		
		label = new JLabel("Exposition:");
		getContentPane().add(label);
		
		expositionSlider = new JSlider(-250, 250, 0);
		getContentPane().add(expositionSlider);
		
		
		expositionSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				int newValue = expositionSlider.getValue();
				
				slideExposition(newValue - lastExposition);
				lastExposition = newValue;
				
			}
		});
		
		label = new JLabel("Gamma:");
		getContentPane().add(label);
		gammaSlider = new JSlider(-250, 250, 0);
		getContentPane().add(gammaSlider);
		
		gammaSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				int newValue = gammaSlider.getValue();
				
				slideGamma(newValue - lastGamma);
				lastGamma = newValue;
				
			}
		});
		
		
		pack();
		
		listenTo.getSelectionModel().addListSelectionListener(this);
	}
	
	void setImageList(List<ImageListEntry> images)
	{
		this.images = new ArrayList<ImageListEntry>(images); 
		
		double exposition = 0;
		double gamma = 0;
		double black = 0;
		
		if (!images.isEmpty()) {
			for(ImageListEntry entry : images)
			{
				exposition += entry.getTarget().getExpoComposensation();
				gamma += entry.getTarget().getGamma();
				black += entry.getTarget().getBlack();
			}
			exposition /= images.size();
			gamma /= images.size();
			black /= images.size();
		}
		
		this.initialExposition = (int)Math.round(exposition);
		this.lastExposition = this.initialExposition;
		
		int min = -250; // c.a.d x100 ce qui est largement suffisent pour 8 bits
		int max = 250;
		if (min > initialExposition) {
			min = initialExposition;
		}
		if (max < initialExposition) {
			max = initialExposition;
		}
		
		expositionSlider.setValue(this.initialExposition);
		expositionSlider.setMinimum(min);
		expositionSlider.setMaximum(max);
		
		
		this.initialGamma = (int)Math.round(gamma);
		this.lastGamma = this.initialGamma;
		min = -250;
		max = 250;
		if (min > initialGamma) {
			min = initialGamma;
		}
		if (max < initialGamma) {
			max = initialGamma;
		}
		
		gammaSlider.setValue(this.initialGamma);
		gammaSlider.setMinimum(min);
		gammaSlider.setMaximum(max);
	
		this.initialBlack = (int)Math.round(black);
		this.lastBlack = this.initialBlack;
		min = 0;
		max = 255;
		if (min > initialBlack) {
			min = initialBlack;
		}
		if (max < initialBlack) {
			max = initialBlack;
		}

		blackSlider.setValue(this.initialBlack);
		blackSlider.setMinimum(min);
		blackSlider.setMaximum(max);
	}
	
	
	private void slideExposition(int delta)
	{
		if (delta == 0) return;
		for(ImageListEntry ile : this.images)
		{
			ile.getTarget().setExpoComposensation(
				ile.getTarget().getExpoComposensation() + delta );
		}
	}
	
	private void slideGamma(int delta)
	{
		if (delta == 0) return;
		for(ImageListEntry ile : this.images)
		{
			ile.getTarget().setGamma(
				ile.getTarget().getGamma() + delta );
		}
	}

	private void slideBlack(int delta)
	{
		if (delta == 0) return;
		for(ImageListEntry ile : this.images)
		{
			ile.getTarget().setBlack(
				ile.getTarget().getBlack() + delta );
		}
	}
	@Override
	public void valueChanged(ListSelectionEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				setImageList(listenTo.getSelectedEntryList());
			}
			
		});
	}
}
