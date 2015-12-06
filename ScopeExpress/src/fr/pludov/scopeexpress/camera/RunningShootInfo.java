package fr.pludov.scopeexpress.camera;

public class RunningShootInfo extends ShootParameters {
	long startTime;
	

	public RunningShootInfo(ShootParameters parameters) {
		super(parameters);
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

}
