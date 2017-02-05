package fr.pludov.scopeexpress.ui;

import java.util.*;
import java.util.function.*;

import javax.swing.*;

import fr.pludov.scopeexpress.*;
import fr.pludov.scopeexpress.OrientationModel.*;
import fr.pludov.scopeexpress.ui.utils.*;
import fr.pludov.scopeexpress.ui.widgets.*;
import fr.pludov.scopeexpress.utils.*;
import net.miginfocom.swing.*;

public class OrientationModelPanel extends OrientationModelPanelDesign {
	private final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	final OrientationModel model;
	private final List<OrientationModelListener> modelCallback = new ArrayList<>();
	
	public OrientationModelPanel(OrientationModel model) {
		this.model = model;
		
		this.model.listeners.addListener(this.listenerOwner, () -> {
			for(OrientationModelListener oml : modelCallback) {
				oml.onChange();
			}
		});
		addDisplayer(Field.Target, (double[] value)-> {
			this.lblRa.setText(
					value == null ?
							"N/A":
							Utils.formatHourMinSec(value[0]));
			this.lblDec.setText(
					value == null ?
							"N/A":
							Utils.formatDegMinSec(value[1]));
			
		});
		

		bindSelector(OrientationModel.Field.Angle, this.comboAngle);
		addClearBton(lblAngle, OrientationModel.Field.Angle);
		addDisplayer(Field.Angle, (Double value) -> {
			this.lblAngle.setText(
						value == null
							? "N/A"
							: String.format(Locale.US, "%5.2f°", value.doubleValue()));
		});
		
		bindSelector(OrientationModel.Field.Ech, this.comboPixSize);
		addClearBton(lblEch, OrientationModel.Field.Ech);
		addDisplayer(Field.Ech, (double [] value) -> {
			this.lblEch.setText(
					value == null 
						? "N/A"
						: String.format(Locale.US, "%4.2f\" x %4.2f\"", value[0], value[1]));
		});

		
		bindSelector(OrientationModel.Field.PixSize, this.comboPixSize);
		addClearBton(lblPixsize, OrientationModel.Field.PixSize);
		addDisplayer(Field.PixSize, (double [] value) -> {
			this.lblPixsize.setText(
					value == null 
						? "N/A"
						: String.format(Locale.US, "%4.2f x %4.2f", value[0], value[1]));
		});

		bindSelector(OrientationModel.Field.Focale, this.comboFocale);
		addClearBton(lblFocale, OrientationModel.Field.Focale);
		addDisplayer(Field.Focale, (Double value) -> {
			this.lblFocale.setText(
					value == null 
						? "N/A"
						: String.format(Locale.US, "%4.1f", value));
		});


		bindSelector(OrientationModel.Field.MountDelta, this.comboDelta);
		addClearBton(lblShiftRa, OrientationModel.Field.MountDelta);
		addDisplayer(Field.MountDelta, (double[] value)-> {
			this.lblShiftRa.setText(
					value == null ?
							"N/A":
							Utils.formatHourMinSec(value[0]));
			this.lblShiftDec.setText(
					value == null ?
							"N/A":
							Utils.formatDegMinSec(value[1]));
			
		});

	}
	
	
	private void bindSelector(Field field, JComboBox<Origin> origin)
	{
		Utils.setComboBoxValues(origin, field.getPossibleOrigins());
		
		OrientationModelListener updateDisplay = () -> {
			origin.setSelectedItem(model.getOrigin(field));
		};
		Utils.addComboChangeListener(origin, () -> {
			Origin o = (Origin) origin.getSelectedItem();
			model.setOrigin(field, o);
		});
		this.modelCallback.add(updateDisplay);
		updateDisplay.onChange();

	}
	
	private <D> void addDisplayer(Field field, Consumer<D> consumer)
	{
		OrientationModelListener updateDisplay = () -> {
			consumer.accept((D)model.getValue(field));
		};
		this.modelCallback.add(updateDisplay);
		updateDisplay.onChange();
	}
	
	private IconButton addClearBton(JLabel where, Field field)
	{
		IconButton result;
		
		Object constraint = ((MigLayout)getLayout()).getConstraintMap().get(where);
		add(result = new IconButton("view-remove", false), constraint);
		
		result.addActionListener((e) -> {
			model.clear(field);
		});
		OrientationModelListener updateBtonStatus = () -> {
			boolean enabled = model.getOrigin(field).storeValue && model.getValue(field) != null; 
			result.setEnabled(enabled);
		};
		this.modelCallback.add( updateBtonStatus);
		updateBtonStatus.onChange();
		
		return result;
		
	}

}
