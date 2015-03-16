package concepts;

public class FromTo {
	private Integer r1;
	private Integer r2;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof FromTo))
			return false;
		FromTo key = (FromTo) o;
		return r1.equals(key.getR1()) && r2.equals(key.getR2());
	}

	@Override
	public int hashCode() {
		int result = r1;
		result = 31 * result + r2;
		return result;
	}

	public Integer getR1() {
		return r1;
	}

	public void setR1(Integer r1) {
		this.r1 = r1;
	}

	public Integer getR2() {
		return r2;
	}

	public void setR2(Integer r2) {
		this.r2 = r2;
	}

	public FromTo(Integer r1, Integer r2) {
		super();
		this.r1 = r1;
		this.r2 = r2;
	}

}
