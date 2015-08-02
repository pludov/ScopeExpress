package fr.pludov.scopeexpress.ui;

public class AutoFocusParameters {
	
	public static final int maxPassCount = 3;
	
	int startPosition;
	
	int photoDuration;
	int photoCount;
	
	int backlash;
	
	boolean [] passEnabled;
	int [] passWidth;
	int [] passStepCount;
	
	public AutoFocusParameters() {
		
		backlash = 0;
		startPosition = 0;
		photoDuration = 1;
		photoCount = 1;

		passEnabled = new boolean[maxPassCount];
		passWidth = new int[maxPassCount];
		passStepCount = new int[maxPassCount];

		for(int pass = 0; pass < maxPassCount; ++pass)
		{
			passEnabled[pass] = true;
			passWidth[pass] = 2048 >> (3 * pass);
			passStepCount[pass] = 10;
		}
	}


	public int getPassPos(int passCenter, int passId, int stepId) {
		int width = passWidth[passId];
		
		return (int)(passCenter - (width / 2) + (width * (long)stepId) / (passStepCount[passId] - 1));
	}



	public int getStepCount(int passId) {
		return passStepCount[passId];
	}



	public int getNextPass(int passId) {
		for(int i = passId + 1; i < maxPassCount; ++i)
		{
			if (passEnabled[i]) return i;
		}
		return -1;
	}

	public boolean isPassEnabled(int passId) {
		return passEnabled[passId];
	}
	
	public void setPassEnabled(int passId, boolean value) {
		passEnabled[passId] = value;
	}


	public int getPassWidth(int passId) {
		return passWidth[passId];
	}


	public void setPassWidth(int passId, int content) {
		passWidth[passId] = content;
	}


	public int getPassStepCount(int passId) {
		return passStepCount[passId];
	}


	public void setPassStepCount(int passId, int content) {
		passStepCount[passId] = content;
	}
}
