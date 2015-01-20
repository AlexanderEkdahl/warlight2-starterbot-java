package concepts;

import map.Region;

public class FromTo {
	private Region r1;
	private Region r2;

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
		int result = r1.getId();
		result = 31 * result + r2.getId();
		return result;
	}

	public Region getR1() {
		return r1;
	}

	public void setR1(Region r1) {
		this.r1 = r1;
	}

	public Region getR2() {
		return r2;
	}

	public void setR2(Region r2) {
		this.r2 = r2;
	}

	public FromTo(Region r1, Region r2) {
		super();
		this.r1 = r1;
		this.r2 = r2;
	}

}
