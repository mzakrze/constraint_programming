package unibo.cp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        int timeoutSeconds = 300;

        List<String> files = new ArrayList<>();
        files.add("pr01.dzn");
//        files.add("pr02.dzn");
        files.add("pr03.dzn");
        files.add("pr04.dzn");
        files.add("pr05.dzn");
        files.add("pr06.dzn");
        files.add("pr07.dzn");
        files.add("pr08.dzn");
        files.add("pr09.dzn");
        files.add("pr10.dzn");

        final StringBuilder sb = new StringBuilder();

        for (String fileName : files) {
            System.out.println("running " + fileName + ". Time: " + Instant.now());

            String resultFileName = fileName + ".result";

            final InputData inputData = InputDataProvider.getInput(fileName);

            final VrpSolution solution = new VrpSolver(inputData).solve(timeoutSeconds);

            String output = solution.solutionDebug + "\n\n" + solution.statistics;

            BufferedWriter writer = new BufferedWriter(new FileWriter(resultFileName));
            writer.write(output);

            writer.close();

            sb.append("filename: " + fileName + ", objective function: " + solution.objectiveFunctionValue + "\n");

        }

        BufferedWriter writer = new BufferedWriter(new FileWriter("dom_w_deg.results"));
        writer.write(sb.toString());
        writer.close();
    }
}
