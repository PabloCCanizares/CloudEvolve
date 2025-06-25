package configuration;

public class EAConfig {

	private static EAConfig config;
	LogLevel eLogLevel;
	int nTotalTimeout;
	int nTotalIterations;

	private EAConfig()
	{
		reset();
	}
	public static EAConfig getInstance() {
		if (config == null){
			config = new EAConfig();
		}        
		return config;
	}

	private void reset()
	{
		eLogLevel= LogLevel.eNORMAL;
		nTotalTimeout = nTotalIterations = 0;
	}

	/**
	 * @return the eLogLevel
	 */
	 public LogLevel getLogLevel() {
		 return eLogLevel;
	 }
	 /**
	  * @param eLogLevel the eLogLevel to set
	  */
	 public void seteLogLevel(LogLevel eLogLevel) {
		 this.eLogLevel = eLogLevel;
	 }
	 /**
	  * @return the nTotalTimeout
	  */
	 public int getTotalTimeout() {
		 return nTotalTimeout;
	 }
	 /**
	  * @param nTotalTimeout the nTotalTimeout to set
	  */
	 public void setTotalTimeout(int nTotalTimeout) {
		 this.nTotalTimeout = nTotalTimeout;
	 }
	 /**
	  * @return the nTotalIterations
	  */
	 public int getTotalIterations() {
		 return nTotalIterations;
	 }
	 /**
	  * @param nTotalIterations the nTotalIterations to set
	  */
	 public void setTotalIterations(int nTotalIterations) {
		 this.nTotalIterations = nTotalIterations;
	 }

}
