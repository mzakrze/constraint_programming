package unibo.cp;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.BacktrackCounter;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.loop.lns.INeighborFactory;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class VrpSolver {

    // input data, and calculated input data
    private final InputData data;
    private int n;
    private int maxDemand;
    private long[][] distanceMatrix;
    private long[] distance;

    // Constraint Programming variables
    IntVar[] order;
    IntVar[] vc;
    IntVar[] left_s;
    IntVar[] right;
    IntVar[] left;

    public VrpSolver(InputData data) {
        this.data = data;

        this.n = data.DEMAND.length;
        this.maxDemand = Arrays.stream(data.DEMAND).max().getAsInt();
        distanceMatrix = new long[n][n];
        distance = new long[n];

        for(int v1 = 0; v1 < n; v1++) {
            for(int v2 = 0; v2 < n; v2++) {
                distanceMatrix[v1][v2] = v1 == v2 ? 0 : calculateDistance(data.LOC_X[v1], data.LOC_Y[v1], data.LOC_X[v2], data.LOC_Y[v2]);
            }
        }

        for(int v = 0; v < n; v++) {
            distance[v] = calculateDistance(data.LOC_X[v], data.LOC_Y[v], data.LOC_X[n], data.LOC_Y[n]);
        }
    }

    public VrpSolution solve(int timeoutSeconds) {
        Model model = new Model("vrp");
        Solution solution = new Solution(model);

        order = model.intVarArray("order_", n, 0, n - 1);
        vc = model.intVarArray("vc_", n, 0, data.VEHICLES_NO - 1);

        model.allDifferent(order).post();

        final IntVar[] vc_sorted = model.intVarArray("vc_sorted_", n, 0, data.VEHICLES_NO - 1);
        model.sort(vc, vc_sorted).post();

        /*
        constraint forall(i in 1..n)
            (vc[order[i]] = vc_sorted[i]);
         */
        left_s = model.intVarArray("left_s_", n, 0, n - 1);
        right = model.intVarArray("right_", n, 0, n - 1);
        left = model.intVarArray("left_", n, 0, n - 1);
        for (int i = 0; i < n; i++) {
            final IntVar iVar = model.intVar(i);

            model.element(right[i], vc_sorted, iVar, 0).post();
            model.element(left_s[i], order, iVar, 0).post();
            model.element(left[i], vc, left_s[i], 0).post();
            model.arithm(left[i], "=", right[i]).post();
        }


        int[] demand_desc_perm = new int[n];
        for (int i = 0; i < n; i++) {
            demand_desc_perm[i] = i; //data.DEMAND[i];
        }
        demand_desc_perm = Arrays.stream(demand_desc_perm)
                .boxed()
                .sorted((a, b) -> data.DEMAND[b] - data.DEMAND[a])
                .mapToInt(e -> e)
                .toArray();


        /*
        constraint forall(v in 1..NumVehicles)
          (Cap >= sum([
            if vc[i] == v then
              Demand[demand_desc_perm[i]]
            else
              0
            endif
            | i in 1..n
          ]));
         */
        final IntVar capConstVar = model.intVar(data.CAPACITY);
        for(int v = 0; v < data.VEHICLES_NO; v++) {
            final IntVar[] vehicle_vs = model.intVarArray("vehicle_v_" + v, n, 0, maxDemand);
            for(int i = 0; i < n; i++) {
                model.ifThenElse(model.arithm(vc[i], "=", model.intVar(v)).reify(),
                        // (1) -> Demand[demand_desc_perm[i]]
                        model.arithm(vehicle_vs[i], "=", model.intVar(data.DEMAND[demand_desc_perm[i]]))
                        ,
                        // (2) -> 0
                        model.arithm(vehicle_vs[i], "=", model.intVar(0))
                );
            }

            model.sum(vehicle_vs, "<=", capConstVar).post();
        }

        Solver solver = model.getSolver();
        solver.limitTime(timeoutSeconds + "s");

        // 1. dom_w_deg
//        solver.setSearch(Search.domOverWDegSearch(vc), Search.domOverWDegSearch(order));

        // 2. dom_w_deg + restart
//        solver.setSearch(Search.domOverWDegSearch(vc), Search.domOverWDegSearch(order));
//        solver.setLubyRestart(2, new BacktrackCounter(model, 30), 100000);

        // 3. dom_w_deg + restart + lns
//        solver.setSearch(Search.domOverWDegSearch(vc), Search.domOverWDegSearch(order));
//        solver.setLubyRestart(2, new BacktrackCounter(model, 30), 100000)
//        solver.setLNS(INeighborFactory.random(order));


        final VrpSolution vrpSolution = new VrpSolution();
        vrpSolution.foundSolution = false;
        vrpSolution.distance = distance;
        vrpSolution.distanceMatrix = distanceMatrix;
        vrpSolution.inputData = data;
        long bestObjectiveValue = Long.MAX_VALUE;
        while(solver.solve()) {
            solution.record();

            final long objectiveFunction = calculateObjectiveFunction(solution);

            if(objectiveFunction < bestObjectiveValue) {
                System.out.println(bestObjectiveValue);
                vrpSolution.foundSolution = true;
                bestObjectiveValue = objectiveFunction;
                vrpSolution.totalDistance = calculateTotalDistance(solution);
                vrpSolution.usedVehicles = calculateUsedVehicles(solution);
                vrpSolution.solutionDebug = debugSolution(solution, demand_desc_perm);
                vrpSolution.objectiveFunctionValue = objectiveFunction;
                vrpSolution.demand_desc_perm = demand_desc_perm;

                vrpSolution.order = new int[n];
                vrpSolution.vc = new int[n];

                for(int i = 0; i < n; i++) {
                    vrpSolution.order[i] = solution.getIntVal(order[i]);
                    vrpSolution.vc[i] = solution.getIntVal(vc[i]);
                }
            }
        }

        if (vrpSolution.foundSolution == false) {
            return vrpSolution;
        }

        vrpSolution.objectiveFunctionValue = bestObjectiveValue;

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            solver.setOut(new PrintStream(os));
            solver.printStatistics();
            vrpSolution.statistics = os.toString("UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return vrpSolution;

    }

    private long calculateDistance(float x1, float y1, float x2, float y2) {
        float dist1 = distanceDiff(x1, x2);
        float dist2 = distanceDiff(y1, y2);
        return Math.round(Math.sqrt(dist1*dist1 + dist2*dist2));
    }

    private float distanceDiff(float v1, float v2) {
        if (v1 > 0 && v2 > 0) {
            return Math.abs(v1 - v2);
        } else if (v1 < 0 && v2 < 0) {
            return Math.abs(Math.abs(v1) - Math.abs(v2));
        } else {
            return Math.abs(v1) + Math.abs(v2);
        }
    }

    public String arrVarToString(String name, IntVar[] arr) {
        return name + " = [" +
                IntStream.range(0, arr.length)
                        .mapToObj(i -> arr[i].getValue() + "")
                        .collect(Collectors.joining(", "))
                +"]\n";
    }

    private String arrToString(String name, int[] arr) {
        return name + " = [" +
                IntStream.range(0, arr.length)
                    .mapToObj(i -> arr[i] + "")
                    .collect(Collectors.joining(", "))
                + "]\n";
    }

    public String debugSolution(Solution solution, int[] demand_desc_perm) {
        return arrVarToString("order", order) + arrVarToString("vc", vc) +
                arrToString("demand_desc_perm", demand_desc_perm) +
                "total distance = " + calculateTotalDistance(solution) + "\n" +
                "used vehicles = " + calculateUsedVehicles(solution) + "\n" +
                "objective function = " + calculateObjectiveFunction(solution)
                ;
    }

    private long calculateObjectiveFunction(Solution solution) {
        return 10000 * calculateUsedVehicles(solution) + calculateTotalDistance(solution);
    }

    private long calculateTotalDistance(Solution solution) {
        long totalDistance = 0;
        totalDistance += distance[solution.getIntVal(vc[0])];
        for(int i = 0; i < n-1; i++) {
            final int vertexFrom = solution.getIntVal(order[i]);
            final int vertexTo = solution.getIntVal(order[i+1]);

            final int fromVehicle = solution.getIntVal(vc[i]);
            final int toVehicle = solution.getIntVal(vc[i + 1]);

            if (fromVehicle == toVehicle) {
                totalDistance += distanceMatrix[vertexFrom][vertexTo];
            } else {
                totalDistance += distance[fromVehicle] + distance[toVehicle];
            }
        }
        totalDistance += distance[solution.getIntVal(vc[0])];

        return totalDistance;
    }

    private long calculateUsedVehicles(Solution solution) {
        return IntStream.range(0, n)
                .map(i -> solution.getIntVal(vc[i]))
                .distinct()
                .count();
    }
}

