package no.ntnu.falldetection.utils.motejx.extensions.motionplus;

import java.util.ArrayList;

public class MotionPlusCalibrate {
	private boolean isFinished;
	
	private final int CALIBRATION_LENGTH = 20;
	private int dataLeft;
	
	private ArrayList<Float> yaw;
	private ArrayList<Float> roll;
	private ArrayList<Float> pitch;
	
	public MotionPlusCalibrate(){
		dataLeft = CALIBRATION_LENGTH;
		isFinished = true;
		yaw = new ArrayList<Float>();
		roll = new ArrayList<Float>();
		pitch = new ArrayList<Float>();
	}
	
	public void addCalibrationData(float yaw, float roll, float pitch){
		this.yaw.add(yaw);
		this.roll.add(roll);
		this.pitch.add(pitch);
		if(dataLeft-- == 0){
			isFinished = true;
		}
	}
	
	public MotionPlusCalibrationData getCalibratedData(){
		MotionPlusCalibrationData calibrationData = new MotionPlusCalibrationData();
		
		float yaw0 = 0, roll0 =0, pitch0 = 0;
		
		for(int i = 0; i < yaw.size(); i++){
			yaw0 += yaw.get(i);
			roll0 += roll.get(i);
			pitch0 += pitch.get(i);
		}
		
		yaw0 /= yaw.size();
		roll0 /= roll.size();
		pitch0 /= pitch.size();
		
		calibrationData.setYaw0(yaw0);
		calibrationData.setRoll0(roll0);
		calibrationData.setPitch0(pitch0);
		
		return calibrationData;
	}
	
	public boolean isFinished(){
		return isFinished;
	}
	
	public void startCalibrating(){
		isFinished = false;
		dataLeft = CALIBRATION_LENGTH;
	}
	
}
