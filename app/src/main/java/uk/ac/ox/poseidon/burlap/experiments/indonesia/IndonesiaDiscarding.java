package uk.ac.ox.poseidon.burlap.experiments.indonesia;

import com.google.common.collect.Lists;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.strategies.discarding.DiscardUnderaged;
import uk.ac.ox.oxfish.fisher.strategies.discarding.DiscardUnderagedFactory;
import uk.ac.ox.oxfish.fisher.strategies.discarding.NoDiscarding;
import uk.ac.ox.oxfish.fisher.strategies.discarding.NoDiscardingFactory;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.market.factory.ThreePricesMarketFactory;
import uk.ac.ox.oxfish.model.regs.Anarchy;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;
import uk.ac.ox.oxfish.utility.yaml.FishYAML;
import uk.ac.ox.poseidon.burlap.scenarios.IndonesiaScenario;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by carrknight on 7/12/17.
 */
public class IndonesiaDiscarding {

    // original
//    private static final Path DIRECTORY = Paths.get("docs","20170715 minimum_indonesia");
//    private static final Path SCENARIO_FILE = DIRECTORY.resolve("market.yaml");
//    public static final int DISCARDING_BIN = 1;
//    public static final int MAXIMUM_FINED_BIN = 0;
//    public static final int EXPECTED_NUMBER_OF_BINS = 3;
//    public static final int NUMBER_OF_YEARS_NO_FISHING = 5;
    //   public static final int NUMBER_OF_YEARS_FISHING = 20;

    public static final int DISCARDING_BIN = 56;
    public static final int MAXIMUM_FINED_BIN = 55;
    public static final int EXPECTED_NUMBER_OF_BINS = 100;
    public static final int NUMBER_OF_RUNS = 5;
    public static final int NUMBER_OF_YEARS_NO_FISHING = 0;
    public static final int NUMBER_OF_YEARS_FISHING = 5;
    //boxcar
    private static final Path DIRECTORY = Paths.get("docs", "20171214 boxcar_indonesia");
    private static final Path SCENARIO_FILE = DIRECTORY.resolve("boxcar_indonesia.yaml");

    public static void discardingFine(String[] args) throws IOException {

        File outputFile = DIRECTORY.resolve("discarding_fine.csv").toFile();
        FileWriter writer = prepWriter(outputFile);
        for (double fine = 10; fine > -30; fine = fine - 1d) {

            for (int run = 0; run < NUMBER_OF_RUNS; run++) {
                FishState state = new FishState(System.currentTimeMillis());
                FishYAML yaml = new FishYAML();
                IndonesiaScenario scenario = yaml.loadAs(
                    new FileReader(
                        SCENARIO_FILE.toFile()
                    ), IndonesiaScenario.class
                );
                state.setScenario(scenario);

                ThreePricesMarketFactory market = new ThreePricesMarketFactory();
                scenario.setMarket(market);
                market.setLowAgeThreshold(new FixedDoubleParameter(MAXIMUM_FINED_BIN));
                market.setPriceBelowThreshold(new FixedDoubleParameter(fine));
                market.setPriceBetweenThresholds(new FixedDoubleParameter(10));

                state.start();
                while (state.getYear() <= NUMBER_OF_YEARS_NO_FISHING)
                    state.schedule.step(state);

                for (Fisher fisher : state.getFishers()) {
                    DiscardUnderagedFactory discardUnderagedFactory = new DiscardUnderagedFactory();
                    discardUnderagedFactory.setMinAgeRetained(new FixedDoubleParameter(1d));
                    PeriodicUpdateDiscarding discarding = new PeriodicUpdateDiscarding(
                        Lists.newArrayList(
                            NoDiscarding.class,
                            DiscardUnderaged.class
                        ),
                        Lists.newArrayList(
                            new NoDiscardingFactory(),
                            discardUnderagedFactory
                        )
                    );
                    discarding.start(state, fisher);

                    fisher.setRegulation(new Anarchy());
                }

                while (state.getYear() <= NUMBER_OF_YEARS_FISHING)
                    state.schedule.step(state);


                dumpObservation(writer, run, fine, state, 10d);

            }

        }


    }

    public static FileWriter prepWriter(File outputFile) throws IOException {
        FileWriter writer = new FileWriter(outputFile);
        //writer.write("price_low,price_high,landings,earnings,cash-flow,landings_0,landings_1,landings_2,discarding_agents,catches_0");
        writer.write("run,price_low,price_high,landings,earnings,cash-flow,");
        for (int i = 0; i < EXPECTED_NUMBER_OF_BINS; i++) {
            writer.write("landings_" + i);
            writer.write(",");
        }
        writer.write("discarding_agents");
        for (int i = 0; i < EXPECTED_NUMBER_OF_BINS; i++) {
            writer.write(",");
            writer.write("catches_" + i);
        }

        writer.write("\n");
        writer.flush();
        return writer;
    }

    public static void noDiscardingFine(String[] args) throws IOException {

        File outputFile = DIRECTORY.resolve("nodiscarding_fine.csv").toFile();
        FileWriter writer = prepWriter(outputFile);
        for (double fine = 10; fine > -30; fine = fine - 1d) {

            for (int run = 0; run < NUMBER_OF_RUNS; run++) {
                FishState state = new FishState(System.currentTimeMillis());
                FishYAML yaml = new FishYAML();
                IndonesiaScenario scenario = yaml.loadAs(
                    new FileReader(
                        SCENARIO_FILE.toFile()
                    ), IndonesiaScenario.class
                );
                state.setScenario(scenario);

                ThreePricesMarketFactory market = new ThreePricesMarketFactory();
                scenario.setMarket(market);
                market.setLowAgeThreshold(new FixedDoubleParameter(MAXIMUM_FINED_BIN));
                market.setPriceBelowThreshold(new FixedDoubleParameter(fine));
                market.setPriceBetweenThresholds(new FixedDoubleParameter(10));

                state.start();
                while (state.getYear() <= NUMBER_OF_YEARS_NO_FISHING)
                    state.schedule.step(state);

                for (Fisher fisher : state.getFishers()) {
                /*    DiscardUnderagedFactory discardUnderagedFactory = new DiscardUnderagedFactory();
                    discardUnderagedFactory.setMinAgeRetained(new FixedDoubleParameter(1d));
                    PeriodicUpdateDiscarding discarding = new PeriodicUpdateDiscarding(
                            Lists.newArrayList(NoDiscarding.class,
                                               DiscardUnderaged.class),
                            Lists.newArrayList(new NoDiscardingFactory(),
                                               discardUnderagedFactory)
                    );
                    discarding.start(state, fisher);
                    */
                    fisher.setRegulation(new Anarchy());
                }

                while (state.getYear() <= NUMBER_OF_YEARS_FISHING)
                    state.schedule.step(state);


                dumpObservation(writer, run, fine, state, 10d);

            }

        }


    }

    public static void dumpObservation(
        FileWriter writer,
        int run,
        double fine,
        FishState state,
        double subsidy
    ) throws IOException {
        StringBuffer observation = new StringBuffer();
        observation.append(Integer.toString(run)).append(",");
        observation.append(fine).append(",");
        observation.append(subsidy).append(",");
        observation.append(state.getLatestYearlyObservation("Red Fish Landings")).append(",");
        observation.append(state.getLatestYearlyObservation("Red Fish Earnings")).append(",");
        observation.append(state.getLatestYearlyObservation("Average Cash-Flow")).append(",");
        for (int i = 0; i < EXPECTED_NUMBER_OF_BINS; i++)
            observation.append(state.getLatestYearlyObservation("Red Fish Landings - age bin " + i)).append(",");

        int discarders = 0;
        for (Fisher fisher : state.getFishers())
            if (!fisher.getDiscardingStrategy().getClass().equals(NoDiscarding.class))
                discarders++;

        for (int i = 0; i < EXPECTED_NUMBER_OF_BINS; i++)
            observation.append(",").append(state.getLatestYearlyObservation("Red Fish Catches - age bin " + i));
        observation.append("\n");
        writer.write(observation.toString());
        writer.flush();
        System.out.println(observation);
    }


    public static void noDiscardingSubsidy(String[] args) throws IOException {

        File outputFile = DIRECTORY.resolve("nodiscarding_subsidy.csv").toFile();
        FileWriter writer = prepWriter(outputFile);

        for (double subsidy = 9; subsidy < 100; subsidy = subsidy + 1d) {

            for (int run = 0; run < NUMBER_OF_RUNS; run++) {
                FishState state = new FishState(System.currentTimeMillis());
                FishYAML yaml = new FishYAML();
                IndonesiaScenario scenario = yaml.loadAs(
                    new FileReader(
                        SCENARIO_FILE.toFile()
                    ), IndonesiaScenario.class
                );
                state.setScenario(scenario);

                ThreePricesMarketFactory market = new ThreePricesMarketFactory();
                scenario.setMarket(market);
                market.setLowAgeThreshold(new FixedDoubleParameter(MAXIMUM_FINED_BIN));
                market.setPriceBelowThreshold(new FixedDoubleParameter(10));
                market.setPriceBetweenThresholds(new FixedDoubleParameter(subsidy));

                state.start();
                while (state.getYear() <= NUMBER_OF_YEARS_NO_FISHING)
                    state.schedule.step(state);

                for (Fisher fisher : state.getFishers()) {
                /*    DiscardUnderagedFactory discardUnderagedFactory = new DiscardUnderagedFactory();
                    discardUnderagedFactory.setMinAgeRetained(new FixedDoubleParameter(1d));
                    PeriodicUpdateDiscarding discarding = new PeriodicUpdateDiscarding(
                            Lists.newArrayList(NoDiscarding.class,
                                               DiscardUnderaged.class),
                            Lists.newArrayList(new NoDiscardingFactory(),
                                               discardUnderagedFactory)
                    );
                    discarding.start(state, fisher);
                    */
                    fisher.setRegulation(new Anarchy());
                }

                while (state.getYear() <= NUMBER_OF_YEARS_FISHING)
                    state.schedule.step(state);


                dumpObservation(writer, run, 10d, state, subsidy);


            }

        }


    }

    public static void discarding(String[] args) throws IOException {

        File outputFile = DIRECTORY.resolve("discarding_subsidy.csv").toFile();
        FileWriter writer = prepWriter(outputFile);
        writer.write(
            "price_low,price_high,landings,earnings,cash-flow,landings_0,landings_1,landings_2,discarding_agents,catches_0");
        writer.write("\n");
        writer.flush();
        for (double subsidy = 9; subsidy < 100; subsidy = subsidy + 1d) {

            for (int run = 0; run < NUMBER_OF_RUNS; run++) {
                FishState state = new FishState(System.currentTimeMillis());
                FishYAML yaml = new FishYAML();
                IndonesiaScenario scenario = yaml.loadAs(
                    new FileReader(
                        SCENARIO_FILE.toFile()
                    ), IndonesiaScenario.class
                );
                state.setScenario(scenario);

                ThreePricesMarketFactory market = new ThreePricesMarketFactory();
                scenario.setMarket(market);
                market.setLowAgeThreshold(new FixedDoubleParameter(MAXIMUM_FINED_BIN));
                market.setPriceBelowThreshold(new FixedDoubleParameter(10));
                market.setPriceBetweenThresholds(new FixedDoubleParameter(subsidy));

                state.start();
                while (state.getYear() <= NUMBER_OF_YEARS_NO_FISHING)
                    state.schedule.step(state);

                for (Fisher fisher : state.getFishers()) {
                    DiscardUnderagedFactory discardUnderagedFactory = new DiscardUnderagedFactory();
                    discardUnderagedFactory.setMinAgeRetained(new FixedDoubleParameter(DISCARDING_BIN));
                    PeriodicUpdateDiscarding discarding = new PeriodicUpdateDiscarding(
                        Lists.newArrayList(
                            NoDiscarding.class,
                            DiscardUnderaged.class
                        ),
                        Lists.newArrayList(
                            new NoDiscardingFactory(),
                            discardUnderagedFactory
                        )
                    );
                    discarding.start(state, fisher);
                    fisher.setRegulation(new Anarchy());
                }

                while (state.getYear() <= NUMBER_OF_YEARS_FISHING)
                    state.schedule.step(state);


                StringBuffer observation = new StringBuffer();
                observation.append(10d).append(",");
                observation.append(subsidy).append(",");
                observation.append(state.getLatestYearlyObservation("Red Fish Landings")).append(",");
                observation.append(state.getLatestYearlyObservation("Red Fish Earnings")).append(",");
                observation.append(state.getLatestYearlyObservation("Average Cash-Flow")).append(",");
                observation.append(state.getLatestYearlyObservation("Red Fish Landings - age bin 0")).append(",");
                observation.append(state.getLatestYearlyObservation("Red Fish Landings - age bin 1")).append(",");
                observation.append(state.getLatestYearlyObservation("Red Fish Landings - age bin 2")).append(",");


                int discarders = 0;
                for (Fisher fisher : state.getFishers())
                    if (!fisher.getDiscardingStrategy().getClass().equals(NoDiscarding.class))
                        discarders++;

                observation.append(discarders).append(",");
                observation.append(state.getLatestYearlyObservation("Red Fish Catches - age bin 0")).append("\n");
                writer.write(observation.toString());
                writer.flush();
                System.out.println(observation);

            }

        }

        writer.close();

    }


}
