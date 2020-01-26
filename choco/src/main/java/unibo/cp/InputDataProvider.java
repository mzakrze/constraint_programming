package unibo.cp;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class InputDataProvider {

    public static InputData getInput(String fileName) {

        final InputData result = new InputData();

        result.NAME = "dummy.txt";
        result.LOC_X = new Float[] {1f, 2f, 3f, 4f, 0f};
        result.LOC_Y = new Float[] {1f, 2f, 3f, 4f, 0f};
        result.DEMAND = new int[] {10, 10, 10, 10};
        result.VEHICLES_NO = 4;
        result.CAPACITY = 10;

        String token_locX = "locX = [";
        String token_locY = "locY = [";
        String token_Demand = "Demand = [";
        String token_NumVehicles = "NumVehicles = ";
        String token_Cap = "Cap = ";
        String token_Name = "Name = ";

        try (Stream<String> stream = Files.lines(Paths.get(InputDataProvider.class.getClassLoader().getResource(fileName).getFile()))) {
            stream.forEach(line -> {

                if (line.startsWith(token_locX)) {
                    result.LOC_X = parseFloatArray(line, token_locX);
                } else if (line.startsWith(token_locY)) {
                    result.LOC_Y = parseFloatArray(line, token_locY);
                } else if (line.startsWith(token_Demand)) {
                    final Integer[] demand = parseIntArray(line, token_Demand);
                    result.DEMAND = new int[demand.length];
                    for (int i = 0; i < demand.length; i++) {
                        result.DEMAND[i] = demand[i];
                    }
                } else if (line.startsWith(token_NumVehicles)) {
                    result.VEHICLES_NO = parseInt(line, token_NumVehicles);
                } else if (line.startsWith(token_Cap)) {
                    result.CAPACITY = parseInt(line, token_Cap);
                } else if (line.startsWith(token_Name)) {
                    // do nothing
                } else {
                    throw new IllegalStateException("Couldn't parse line " + line);
                }

            });
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't find file " + fileName, e);
        }

        return result;
    }

    private static int parseInt(String line, String token) {
        line = line.substring(token.length());
        line = line.substring(0, line.indexOf(";"));

        return Integer.valueOf(line);
    }

    private static Float[] parseFloatArray(String line, String token) {
        line = line.substring(token.length());
        line = line.substring(0, line.indexOf("]"));

        List<Float> res = new ArrayList<>();
        for (String val : line.split(",")) {
            res.add(Float.valueOf(val.trim()));
        }

        return res.stream().toArray(Float[]::new);
    }

    private static Integer[] parseIntArray(String line, String token) {
        line = line.substring(token.length());
        line = line.substring(0, line.indexOf("]"));

        List<Integer> res = new ArrayList<>();
        for (String val : line.split(",")) {
            res.add(Integer.valueOf(val.trim()));
        }

        return res.stream().toArray(Integer[]::new);
    }

}
