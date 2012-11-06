package no.ntnu.falldetection.activities;

import no.ntnu.falldetection.models.OrientationEvent;
import no.ntnu.falldetection.models.OrientationListener;
import no.ntnu.falldetection.models.OrientationModel;
import android.util.Log;

public class ThresholdAlarm implements OrientationListener{
	
	public ThresholdAlarm(OrientationModel model){
		model.addOrientationListener(this);
	}
	
	@Override
	public void orientationChanged(OrientationEvent evt) {
		double a = Math.toRadians(evt.getPitch());
		double b = Math.toRadians(evt.getRoll());
		
		a = Math.cos(a);
		b = Math.cos(b);
		
		if(a < 0.8 || b < 0.8){
			Log.w("Alarm", "Alarm, alarm!");
		}
	}

}
