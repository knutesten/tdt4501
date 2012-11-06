package no.ntnu.falldetection.activities;

import java.io.IOException;

import no.ntnu.falldetection.models.AlarmEvent;
import no.ntnu.falldetection.models.AlarmListener;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

public class AlarmSound extends Thread implements AlarmListener{
	private MediaPlayer mediaPlayer;
	private boolean paused =true;
	private float severity = 0;
	private final long DELAY = 1000l;
	
	public AlarmSound(Context context) {
		AssetFileDescriptor afd;
		mediaPlayer = new MediaPlayer();

		// Set the alarm volume to match the phone alarm volume.
		AudioManager amanager = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		int maxVolume = amanager
				.getStreamMaxVolume(AudioManager.STREAM_ALARM);
		amanager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);

		mediaPlayer.setLooping(false);
		try {
			afd = context.getAssets().openFd("beep.wav");
			mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
					afd.getDeclaredLength());
			mediaPlayer.prepare();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		start();
	}	

	public void run(){
		for(;;){
			try{
				Thread.sleep((long)(DELAY*(1-severity)));
				if(!paused){
					mediaPlayer.start();
				}
			}catch(InterruptedException e){
			}
		}
	}

	@Override
	public void alarmOn(AlarmEvent evt) {
		paused = false;
		severity = evt.getSeverity();
	}
	
	@Override
	public void alarmOff(){
		paused = true;
	}
}