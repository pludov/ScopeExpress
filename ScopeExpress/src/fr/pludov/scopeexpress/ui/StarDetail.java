package fr.pludov.scopeexpress.ui;

import javax.swing.JLabel;

import fr.pludov.scopeexpress.focus.ExclusionZone;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.focus.PointOfInterest;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarCorrelationPosition;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.focus.StarOccurenceListener;
import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;
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
			public void imageRemoved(Image image, MosaicImageParameter mip) {
				
			}
			
			@Override
			public void imageAdded(Image image, MosaicListener.ImageAddedCause cause) {
				
			}

			@Override
			public void pointOfInterestAdded(PointOfInterest poi) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void pointOfInterestRemoved(PointOfInterest poi) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void exclusionZoneAdded(ExclusionZone ze) {
			}

			@Override
			public void exclusionZoneRemoved(ExclusionZone ze) {
			}
		});
		
		refreshLabels();
	}
	
	private void refreshLabels()
	{
		if (this.so != null && this.so.isAnalyseDone() && this.so.isStarFound()) {
			this.lblFWHM.setText(Utils.doubleToString(this.so.getFwhm(), 2) 
					+ " (" + Utils.doubleToString(this.so.getMinFwhm(),  2)+" < " + Utils.doubleToString(this.so.getMaxFwhm(),  2) + ") ratio: " + Utils.doubleToString(this.so.getMinFwhm() / this.so.getFwhm(), 2));
			this.lblStdDev.setText(Utils.doubleToString(this.so.getStddev(), 2)
					+ " (" + Utils.doubleToString(this.so.getMinStddev(),  2)+" < " + Utils.doubleToString(this.so.getMaxStddev(),  2) + ")");
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
			this.lblAduMaxList[3].setText(Integer.toString(aduMax) + (this.so.isSaturationDetected() ? " (sat)" : ""));
			this.lblAduSumList[3].setText(Integer.toString(aduSum));
			this.lblBlackList[3].setText("");

			if (this.so != null && this.so.getStar().getPositionStatus() == StarCorrelationPosition.Reference)
			{
				double aduExpected = Math.pow(2.512, -this.so.getStar().getMagnitude());
				double ratio = aduSum / aduExpected;
				System.out.println("ratio = " + ratio);
			}
			

			this.lblPosXImage.setText(Utils.doubleToString(so.getPicX(), 2));
			this.lblPosYImage.setText(Utils.doubleToString(so.getPicY(), 2));

		} else {
			this.lblFWHM.setText("");
			this.lblStdDev.setText("");

			this.lblPosXImage.setText("");
			this.lblPosYImage.setText("");
			
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
		
		if (this.so != null && this.so.getStar().getPositionStatus() == StarCorrelationPosition.Reference)
		{
			this.lblIdent.setText(this.so.getStar().getReference()+ " mag=" + Utils.doubleToString(this.so.getStar().getMagnitude(), 3));
			
		} else {
			this.lblIdent.setText("");
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
