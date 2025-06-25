package configuration;

public enum LogLevel {

	eVERBOSE(4), eLOG(3), eNORMAL(2), eCRITICAL(1);
	
	
	private int nLogLevel;
	
	LogLevel(int nLogLevel)
	{
		this.nLogLevel = nLogLevel;
	}

	public int getValue() {

		return nLogLevel;
	}
}
