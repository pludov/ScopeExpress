package fr.pludov.scopeexpress.ui.settings;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.pludov.io.CameraFrame;
import fr.pludov.scopeexpress.ImageDisplayParameter;
import fr.pludov.scopeexpress.ImageDisplayParameterListener;
import fr.pludov.scopeexpress.ImageDisplayParameter.ChannelMode;
import fr.pludov.scopeexpress.focus.Application;
import fr.pludov.scopeexpress.focus.Histogram;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.ui.ActionOpen;
import fr.pludov.scopeexpress.ui.preferences.BooleanConfigItem;
import fr.pludov.scopeexpress.ui.preferences.EnumConfigItem;
import fr.pludov.scopeexpress.ui.preferences.StringConfigItem;
import fr.pludov.scopeexpress.ui.settings.HistogramDisplay.Channel;
import fr.pludov.scopeexpress.ui.settings.InputOutputHandler.Converter;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask.BackgroundTaskCanceledException;
import fr.pludov.scopeexpress.ui.utils.BackgroundTaskQueue;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask.Status;
import fr.pludov.scopeexpress.ui.widgets.IconButton;
import fr.pludov.scopeexpress.ui.widgets.ToolbarButton;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class ImageDisplayParameterPanel extends ImageDisplayParameterPanelDesign implements ImageDisplayParameterListener {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	InputOutputHandler<ImageDisplayParameter> ioHandler;
	
	public static final EnumConfigItem<ChannelMode> lastChannelMode = new EnumConfigItem<ChannelMode>(ImageDisplayParameterPanel.class, "lastChannelMode", ChannelMode.class, ChannelMode.Color);
	public static final BooleanConfigItem lastAutoMode = new BooleanConfigItem(ImageDisplayParameterPanel.class, "lastAutoMode", false);

	final Application application;
	ImageDisplayParameter parameter;
	
	boolean isDefaultViewParameter;
	
	final LevelDisplayPosition position;
	final HistogramDisplay histogramDisplay;
	Image imageForHistogram;
	IconButton zoomIn, zoomOut, zoomFit;
	
	public ImageDisplayParameterPanel(Application application, boolean isDefaultViewParameter) {
		this.application = application;
		this.isDefaultViewParameter = isDefaultViewParameter;
		
		ioHandler = new InputOutputHandler<ImageDisplayParameter>();
		ioHandler.init(getConverters());
		
		position = new LevelDisplayPosition();
		
		position.listeners.addListener(this.listenerOwner, new LevelDisplayPositionListener() {
			
			@Override
			public void positionChanged() {
				updateScrollbar();
			}
		});
		this.scrollBar.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent arg0) {
				if (isScrollbarAdjusting) return;
				int val = scrollBar.getValue();
				position.setOffset(val);
			}
		});
		updateScrollbar();
		
		zoomIn = new IconButton("zoom-in");
		zoomIn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				position.zoomAt(-1, 0.5);
			}
		});
		this.panel.add(zoomIn, "cell 1 1,alignx center");
		zoomOut = new IconButton("zoom-out");
		zoomOut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				position.zoomAt(1, 0.5);
			}
		});
		this.panel.add(zoomOut, "cell 2 1,alignx center");
		zoomFit = new IconButton("zoom-fit-best");
		zoomFit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				position.setPosition(65536, 0);
			}
		});
		this.panel.add(zoomFit, "cell 3 1,alignx center");
		
		histogramDisplay = new HistogramDisplay(position);
		this.histoPanel.add(histogramDisplay);
	}

	boolean isScrollbarAdjusting = false;
	void updateScrollbar()
	{
		isScrollbarAdjusting = true;
		this.scrollBar.setMinimum(0);
		this.scrollBar.setMaximum(65536 - position.getZoom());
		this.scrollBar.setEnabled(position.getZoom() != 65536);
		this.scrollBar.setValue(position.getOffset());
		isScrollbarAdjusting = false;
	}
	
	Converter<ImageDisplayParameter> [] getConverters()
	{
		return new Converter[] {
				new InputOutputHandler.EnumConverter<ImageDisplayParameter, ChannelMode>(this.channelModeCombo, ChannelMode.values(), this.isDefaultViewParameter ? lastChannelMode : null) {
					@Override
					ChannelMode getFromParameter(ImageDisplayParameter parameters) {
						return parameters.getChannelMode();
					}
					
					@Override
					void setParameter(ImageDisplayParameter parameters, ChannelMode content) throws Exception {
						if (content == null) throw new Exception("Obligatoire!");
						parameters.setChannelMode(content);
					}
				},
				new InputOutputHandler.BooleanConverter<ImageDisplayParameter>(this.autoHistogramCheckBox, isDefaultViewParameter ? lastAutoMode : null) {
					@Override
					public Boolean getFromParameter(ImageDisplayParameter parameters) {
						return parameters.isAutoHistogram();
					}
					
					@Override
					public void setParameter(ImageDisplayParameter parameters, Boolean content) throws Exception {
						if (content == null) throw new Exception("Obligatoire!");
						parameters.setAutoHistogram(content);
					}
				},

				new InputOutputHandler.StringConverter<ImageDisplayParameter>(this.darkTextBox) {
					@Override
					public String getFromParameter(ImageDisplayParameter parameters) {
						return parameters.getDarkFrame() != null ? parameters.getDarkFrame().getPath().getName() : "";
					}
					
					@Override
					public void setParameter(ImageDisplayParameter parameters, String content) throws Exception {
						throw new Exception("cannot set");
						
					}
				},
				new InputOutputHandler.BooleanConverter<ImageDisplayParameter>(this.darkEnabledCheckbox) {
					@Override
					public Boolean getFromParameter(ImageDisplayParameter parameters) {
						return parameters.isDarkEnabled();
					}
					
					@Override
					public void setParameter(ImageDisplayParameter parameters, Boolean content) throws Exception {
						if (content == null) {
							throw new Exception("Obligatoire !");
						}
						ImageDisplayParameter tmp = new ImageDisplayParameter(parameters);
						tmp.setDarkEnabled(content);
						if (!content) {
							tmp.setDarkFrame(null);
						}
						parameters.copyFrom(tmp);
					}
				},
		};
	}
	
	public void loadParameters(ImageDisplayParameter parameter)
	{
		if (this.parameter == parameter) return;
		if (this.parameter != null) {
			this.parameter.listeners.removeListener(this.listenerOwner);
		}
		this.parameter = parameter;
		parameter.listeners.addListener(this.listenerOwner, this);
		ioHandler.loadWithConfigParameters(parameter);
		updateLevelDisplays();
		updateHistogram();
		updateHistogramCurves();
	}
	
	final List<LevelDisplayWithControl> levelDisplays = new ArrayList<>();
	private void updateLevelDisplays()
	{
		
		for(LevelDisplayWithControl ld: levelDisplays) {
			ld.setImageDisplayParameter(null);
			this.levelPanel.remove(ld);
		}
		levelDisplays.clear();
		
		if (this.parameter != null) {
			// Assurer qu'on ait un levelDisplay par canal
			switch(this.parameter.getChannelMode()) {
			case Color:
				levelDisplays.add(new LevelDisplayWithControl(position, 0, Color.red));
				levelDisplays.add(new LevelDisplayWithControl(position, 1, Color.green));
				levelDisplays.add(new LevelDisplayWithControl(position, 2, Color.blue));
				break;
			case GreyScale:
			case NarrowBlue:
			case NarrowGreen:
			case NarrowRed:
				levelDisplays.add(new LevelDisplayWithControl(position, 0, this.parameter.getChannelMode().displayColor));
				break;
			}
			int id = 0;
			for(LevelDisplayWithControl ld : levelDisplays) {
				ld.setImageDisplayParameter(parameter);
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = id++;
				c.fill = GridBagConstraints.BOTH;
				c.weightx = 1;
				c.weighty = 1;
				this.levelPanel.add(ld, c);
			}
			this.levelPanel.layout();
			this.levelPanel.revalidate();
		}
	}
	

	@Override
	public void parameterChanged(ImageDisplayParameter previous, ImageDisplayParameter current) {
		if (previous.getChannelMode() != current.getChannelMode()) {
			updateLevelDisplays();
			updateHistogram();
		} else {
			if (current.isDarkEnabled() != previous.isDarkEnabled() || !Objects.equals(current.getDarkFrame(), previous.getDarkFrame())) {
				updateHistogram();
			} else {
				updateHistogramCurves();
			}
		}
		ioHandler.loadParameters(current);
	}

	public void updateHistogramCurves()
	{

		switch(this.parameter.getChannelMode()) {
		case Color:
			if (imageForHistogram == null || imageForHistogram.isCfa()) {
				histogramDisplay.setCurve(Channel.Red, parameter.getAduLevelMapper(0));
				histogramDisplay.setCurve(Channel.Green, parameter.getAduLevelMapper(1));
				histogramDisplay.setCurve(Channel.Blue, parameter.getAduLevelMapper(2));
			} else {
				histogramDisplay.setCurve(Channel.Grey, parameter.getAduLevelMapper(0));
				histogramDisplay.setCurve(Channel.Grey, parameter.getAduLevelMapper(1));
				histogramDisplay.setCurve(Channel.Grey, parameter.getAduLevelMapper(2));
			}

			break;
		case GreyScale:
			histogramDisplay.setCurve(Channel.Grey, parameter.getAduLevelMapper(0));
			break;
		case NarrowBlue:
			histogramDisplay.setCurve(Channel.Blue, parameter.getAduLevelMapper(0));
			break;
		case NarrowGreen:
			histogramDisplay.setCurve(Channel.Green, parameter.getAduLevelMapper(0));
			break;
		case NarrowRed:
			histogramDisplay.setCurve(Channel.Red, parameter.getAduLevelMapper(0));
			break;
		}
	}
	
	BackgroundTask updateHistogramTask;

	static private EnumMap<Channel, Histogram> getHistograms(BackgroundTask thisTask, Image imageForHistogram, Image darkFrame, ChannelMode displayMode, boolean nullIfNotReady) throws BackgroundTaskCanceledException {
		EnumMap<Channel, Histogram> result;
		result = new EnumMap<>(Channel.class);
		switch(displayMode) {
		case Color:
			if (imageForHistogram.isCfa()) {
				result.put(Channel.Red, imageForHistogram.getHistogram(darkFrame, fr.pludov.utils.ChannelMode.Red, nullIfNotReady));
				if (thisTask != null) thisTask.checkInterrupted();
				result.put(Channel.Green, imageForHistogram.getHistogram(darkFrame, fr.pludov.utils.ChannelMode.Green, nullIfNotReady));
				if (thisTask != null) thisTask.checkInterrupted();
				result.put(Channel.Blue, imageForHistogram.getHistogram(darkFrame, fr.pludov.utils.ChannelMode.Blue, nullIfNotReady));
			} else {
				Histogram greyHisto = imageForHistogram.getHistogram(darkFrame, fr.pludov.utils.ChannelMode.Bayer, nullIfNotReady);
				result.put(Channel.Red, greyHisto);
				result.put(Channel.Green, greyHisto);
				result.put(Channel.Blue, greyHisto);
			}

			break;
		case GreyScale:
			result.put(Channel.Grey, imageForHistogram.getHistogram(darkFrame, fr.pludov.utils.ChannelMode.Red, nullIfNotReady));
			break;
		case NarrowBlue:
			result.put(Channel.Blue, imageForHistogram.getHistogram(darkFrame, imageForHistogram.isCfa() ? fr.pludov.utils.ChannelMode.Blue : fr.pludov.utils.ChannelMode.Bayer, nullIfNotReady));
			break;
		case NarrowGreen:
			result.put(Channel.Green, imageForHistogram.getHistogram(darkFrame, imageForHistogram.isCfa() ? fr.pludov.utils.ChannelMode.Green : fr.pludov.utils.ChannelMode.Bayer, nullIfNotReady));
			break;
		case NarrowRed:
			result.put(Channel.Red, imageForHistogram.getHistogram(darkFrame, imageForHistogram.isCfa() ? fr.pludov.utils.ChannelMode.Red : fr.pludov.utils.ChannelMode.Bayer, nullIfNotReady));
			break;
		}
		return result;
	}
	
	private void setHistograms(EnumMap<Channel, Histogram> result) {
		for(Map.Entry<Channel, Histogram> entry: result.entrySet())
		{
			histogramDisplay.setHistogram(entry.getKey(), entry.getValue());
		}
	}
	
	public void updateHistogram()
	{
		histogramDisplay.clear();
		if (updateHistogramTask != null) {
			updateHistogramTask.abort();
			updateHistogramTask = null;
		}
		updateHistogramCurves();
		
		if (imageForHistogram == null) return;
		
		EnumMap<Channel, Histogram> histograms;
		try {
			histograms = getHistograms(null, imageForHistogram, parameter.getDarkFrame(), parameter.getChannelMode(), true);
		} catch (BackgroundTaskCanceledException e) {
			// Impossible
			throw new RuntimeException("Erreur interne");
		}
		setHistograms(histograms);
		
		if (histograms.containsValue(null)) {
			
			updateHistogramTask = new BackgroundTask("Computing histogram for " + imageForHistogram.getPath().getName()) {
				Image imageForHistogram = ImageDisplayParameterPanel.this.imageForHistogram;
				Image darkFrame = ImageDisplayParameterPanel.this.parameter.getDarkFrame();
				ChannelMode displayMode = ImageDisplayParameterPanel.this.parameter.getChannelMode();
				
				EnumMap<Channel, Histogram> result;
				
				@Override
				public int getResourceOpportunity() {
					return 0;
				}
				
				@Override
				protected void proceed() throws BackgroundTaskCanceledException, Throwable {
					checkInterrupted();
					EnumMap<Channel, Histogram> result = getHistograms(this, imageForHistogram, darkFrame, displayMode, false);
					this.result = result;
				}
	
				@Override
				protected void onDone() {
					if (getStatus() == Status.Done && updateHistogramTask == this) {
						updateHistogramTask = null;
						setHistograms(result);
					}
				}
			};
			application.getBackgroundTaskQueue().addTask(updateHistogramTask);
		}

//		
//		switch(this.parameter.getChannelMode()) {
//		case Color:
//			if (imageForHistogram.isCfa()) {
//				histogramDisplay.setHistogram(Channel.Red, imageForHistogram.getHistogram(this.parameter.getDarkFrame(), fr.pludov.utils.ChannelMode.Red), parameter.getAduLevelMapper(0));
//				histogramDisplay.setHistogram(Channel.Green, imageForHistogram.getHistogram(this.parameter.getDarkFrame(), fr.pludov.utils.ChannelMode.Green), parameter.getAduLevelMapper(1));
//				histogramDisplay.setHistogram(Channel.Blue, imageForHistogram.getHistogram(this.parameter.getDarkFrame(), fr.pludov.utils.ChannelMode.Blue), parameter.getAduLevelMapper(2));
//			} else {
//				histogramDisplay.setHistogram(Channel.Grey, imageForHistogram.getHistogram(this.parameter.getDarkFrame(), fr.pludov.utils.ChannelMode.Bayer), parameter.getAduLevelMapper(0));
//				histogramDisplay.setHistogram(Channel.Grey, imageForHistogram.getHistogram(this.parameter.getDarkFrame(), fr.pludov.utils.ChannelMode.Bayer), parameter.getAduLevelMapper(1));
//				histogramDisplay.setHistogram(Channel.Grey, imageForHistogram.getHistogram(this.parameter.getDarkFrame(), fr.pludov.utils.ChannelMode.Bayer), parameter.getAduLevelMapper(2));
//			}
//
//			break;
//		case GreyScale:
//			histogramDisplay.setHistogram(Channel.Grey, imageForHistogram.getHistogram(this.parameter.getDarkFrame(), fr.pludov.utils.ChannelMode.Red), parameter.getAduLevelMapper(0));
//			break;
//		case NarrowBlue:
//			histogramDisplay.setHistogram(Channel.Blue, imageForHistogram.getHistogram(this.parameter.getDarkFrame(), imageForHistogram.isCfa() ? fr.pludov.utils.ChannelMode.Blue : fr.pludov.utils.ChannelMode.Bayer), parameter.getAduLevelMapper(0));
//			break;
//		case NarrowGreen:
//			histogramDisplay.setHistogram(Channel.Green, imageForHistogram.getHistogram(this.parameter.getDarkFrame(), imageForHistogram.isCfa() ? fr.pludov.utils.ChannelMode.Green : fr.pludov.utils.ChannelMode.Bayer), parameter.getAduLevelMapper(0));
//			break;
//		case NarrowRed:
//			histogramDisplay.setHistogram(Channel.Red, imageForHistogram.getHistogram(this.parameter.getDarkFrame(), imageForHistogram.isCfa() ? fr.pludov.utils.ChannelMode.Red : fr.pludov.utils.ChannelMode.Bayer), parameter.getAduLevelMapper(0));
//			break;
//		}
	}
	
	public void setImage(Image image) {
		if (this.imageForHistogram == image) return;
		this.imageForHistogram = image;
		updateHistogram();
		updateHistogramCurves();
	}
	
}
