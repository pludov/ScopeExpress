package fr.pludov.scopeexpress.ui;

public class TaskFieldStatus<TYPE> {
	public enum Status {
		Visible,
		MeaningLess,	// Hidden, sans valeur
		Forced
	};
	
	public final Status status;
	public final TYPE forcedValue;
	
	public TaskFieldStatus(Status value)
	{
		this.status = value;
		this.forcedValue = null;
	}

	public TaskFieldStatus(Status value, TYPE forcedValue)
	{
		this.status = value;
		this.forcedValue = forcedValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((forcedValue == null) ? 0 : forcedValue.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaskFieldStatus other = (TaskFieldStatus) obj;
		if (forcedValue == null) {
			if (other.forcedValue != null)
				return false;
		} else if (!forcedValue.equals(other.forcedValue))
			return false;
		if (status != other.status)
			return false;
		return true;
	}

	public boolean isVisible() {
		return status == Status.Visible;
	}
	
	public boolean isEditable() {
		return status == Status.Visible;
	}
}
