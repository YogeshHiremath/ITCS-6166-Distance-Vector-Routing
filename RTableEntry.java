public class RTableEntry implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String source;
	private String dest;
	private String nextHop;
	private double cost;
	
	public RTableEntry(String source, String dest, String nextHop, Double cost) {
		this.source = source;
		this.dest = dest;
		this.nextHop = nextHop;
		this.cost = cost;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDest() {
		return dest;
	}

	public void setDest(String dest) {
		this.dest = dest;
	}

	public String getNextHop() {
		return nextHop;
	}

	public void setNextHop(String nextHop) {
		this.nextHop = nextHop;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

}
