package unibo.cp;

public class VrpSolution {

    public int[] demand_desc_perm;
    boolean foundSolution;
    String statistics;
    String solutionDebug;

    InputData inputData;
    public long[][] distanceMatrix;
    public long[] distance;

    int[] vc;
    int[] order;

    Long objectiveFunctionValue;
    Long totalDistance;
    Long usedVehicles;
}
