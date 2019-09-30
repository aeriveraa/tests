package mx.com.nokia.testing;

public class Resolution {
	String rr;
	String mwf;
	public Resolution(String rKey, String file) {
		this.mwf=file;
		this.rr=rKey;
	}

	public Resolution() {
	}

	public String getRr() {
		return rr;
	}
	public void setRr(String rr) {
		this.rr = rr;
	}
	public String getMwf() {
		return mwf;
	}
	public void setMwf(String mwf) {
		this.mwf = mwf;
	}
	
	public String toString() {
		return "rr: " + this.getRr() + "| mwf : " + this.getMwf();
	}

}
