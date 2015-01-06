package bot;

public class Values {
  public static float startingRegion(int neutrals, int reward) {
    if (reward == 0) {
      return 0;
    }

    return neutrals;
  }
}
