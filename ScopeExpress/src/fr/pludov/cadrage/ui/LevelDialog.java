package fr.pludov.cadrage.ui;

import java.awt.Container;
import java.awt.Frame;
import java.awt.image.ImagingOpException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import fr.pludov.cadrage.ui.ImageList.ImageListEntry;

public class LevelDialog extends JDialog implements ListSelectionListener {
	List<ImageListEntry> images;
	
	final ImageList listenTo;
	
	int initialExposition;
	int lastExposition;
	
	// Le slider est le log (base 10 de l'expo)/10
	final JSlider slider;
	
	public LevelDialog(Frame frame, ImageList listenTo)
	{
		super(frame, "Levels", false);
		this.images = new ArrayList<ImageListEntry>();
		this.listenTo = listenTo;
		
		slider = new JSlider(-250, 250, initialExposition);
		Container contentPane = getContentPane();
		contentPane.add(slider);
		
		slider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				int newValue = slider.getValue();
				
				slideExposition(newValue - lastExposition);
				lastExposition = newValue;
				
			}
		});
		
		listenTo.getSelectionModel().addListSelectionListener(this);
	}
	
	void setImageList(List<ImageListEntry> images)
	{
		this.images = new ArrayList<ImageList.ImageListEntry>(images); 
		
		double exposition = 0;
		if (!images.isEmpty()) {
			for(ImageListEntry entry : images)
			{
				exposition += entry.getTarget().getExpoComposensation();
			}
			exposition /= images.size();
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
		
		slider.setValue(this.initialExposition);
		slider.setMinimum(min);
		slider.setMaximum(max);
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
