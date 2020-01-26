package unibo.cp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.testng.Assert;

import java.util.Arrays;

class VrpSolverTest {

    int timeoutSeconds = 10;

    @Test
    public void test2plus2() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testDummy() {
        testCaseExpectingSuccess("dummy.dzn");
    }

    @Test
    public void testPr00() {
        testCaseExpectingSuccess("pr00.dzn");
    }

    @Test
    public void testPr01() {
        testCaseExpectingSuccess("pr01.dzn");
    }

    @Test
    public void testPr02() {
        testCaseExpectingSuccess("pr02.dzn");
    }

    @Test
    public void testPr03() {
        testCaseExpectingSuccess("pr03.dzn");
    }

    @Test
    public void testPr04() {
        testCaseExpectingSuccess("pr04.dzn");
    }

    @Test
    public void testPr05() {
        testCaseExpectingSuccess("pr05.dzn");
    }


    @Test
    public void testPr06() {
        testCaseExpectingSuccess("pr06.dzn");
    }


    @Test
    public void testPr07() {
        testCaseExpectingSuccess("pr07.dzn");
    }


    @Test
    public void testPr08() {
        testCaseExpectingSuccess("pr08.dzn");
    }


    @Test
    public void testPr09() {
        testCaseExpectingSuccess("pr09.dzn");
    }


    @Test
    public void testPr10() {
        testCaseExpectingSuccess("pr10.dzn");
    }


    @Test
    public void testPr11() {
        testCaseExpectingSuccess("pr11.dzn");
    }


    @Test
    public void testPr12() {
        testCaseExpectingSuccess("pr12.dzn");
    }

    @Test
    public void testPr13() {
        testCaseExpectingSuccess("pr13.dzn");
    }

    @Test
    public void testPr14() {
        testCaseExpectingSuccess("pr14.dzn");
    }

    @Test
    public void testPr15() {
        testCaseExpectingSuccess("pr15.dzn");
    }

    @Test
    public void testPr16() {
        testCaseExpectingSuccess("pr16.dzn");
    }

    @Test
    public void testPr17() {
        testCaseExpectingSuccess("pr17.dzn");
    }

    @Test
    public void testPr18() {
        testCaseExpectingSuccess("pr18.dzn");
    }

    @Test
    public void testPr19() {
        testCaseExpectingSuccess("pr19.dzn");
    }

    @Test
    public void testPr20() {
        testCaseExpectingSuccess("pr20.dzn");
    }


    private void testCaseExpectingSuccess(String fileName){
        // given
        final InputData inputData = InputDataProvider.getInput(fileName);

        // when
        final VrpSolution solution = new VrpSolver(inputData).solve(timeoutSeconds);

        // then
        if (solution.foundSolution == false) {
            Assert.fail("Couldn't find solution for " + fileName + " in " + timeoutSeconds + " seconds!!!");
        }

        // calculate used resource
        int[] vehiclesCap = new int[solution.inputData.VEHICLES_NO];
        for(int v = 0; v < solution.inputData.VEHICLES_NO; v++) {
            vehiclesCap[v] = 0;
        }
        for (int i = 0; i < solution.vc.length; i++) {
            final int before = vehiclesCap[solution.vc[i]];
            final int toAdd = inputData.DEMAND[solution.demand_desc_perm[i]];
//            vehiclesCap[solution.vc[i]] = before + toAdd;
            vehiclesCap[solution.vc[i]] += toAdd;
        }

        // validate used resources not exceeded
        for(int v = 0; v < solution.inputData.VEHICLES_NO; v++) {
            Assert.assertTrue(
                    vehiclesCap[v] <= solution.inputData.CAPACITY,
                    "Vehicle " + v + " exceeds capacity (loaded " + vehiclesCap[v] + ") !!!\nSolution dump:\n" + solution.solutionDebug + ".\n"
            );
        }

        final int actualSum = Arrays.stream(inputData.DEMAND).sum();
        final int expectedSum = Arrays.stream(vehiclesCap).sum();

        Assert.assertEquals(actualSum, expectedSum, "sum(Demand) is not equal no sum(vehicle_cap)!!!");
    }
}