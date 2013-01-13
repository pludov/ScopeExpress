package fr.pludov.cadrage.ui.focus;

import javax.swing.JLabel;

import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.StarOccurenceListener;
import fr.pludov.cadrage.utils.WeakListenerOwner;
import fr.pludov.utils.ChannelMode;

public class StarDetail extends StarDetailDesign {
	final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	final StarOccurence3DView view;

	final Mosaic focus;
	StarOccurence so;
	
	final JLabel [] lblBlackList;
	final JLabel [] lblAduMaxList;
	final JLabel [] lblAduSumList;
	
	public StarDetail(Mosaic fo) {
		super();
		this.focus = fo;
		this.so = null;
		view = new StarOccurence3DView();
		this.lblBlackList = new JLabel[] {
				this.lblValBlackRed,
				this.lblValBlackGreen,
				this.lblValBlackBlue,
				this.lblValBlackGrey
		};
		
		this.lblAduMaxList = new JLabel[] {
				this.lblValAduMaxRed,
				this.lblValAduMaxGreen,
				this.lblValAduMaxBlue,
				this.lblValAduMaxGrey
		};
		
		this.lblAduSumList = new JLabel[] {
				this.lblValAduSumRed,
				this.lblValAduSumGreen,
				this.lblValAduSumBlue,
				this.lblValAduSumGrey
		};
		
		
		this.graphPanel.add(view);
		this.focus.listeners.addListener(this.listenerOwner, new MosaicListener() {
			
			@Override
			public void starRemoved(Star star) {
			}
			
			@Override
			public void starOccurenceRemoved(StarOccurence sco) {
				if (sco == StarDetail.this.so) {
					setStarOccurence(null);
				}
			}
			
			@Override
			public void starOccurenceAdded(StarOccurence sco) {
			}
			
			@Override
			public void starAdded(Star star) {
				
			}
			
			@Override
			public void imageRemoved(Image image) {
				
			}
			
			@Override
			public void imageAdded(Image image, MosaicListener.ImageAddedCause cause) {
				
			}
		});
		
		refreshLabels();
	}

	private String doubleToString(double d)
	{
		return Double.toString(d);
	}
	
	private void refreshLabels()
	{
		if (this.so != null && this.so.isAnalyseDone() && this.so.isStarFound()) {
			this.lblFWHM.setText(doubleToString(this.so.getFwhm()));
			this.lblStdDev.setText(doubleToString(this.so.getStddev()));
			int aduMax = 0;
			int aduSum = 0;
			for(int i = 0; i < 3; ++i)
			{
				int aduMaxC = so.getAduMaxByChannel()[i];
				int aduSumC = so.getAduSumByChannel()[i];
				this.lblAduMaxList[i].setText(Integer.toString(aduMaxC));
				this.lblAduSumList[i].setText(Integer.toString(aduSumC));
				this.lblBlackList[i].setText(Integer.toString(so.getBlackLevel(ChannelMode.values()[i])));
				
				if (aduMaxC > aduMax) {
					aduMax = aduMaxC;
				}
				
				aduSum += aduSumC;
			}
			this.lblAduMaxList[3].setText(Integer.toString(aduMax));
			this.lblAduSumList[3].setText(Integer.toString(aduSum));
			this.lblBlackList[3].setText("");
		} else {
			this.lblFWHM.setText("");
			this.lblStdDev.setText("");

			if (this.so != null) {
				if (!this.so.isAnalyseDone()) {
					this.lblFWHM.setText("** en attente **");
					this.lblStdDev.setText("** en attente **");
					
				} else if (!this.so.isStarFound()) {
					this.lblFWHM.setText("** introuvable **");
					this.lblStdDev.setText("** introuvable **");
				}
			}
			for(int i = 0; i < 4; ++i)
			{
				this.lblAduMaxList[i].setText("");
				this.lblAduSumList[i].setText("");
				this.lblBlackList[i].setText("");
			}
		}
	}
	
	public void setStarOccurence(StarOccurence so)
	{
		if (this.so == so) {
			return;
		}
		
		if (this.so != null) {
			this.so.listeners.removeListener(this.listenerOwner);
		}
		
		this.so = so;
		view.setStarOccurence(so);
		
		if (this.so != null) {
			this.so.listeners.addListener(this.listenerOwner, new StarOccurenceListener() {
				@Override
				public void analyseDone() {
					refreshLabels();
				}

				@Override
				public void imageUpdated() {
				}
				
			});
		}
		
		refreshLabels();
	}
	
}
