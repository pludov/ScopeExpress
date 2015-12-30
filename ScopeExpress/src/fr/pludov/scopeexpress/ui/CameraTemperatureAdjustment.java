package fr.pludov.scopeexpress.ui;

import java.text.*;

import fr.pludov.scopeexpress.ui.utils.*;

public class CameraTemperatureAdjustment extends CameraTemperatureAdjustmentDesign {

	double targetTemperature;
	double temperatureStep;
	int timeout;
	
	public CameraTemperatureAdjustment() {
		Utils.addTextFieldChangeListener(this.textFieldTemperature, new Runnable() {
			@Override
			public void run() {
				targetTemperature = getTargetTemperature();
				textFieldTemperature.setText(CameraControlPanel.tempFormat.format(targetTemperature));
			}
		});
		
		Utils.addTextFieldChangeListener(this.textFieldTemperatureStep, new Runnable() {
			@Override
			public void run() {
				temperatureStep = getTemperatureStep();
				textFieldTemperatureStep.setText(CameraControlPanel.tempFormat.format(temperatureStep));
			}
		});
		
		Utils.addTextFieldChangeListener(this.textFieldTemperatureStep, new Runnable() {
			@Override
			public void run() {
				timeout = getTimeout();
				textFieldTimeout.setText(Integer.toString(timeout));
			}
		});
	}
	
	
	public void setValue(double targetTemperature, double temperatureStep, int timeout)
	{
		this.targetTemperature = targetTemperature;
		this.temperatureStep = temperatureStep;
		this.timeout = timeout;
		this.textFieldTemperature.setText(CameraControlPanel.tempFormat.format(targetTemperature));
		this.textFieldTemperatureStep.setText(CameraControlPanel.tempFormat.format(temperatureStep));
		this.textFieldTimeout.setText(Integer.toString(timeout));
	}
	
	public double getTargetTemperature()
	{
		try {
			return CameraControlPanel.tempParseFormat.parse(this.textFieldTemperature.getText()).doubleValue();
		} catch (ParseException e) {
			e.printStackTrace();
			return targetTemperature;
		}
	}
	
	public double getTemperatureStep()
	{
		try {
			double step = CameraControlPanel.tempParseFormat.parse(this.textFieldTemperatureStep.getText())
					.doubleValue();
			if (step < 0.1) {
				step = 0.1;
			}
			return step;
		} catch (ParseException e) {
			e.printStackTrace();
			return temperatureStep;
		}
	}
	
	public int getTimeout()
	{
		try {
			int timeout = Integer.parseInt(this.textFieldTimeout.getText());
			if (timeout < 1) {
				timeout = 1;
			}
			return timeout;
		} catch(NumberFormatException e) {
			e.printStackTrace();
			return timeout;
		}
	}

}
