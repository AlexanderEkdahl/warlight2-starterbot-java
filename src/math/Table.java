package math;

import java.util.HashMap;

public class Table {
	private static Table table;
	private static HashMap<Double, HashMap<Double, Double>> power;

	public static Table getInstance() {
		power = new HashMap<Double, HashMap<Double, Double>>();
		if (table == null) {
			table = new Table();
		}
		return table;

	};

	public Double getPower(Double d1, Double d2) {
		if (power.get(d1) == null) {
			power.put(d1, new HashMap<Double, Double>());
		}
		if (power.get(d1).get(d2) == null) {
			power.get(d1).put(d2, Math.pow(d1, d2));
		}

		return power.get(d1).get(d2);

	}

}
