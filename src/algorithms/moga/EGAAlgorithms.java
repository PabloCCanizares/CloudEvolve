package algorithms.moga;

public enum EGAAlgorithms {
	eMOGA, eVEGA, eVEGA2, eSPEA2, eSPEA3, ePAES, ePAES2, eNSGAII, eNSGAII2;

	@Override
	public String toString() {
		switch (this) {
		case eMOGA:
			return "MOGA";
		case eVEGA:
			return "VEGA";
		case eVEGA2:
			return "VEGA2";			
		case eSPEA2:
			return "SPEA2";
		case eSPEA3:
			return "SPEA3";
		case ePAES:
			return "PAES";
		case ePAES2:
			return "PAES2";			
		case eNSGAII:
			return "NSGAII";
		case eNSGAII2:
			return "NSGAII2";
		}
		return "";
	}
}
