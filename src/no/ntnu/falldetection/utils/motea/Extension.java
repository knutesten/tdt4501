package no.ntnu.falldetection.utils.motea;

public interface Extension {
	
	public Object parseExtensionData(byte[] extensionData);
	
	public void setMote(Mote mote);
	
	public void initialize();
	
}
