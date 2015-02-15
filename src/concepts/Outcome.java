package concepts;

public class Outcome {
	private int attackingArmies;
	private int defendingArmies;
	
	public Outcome(int attackingArmies, int defendingArmies) {
		super();
		this.attackingArmies = attackingArmies;
		this.defendingArmies = defendingArmies;
	}
	public int getAttackingArmies() {
		return attackingArmies;
	}
	public void setAttackingArmies(int attackingArmies) {
		this.attackingArmies = attackingArmies;
	}
	public int getDefendingArmies() {
		return defendingArmies;
	}
	public void setDefendingArmies(int defendingArmies) {
		this.defendingArmies = defendingArmies;
	}

	
}
