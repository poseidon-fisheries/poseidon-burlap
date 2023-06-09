/*
 *     POSEIDON, an agent-based model of fisheries
 *     Copyright (C) 2017  CoHESyS Lab cohesys.lab@gmail.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package uk.ac.ox.poseidon.burlap.experiments;

import ec.util.MersenneTwisterFast;
import uk.ac.ox.oxfish.biology.growers.SimpleLogisticGrowerFactory;
import uk.ac.ox.oxfish.biology.initializer.factory.DiffusingLogisticFactory;
import uk.ac.ox.oxfish.biology.initializer.factory.LinearGetterBiologyFactory;
import uk.ac.ox.oxfish.biology.initializer.factory.OneSpeciesSchoolFactory;
import uk.ac.ox.oxfish.fisher.heatmap.acquisition.factory.ExhaustiveAcquisitionFunctionFactory;
import uk.ac.ox.oxfish.fisher.heatmap.regression.factory.NearestNeighborTransductionFactory;
import uk.ac.ox.oxfish.fisher.strategies.destination.DestinationStrategy;
import uk.ac.ox.oxfish.fisher.strategies.destination.factory.*;
import uk.ac.ox.oxfish.geography.discretization.SquaresMapDiscretizerFactory;
import uk.ac.ox.oxfish.geography.mapmakers.SimpleMapInitializerFactory;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.scenario.PrototypeScenario;
import uk.ac.ox.oxfish.model.scenario.Scenario;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.FishStateUtilities;
import uk.ac.ox.oxfish.utility.adaptation.probability.factory.FixedProbabilityFactory;
import uk.ac.ox.oxfish.utility.adaptation.probability.factory.SocialAnnealingProbabilityFactory;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;
import uk.ac.ox.oxfish.utility.parameters.UniformDoubleParameter;
import uk.ac.ox.oxfish.utility.yaml.FishYAML;
import uk.ac.ox.poseidon.burlap.scenarios.DerisoCaliforniaScenario;
import uk.ac.ox.poseidon.burlap.strategies.LogitRPUEDestinationFactory;
import uk.ac.ox.poseidon.burlap.strategies.SoftmaxBanditFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IndirectInferencePaper {


    /**
     * list of names and associated "initializers" which are supposed to randomize some scenario parameters
     */
    public final static LinkedHashMap<String,
        ScenarioInitializer> initializers = new LinkedHashMap<>();
    public static final int TARGET_RUNS = 200;
    public static final int SIMULATION_YEARS = 1;
    public static final int CANDIDATE_RUNS = 1;
    /**
     * store list of names of the algorithms to use and their factory; this is used for the model-selection bit
     */
    public final static LinkedHashMap<String,
        AlgorithmFactory<? extends DestinationStrategy>> strategies =
        new LinkedHashMap<>();
    private final static Path MAIN_DIRECTORY = Paths.get("docs", "indirect_inference", "simulation_short_validation");
    private final static Path MLOGIT_SCRIPT = MAIN_DIRECTORY.resolve("mlogit_fit_full.R");

    static {

        //the baseline scenario: fishing front and all

        initializers.put(
            "baseline",
            new ScenarioInitializer() {
                @Override
                public void initialize(
                    final Scenario scenario, final long seed,
                    final AlgorithmFactory<? extends DestinationStrategy> strategy
                ) {

                    final PrototypeScenario cast = (PrototypeScenario) scenario;
                    //randomize biomass, speed and port position
                    final MersenneTwisterFast random = new MersenneTwisterFast(seed);
                    final DiffusingLogisticFactory biology = (DiffusingLogisticFactory) cast
                        .getBiologyInitializer();
                    biology.setCarryingCapacity(
                        new FixedDoubleParameter(random.nextDouble() * 9000 + 1000)
                    );
                    biology.setDifferentialPercentageToMove(
                        new FixedDoubleParameter(random.nextDouble() * .003)
                    );
                    ((SimpleLogisticGrowerFactory) biology.getGrower()).setSteepness(
                        new FixedDoubleParameter(random.nextDouble() * .5 + .3)
                    );

                    final SimpleMapInitializerFactory map = new SimpleMapInitializerFactory();
                    map.setHeight(new FixedDoubleParameter(50));
                    map.setWidth(new FixedDoubleParameter(50));
                    map.setCoastalRoughness(new FixedDoubleParameter(0));
                    map.setMaxLandWidth(new FixedDoubleParameter(10));
                    cast.setMapInitializer(map);
                    cast.setPortPositionX(40);
                    cast.setPortPositionY(random.nextInt(50));

                    cast.setMapMakerDedicatedRandomSeed(seed);

                    cast.setDestinationStrategy(strategy);

                }
            }
        );


        initializers.put(
            "chaser",
            new ScenarioInitializer() {
                @Override
                public void initialize(
                    final Scenario scenario, final long seed,
                    final AlgorithmFactory<? extends DestinationStrategy> destinationStrategy
                ) {

                    final PrototypeScenario cast = (PrototypeScenario) scenario;
                    final MersenneTwisterFast random = new MersenneTwisterFast(seed);
                    cast.setHoldSize(
                        new FixedDoubleParameter(
                            random.nextDouble() * 100 + 50
                        )
                    );
                    final OneSpeciesSchoolFactory biologyInitializer = (OneSpeciesSchoolFactory) cast
                        .getBiologyInitializer();
                    biologyInitializer.setDiameter(
                        new FixedDoubleParameter(
                            random.nextInt(8) + 1
                        )
                    );
                    biologyInitializer.setSpeedInDays(
                        new FixedDoubleParameter(
                            random.nextInt(10) + 1
                        )
                    );
                    biologyInitializer.setNumberOfSchools(
                        new FixedDoubleParameter(
                            random.nextInt(3) + 1
                        )
                    );

                    final SimpleMapInitializerFactory map = new SimpleMapInitializerFactory();
                    map.setHeight(new FixedDoubleParameter(50));
                    map.setWidth(new FixedDoubleParameter(50));
                    map.setCoastalRoughness(new FixedDoubleParameter(0));
                    map.setMaxLandWidth(new FixedDoubleParameter(10));
                    cast.setMapInitializer(map);
                    cast.setPortPositionX(40);
                    cast.setPortPositionY(random.nextInt(50));

                    cast.setMapMakerDedicatedRandomSeed(seed);

                    cast.setDestinationStrategy(destinationStrategy);

                }
            }

        );

        initializers.put(
            "deriso",
            new ScenarioInitializer() {
                @Override
                public void initialize(
                    final Scenario scenario, final long seed,
                    final AlgorithmFactory<? extends DestinationStrategy> destinationStrategy
                ) {

                    final DerisoCaliforniaScenario cast = (DerisoCaliforniaScenario) scenario;
                    final MersenneTwisterFast random = new MersenneTwisterFast(seed);
                    cast.setHoldSizePerBoat(
                        new FixedDoubleParameter(
                            random.nextDouble() * 10000 + 5000
                        )
                    );
                    final LinkedHashMap<String, String> exogenousCatches = new LinkedHashMap<>();
                    exogenousCatches.put("Dover Sole", Double.toString(random.nextDouble() * 500000 + 300000));
                    exogenousCatches.put("Sablefish", Double.toString(random.nextDouble() * 5000000 + 3000000));
                    cast.setExogenousCatches(exogenousCatches
                    );

                    cast.setDestinationStrategy(destinationStrategy);

                }
            }

        );


        initializers.put(
            "threeport",
            new ScenarioInitializer() {
                @Override
                public void initialize(
                    final Scenario scenario, final long seed,
                    final AlgorithmFactory<? extends DestinationStrategy> strategy
                ) {

                    final PrototypeScenario cast = (PrototypeScenario) scenario;
                    //randomize biomass, speed and port position
                    final MersenneTwisterFast random = new MersenneTwisterFast(seed);
                    final DiffusingLogisticFactory biology = (DiffusingLogisticFactory) cast
                        .getBiologyInitializer();
                    biology.setCarryingCapacity(
                        new FixedDoubleParameter(random.nextDouble() * 9000 + 1000)
                    );
                    biology.setDifferentialPercentageToMove(
                        new FixedDoubleParameter(random.nextDouble() * .003)
                    );
                    ((SimpleLogisticGrowerFactory) biology.getGrower()).setSteepness(
                        new FixedDoubleParameter(random.nextDouble() * .5 + .3)
                    );

                    final SimpleMapInitializerFactory map = new SimpleMapInitializerFactory();
                    map.setHeight(new FixedDoubleParameter(50));
                    map.setWidth(new FixedDoubleParameter(50));
                    map.setCoastalRoughness(new UniformDoubleParameter(0, 4));
                    map.setMaxLandWidth(new UniformDoubleParameter(1, 10));
                    cast.setMapInitializer(map);

                    cast.setMapMakerDedicatedRandomSeed(seed);
                    //functional friendship only!
                    cast.getNetworkBuilder().addPredicate((from, to) -> from.getHomePort().equals(to.getHomePort()));
                    cast.setDestinationStrategy(strategy);

                }
            }
        );


        initializers.put(
            "moving",
            new ScenarioInitializer() {
                @Override
                public void initialize(
                    final Scenario scenario, final long seed,
                    final AlgorithmFactory<? extends DestinationStrategy> strategy
                ) {

                    final PrototypeScenario cast = (PrototypeScenario) scenario;
                    //randomize biomass, speed and port position
                    final MersenneTwisterFast random = new MersenneTwisterFast(seed);
                    final LinearGetterBiologyFactory biology = (LinearGetterBiologyFactory) cast
                        .getBiologyInitializer();
                    biology.setxDay(new UniformDoubleParameter(-3, -1));
                    biology.setyDay(new UniformDoubleParameter(1, 3));
                    biology.setX(new UniformDoubleParameter(0, 200));
                    biology.setY(new UniformDoubleParameter(-200, 0));

                    final SimpleMapInitializerFactory map = new SimpleMapInitializerFactory();
                    cast.setMapInitializer(map);

                    cast.setMapMakerDedicatedRandomSeed(seed);
                    //functional friendship only!
                    //      cast.getNetworkBuilder().addPredicate((from, to) -> from.getHomePort().equals(to.getHomePort()));
                    cast.setDestinationStrategy(strategy);

                }
            }
        );

    }

    //fill up the strategies map with pre-made models
    static {

        //perfect agents
        final LogitRPUEDestinationFactory perfect = new LogitRPUEDestinationFactory();
        final SquaresMapDiscretizerFactory discretizer = new SquaresMapDiscretizerFactory();
        discretizer.setHorizontalSplits(new FixedDoubleParameter(2));
        discretizer.setVerticalSplits(new FixedDoubleParameter(2));
        perfect.setDiscretizer(discretizer);
        strategies.put(
            "perfect3by3",
            perfect
        );


        //3 variants of explore-exploit-imitate
        final PerTripImitativeDestinationFactory exploreExploit = new PerTripImitativeDestinationFactory();
        exploreExploit.setProbability(new FixedProbabilityFactory(.2, 1));
        exploreExploit.setStepSize(new FixedDoubleParameter(5));
        exploreExploit.setAlwaysCopyBest(true);
        exploreExploit.setAutomaticallyIgnoreAreasWhereFishNeverGrows(true);
        exploreExploit.setAutomaticallyIgnoreMPAs(true);
        strategies.put("explore20", exploreExploit);

        final PerTripImitativeDestinationFactory exploreExploit80 = new PerTripImitativeDestinationFactory();
        exploreExploit80.setProbability(new FixedProbabilityFactory(.8, 1));
        exploreExploit80.setStepSize(new FixedDoubleParameter(5));
        exploreExploit80.setAlwaysCopyBest(true);
        exploreExploit80.setAutomaticallyIgnoreAreasWhereFishNeverGrows(true);
        exploreExploit80.setAutomaticallyIgnoreMPAs(true);
        strategies.put("explore80", exploreExploit80);

        final PerTripImitativeDestinationFactory exploreLarge = new PerTripImitativeDestinationFactory();
        exploreLarge.setProbability(new FixedProbabilityFactory(.2, 1));
        exploreLarge.setStepSize(new FixedDoubleParameter(20));
        exploreLarge.setAlwaysCopyBest(true);
        exploreLarge.setAutomaticallyIgnoreAreasWhereFishNeverGrows(true);
        exploreLarge.setAutomaticallyIgnoreMPAs(true);
        strategies.put("exploreLarge", exploreLarge);

        //heatmapper (these are the parameters in the original kernel regression)
        final HeatmapDestinationFactory heatmap = new HeatmapDestinationFactory();
        final ExhaustiveAcquisitionFunctionFactory acquisition = new ExhaustiveAcquisitionFunctionFactory();
        acquisition.setProportionSearched(new FixedDoubleParameter(.1));
        heatmap.setAcquisition(acquisition);
        heatmap.setExplorationStepSize(new FixedDoubleParameter(1));
        heatmap.setProbability(new FixedProbabilityFactory(.5, 1));
        final NearestNeighborTransductionFactory regression = new NearestNeighborTransductionFactory();
        // regression.setTimeBandwidth(new FixedDoubleParameter(0.999989));
        regression.setSpaceBandwidth(new FixedDoubleParameter(5));
        heatmap.setRegression(regression);
        strategies.put("nn", heatmap);

        //social annealing
        final PerTripImitativeDestinationFactory annealing = new PerTripImitativeDestinationFactory();
        annealing.setAlwaysCopyBest(true);
        annealing.setAutomaticallyIgnoreAreasWhereFishNeverGrows(true);
        annealing.setAutomaticallyIgnoreMPAs(true);
        annealing.setStepSize(new FixedDoubleParameter(5));
        annealing.setProbability(new SocialAnnealingProbabilityFactory(.7));
        strategies.put("annealing", annealing);


        //2 softmax bandits (differ in number of splits!)
        final BanditDestinationFactory bandit = new BanditDestinationFactory();
        final SoftmaxBanditFactory softmax = new SoftmaxBanditFactory();
        bandit.setBandit(softmax);
        final SquaresMapDiscretizerFactory banditDiscretizer = new SquaresMapDiscretizerFactory();
        banditDiscretizer.setVerticalSplits(new FixedDoubleParameter(2));
        banditDiscretizer.setHorizontalSplits(new FixedDoubleParameter(2));
        bandit.setDiscretizer(banditDiscretizer);
        strategies.put("bandit3by3", bandit);

        final BanditDestinationFactory bandit2 = new BanditDestinationFactory();
        final SoftmaxBanditFactory softmax2 = new SoftmaxBanditFactory();
        bandit2.setBandit(softmax2);
        final SquaresMapDiscretizerFactory banditDiscretizer2 = new SquaresMapDiscretizerFactory();
        banditDiscretizer2.setVerticalSplits(new FixedDoubleParameter(4));
        banditDiscretizer2.setHorizontalSplits(new FixedDoubleParameter(4));
        bandit2.setDiscretizer(banditDiscretizer2);
        strategies.put("bandit5by5", bandit2);

        //gravitational pull
        final GravitationalSearchDestinationFactory gravitational = new GravitationalSearchDestinationFactory();
        strategies.put("gravitational", gravitational);

        //randomizer
        strategies.put(
            "random",
            new RandomThenBackToPortFactory()
        );

    }


    public static void main(final String[] args) throws IOException, InterruptedException {


        //reader and randomizer
        final FishYAML yamler = new FishYAML();
        final MersenneTwisterFast random = new MersenneTwisterFast(System.currentTimeMillis());

        final String onlyScenario = args[0];
        final ScenarioInitializer selected = initializers.get(onlyScenario);
        initializers.clear();
        initializers.put(args[0], selected);

        //strategies that make up
        final LinkedHashSet<Map.Entry<String, AlgorithmFactory<? extends DestinationStrategy>>> mainStrategiesLeft =
            new LinkedHashSet<>(strategies.entrySet());

        int firstRun = 0;
        if (args.length > 1) //if we are resuming a previous run
        {
            firstRun = Integer.parseInt(args[1]);
            //find the main strategy you are going to start with
            final String startingMainStrategy = args[2];
            do {
                final Map.Entry<String, AlgorithmFactory<? extends DestinationStrategy>> nextMainStrategy = mainStrategiesLeft.iterator()
                    .next();
                if (nextMainStrategy.getKey().equalsIgnoreCase(startingMainStrategy))
                    break;
                else
                    mainStrategiesLeft.remove(nextMainStrategy);
            }
            while (true);

        }


        for (final Map.Entry<String, ScenarioInitializer> initializer : initializers.entrySet()) {

            final Path scenarioDirectory = MAIN_DIRECTORY.resolve(initializer.getKey());
            final Path inputDirectory = scenarioDirectory.resolve("inputs");

            final String pathToCSV = scenarioDirectory.resolve(initializer.getKey() + ".csv")
                .toAbsolutePath()
                .toString();
            final Path pathToAggregates = scenarioDirectory.resolve(initializer.getKey() + "_aggregates.csv");
            //Species 0 Landings
            //Total Effort
            //Average Distance From Port
            //Average Number of Trips
            //Average Hours Out
            //Average Cash-Flow
            if (args.length == 1) {
                final boolean alreadyExists = pathToAggregates.toFile().exists();
                if (!alreadyExists) {

                    try (final FileWriter writer =
                             new FileWriter(pathToAggregates.toFile(), true)) {
                        writer.append(
                            "landings,effort,distance,trips,hours,profits,run,target_strategy,current_strategy,scenario,isTargetRun,seed");

                        writer.append("\n");
                        writer.close();
                    }
                }

            }
            for (final Map.Entry<String, AlgorithmFactory<? extends DestinationStrategy>> targetStrategy :
                mainStrategiesLeft) {

                fullStrategyLoop(yamler,
                    random,
                    initializer,
                    scenarioDirectory,
                    inputDirectory,
                    pathToCSV,
                    targetStrategy,
                    firstRun, CANDIDATE_RUNS, pathToAggregates
                );
                firstRun = 0; //it's not 0 only for the first run when we are resuming!


            }


        }

    }

    public static void fullStrategyLoop(
        final FishYAML yamler, final MersenneTwisterFast random,
        final Map.Entry<String, ScenarioInitializer> initializer,
        final Path scenarioDirectory, final Path inputDirectory,
        final String pathToCSV,
        final Map.Entry<String, AlgorithmFactory<? extends DestinationStrategy>> targetStrategy,
        final int initialRun, final int maxCandidateRuns,
        final Path pathToAggregates
    ) throws IOException, InterruptedException {
        for (int run = 100; run < TARGET_RUNS; run++) {

            final FileReader reader = new FileReader(
                scenarioDirectory.resolve(initializer.getKey() + ".yaml").toFile()
            );
            final Scenario mainScenario = yamler.loadAs(
                reader, Scenario.class

            );
            reader.close();
            //first run the target!
            initializer.getValue().initialize(mainScenario, run, targetStrategy.getValue());
            final String targetName = targetStrategy.getKey() + "_" + run;
            Path output = scenarioDirectory.resolve("output").resolve(targetName);
            output.toFile().mkdirs();
            //write down the scenario to file;
            //this is in order to keep a record of everything
            inputDirectory.toFile().mkdirs();
            FileWriter writer = new FileWriter(
                inputDirectory.resolve(targetName + ".yaml").toFile());
            yamler.dump(
                mainScenario,
                writer
            );
            writer.close();

                                /*
            Rscript ~/code/oxfish/docs/indirect_inference/simulation/baseline/mlogit_fit.R
            ~/code/oxfish/docs/indirect_inference/simulation/baseline/output/perfect3by3_1/logistic_long.csv
            ~/code/oxfish/docs/indirect_inference/simulation/baseline/baseline.csv 2
            baseline 2 perfect3by3 perfect3by3 TRUE
             */
            final String runArgument = Integer.toString(run);
            final String scenario = initializer.getKey();
            final String seedArgument = runArgument;
            final String targetStrategyArgument = targetStrategy.getKey();
            String currentStrategyArgument = targetStrategyArgument;
            String isTargetRun = "TRUE";
            Logger.getGlobal().info("Starting target run : " + targetName);

            runOneSimulation(inputDirectory, run, targetName, output, pathToCSV, runArgument, scenario,
                seedArgument,
                targetStrategyArgument, currentStrategyArgument, isTargetRun, MLOGIT_SCRIPT, SIMULATION_YEARS,
                pathToAggregates
            );


            //now do variations
            for (final Map.Entry<String, AlgorithmFactory<? extends DestinationStrategy>> candidateStrategy :
                strategies.entrySet()) {
                for (int candidate_run = 0; candidate_run < maxCandidateRuns; candidate_run++) {
                    //re-read and re-initialize
                    final FileReader io = new FileReader(
                        scenarioDirectory.resolve(initializer.getKey() + ".yaml").toFile()
                    );
                    final Scenario candidateScenario = yamler.loadAs(
                        io, Scenario.class

                    );
                    io.close();
                    initializer.getValue().initialize(candidateScenario, run, candidateStrategy.getValue());
                    final String candidateName = candidateStrategy.getKey() + "_" + candidate_run;
                    output = scenarioDirectory.resolve("output").resolve(targetName).resolve(candidateName);
                    output.toFile().mkdirs();
                    inputDirectory.toFile().mkdirs();
                    writer = new FileWriter(
                        inputDirectory.resolve(targetName + "_" + candidateName + ".yaml").toFile());
                    yamler.dump(
                        candidateScenario,
                        writer
                    );
                    writer.close();

                    final long seed = random.nextLong();
                    currentStrategyArgument = candidateStrategy.getKey();
                    isTargetRun = "FALSE";
                    Logger.getGlobal()
                        .info("Starting target run : " + targetName + "   ---- candidate: " + candidateName);

                    runOneSimulation(inputDirectory,
                        seed,
                        targetName + "_" + candidateName, output, pathToCSV, runArgument, scenario,
                        Long.toString(seed),
                        targetStrategyArgument, currentStrategyArgument, isTargetRun, MLOGIT_SCRIPT, SIMULATION_YEARS,
                        pathToAggregates
                    );


                }
            }

        }
    }

    public static void runOneSimulation(
        final Path inputDirectory,
        final long seed,
        final String targetName,
        final Path output,
        final String pathToCSV,
        final String runArgument,
        final String scenario,
        final String seedArgument,
        final String targetStrategyArgument,
        final String currentStrategyArgument,
        final String isTargetRun,
        final Path mlogitScript,
        final int simulationYears,
        final Path pathToAggregatesCSV
    ) throws IOException, InterruptedException {
        final FishState state = FishStateUtilities.run(
            targetName,
            inputDirectory.resolve(targetName + ".yaml"),
            output,
            seed,
            Level.INFO.getName(),
            false,
            null,
            simulationYears,
            false,
            -1,
            null, null, null, null
        );

        //at the end I'd like a CSV like this:
        // run, scenario, seed, target-strategy,current-strategy,isTargetRun,beta_0,beta_0_sd,beta_1,beta_1_sd,....
        final String pathToRScript = mlogitScript.toAbsolutePath().toString();
        final String pathToLogbook = output.resolve("logistic_long.csv").toAbsolutePath().toString();

        final String[] arguments =
            new String[]{
                "Rscript",
                pathToRScript,
                pathToLogbook,
                pathToCSV,
                runArgument,
                scenario,
                seedArgument,
                targetStrategyArgument,
                currentStrategyArgument,
                isTargetRun
            };
        Logger.getGlobal().info(Arrays.toString(arguments));
        final Process exec = Runtime.getRuntime().exec(arguments);
        final int code = exec.waitFor();
        final FileWriter fileWriter = new FileWriter(pathToAggregatesCSV.toFile(), true);
        if (state.getYearlyDataSet().getColumn("Species 0 Landings") != null) {
            fileWriter.append(
                Double.toString(
                    state.getAverageYearlyObservation("Species 0 Landings"))
            );
        } else {
            fileWriter.append(
                Double.toString(
                    state.getAverageYearlyObservation("Dover Sole Landings"))
            );
        }
        fileWriter.append(",");


        fileWriter.append(
            Double.toString(
                state.getAverageYearlyObservation("Total Effort"))
        );
        fileWriter.append(",");

        fileWriter.append(
            Double.toString(
                state.getAverageYearlyObservation("Average Distance From Port"))
        );
        fileWriter.append(",");

        fileWriter.append(
            Double.toString(
                state.getAverageYearlyObservation("Average Number of Trips"))
        );
        fileWriter.append(",");

        fileWriter.append(
            Double.toString(
                state.getAverageYearlyObservation("Average Hours Out"))
        );
        fileWriter.append(",");

        fileWriter.append(
            Double.toString(
                state.getAverageYearlyObservation("Average Cash-Flow"))
        );
        fileWriter.append(",");

        //"landings,effort,distance,trips,hours,profits,run,target_strategy,current_strategy,scenario,isTargetRun,seed"
        fileWriter.append(runArgument);
        fileWriter.append(",");
        fileWriter.append(targetStrategyArgument);
        fileWriter.append(",");
        fileWriter.append(currentStrategyArgument);
        fileWriter.append(",");
        fileWriter.append(scenario);
        fileWriter.append(",");
        fileWriter.append(isTargetRun);
        fileWriter.append(",");
        fileWriter.append(seedArgument);

        fileWriter.append("\n");
        fileWriter.flush();
        fileWriter.close();

        switch (code) {
            case 0:
                deleteFolder(output.toFile());
                inputDirectory.resolve(targetName + ".yaml").toFile().delete();

                break;
            case 1:
                //Read the error stream then
                final String message = convertStreamToString(exec.getErrorStream());
                Logger.getGlobal().info("Swish!");
                Logger.getGlobal().info(message);
                deleteFolder(output.toFile());
                //throw new RuntimeException(message);


        }
        //normal termination, everything is fine


    }


    static String convertStreamToString(final java.io.InputStream is) {
        final java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * grabbed from
     * https://stackoverflow.com/questions/7768071/how-to-delete-directory-content-in-java
     */
    public static void deleteFolder(final File folder) {
        final File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (final File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }


    public interface ScenarioInitializer {

        void initialize(
            Scenario scenario, long seed,
            AlgorithmFactory<? extends DestinationStrategy> destinationStrategy
        );


    }

}
