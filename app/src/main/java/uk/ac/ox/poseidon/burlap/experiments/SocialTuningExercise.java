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

import uk.ac.ox.oxfish.fisher.heatmap.regression.factory.*;
import uk.ac.ox.oxfish.fisher.heatmap.regression.numerical.GeographicalRegression;
import uk.ac.ox.oxfish.fisher.strategies.destination.DestinationStrategy;
import uk.ac.ox.oxfish.fisher.strategies.destination.factory.*;
import uk.ac.ox.oxfish.geography.discretization.SquaresMapDiscretizerFactory;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.data.collectors.DataColumn;
import uk.ac.ox.oxfish.model.data.factory.ExponentialMovingAverageFactory;
import uk.ac.ox.oxfish.model.scenario.PolicyScripts;
import uk.ac.ox.oxfish.model.scenario.PrototypeScenario;
import uk.ac.ox.oxfish.model.scenario.Scenario;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.FishStateUtilities;
import uk.ac.ox.oxfish.utility.Pair;
import uk.ac.ox.oxfish.utility.adaptation.probability.factory.FixedProbabilityFactory;
import uk.ac.ox.oxfish.utility.adaptation.probability.factory.SocialAnnealingProbabilityFactory;
import uk.ac.ox.oxfish.utility.bandit.factory.EpsilonGreedyBanditFactory;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;
import uk.ac.ox.oxfish.utility.parameters.UniformDoubleParameter;
import uk.ac.ox.oxfish.utility.yaml.FishYAML;
import uk.ac.ox.poseidon.burlap.scenarios.CaliforniaAbundanceScenario;
import uk.ac.ox.poseidon.burlap.strategies.SoftmaxBanditFactory;
import uk.ac.ox.poseidon.burlap.strategies.UCB1BanditFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs social tuning many times, looking for the right parameter
 * Created by carrknight on 8/30/16.
 */
public class SocialTuningExercise {


    public static final Path MAIN_DIRECTORY = Paths.get("inputs", "algorithms");
    public static final int NUMBER_OF_EXPERIMENTS = 100;
    private final static int YEARS_TO_RUN = 5;

    public static void main(final String[] args) throws IOException {


        fine();


        front();

        chaser();

        reversal();

        parameterSweep("fine.yaml", YEARS_TO_RUN, 0L, 1);

    }

    public static void fine() throws IOException {
        defaults("fine.yaml", "_fine", YEARS_TO_RUN, 0, null);
        batchRun("fine.yaml", "_fine",
            pair -> ((SocialTuningRegressionFactory) ((HeatmapDestinationFactory)
                ((PrototypeScenario) pair.getFirst()).getDestinationStrategy()).getRegression()).setNested(
                pair.getSecond()
            ), YEARS_TO_RUN, 0, null
        );
    }

    public static void front() throws IOException {
        defaults("no_regrowth.yaml", "_noregrowth", YEARS_TO_RUN, 0, null);

        batchRun("no_regrowth.yaml", "_noregrowth",
            pair -> ((SocialTuningRegressionFactory) ((HeatmapDestinationFactory)
                ((PrototypeScenario) pair.getFirst()).getDestinationStrategy()).getRegression()).setNested(
                pair.getSecond()
            ), YEARS_TO_RUN, 0, null
        );
    }

    public static void chaser() throws IOException {
        defaults("chaser_gas.yaml", "_chasergas", YEARS_TO_RUN, 0, null);
        batchRun("chaser_gas.yaml", "_chasergas",
            pair -> ((SocialTuningRegressionFactory) ((HeatmapDestinationFactory)
                ((PrototypeScenario) pair.getFirst()).getDestinationStrategy()).getRegression()).setNested(
                pair.getSecond()
            ), YEARS_TO_RUN, 0, null
        );
    }

    public static void reversal() throws IOException {
        defaults("gear.yaml", "_gear", 3, 2, MAIN_DIRECTORY.resolve("policy.yaml"));
        batchRun("gear.yaml", "_gear",
            pair -> ((SocialTuningRegressionFactory) ((HeatmapDestinationFactory)
                ((PrototypeScenario) pair.getFirst()).getDestinationStrategy()).getRegression()).setNested(
                pair.getSecond()
            ), 3, 2, MAIN_DIRECTORY.resolve("policy.yaml")
        );
    }

    /**
     * sweep through the epsilon greedy to show the effect on parameters
     */
    public static void parameterSweep(
        final String inputFile,
        final int yearsToRun,
        final long randomSeed,
        final int runsPerParameter
    ) throws IOException {

        final BanditDestinationFactory epsilonGreedy = new BanditDestinationFactory();
        final ExponentialMovingAverageFactory ema = new ExponentialMovingAverageFactory();
        final EpsilonGreedyBanditFactory greedy = new EpsilonGreedyBanditFactory();
        epsilonGreedy.setBandit(greedy);
        epsilonGreedy.setAverage(ema);
        final SquaresMapDiscretizerFactory discretizer = new SquaresMapDiscretizerFactory();
        epsilonGreedy.setDiscretizer(discretizer);
        discretizer.setHorizontalSplits(new FixedDoubleParameter(49));
        discretizer.setVerticalSplits(new FixedDoubleParameter(49));


        final FileWriter writer = new FileWriter(MAIN_DIRECTORY.resolve("sweep.csv").toFile());
        writer.append("alpha,epsilon,performance" + "\n");
        for (double alpha = 0; alpha <= 1; alpha += .01)
            for (double epsilon = 0; epsilon <= 1; epsilon += .01) {
                //read up the scenario
                alpha = FishStateUtilities.round(alpha);
                epsilon = FishStateUtilities.round(epsilon);
                ema.setAlpha(new FixedDoubleParameter(alpha));
                greedy.setExplorationRate(new FixedDoubleParameter(epsilon));

                final FishYAML yaml = new FishYAML();
                final String inputScenario = String.join("\n", Files.readAllLines(
                    MAIN_DIRECTORY.resolve(inputFile)));
                final PrototypeScenario scenario = yaml.loadAs(inputScenario, PrototypeScenario.class);
                scenario.setDestinationStrategy(epsilonGreedy);


                final DoubleSummaryStatistics performance = new DoubleSummaryStatistics();


                for (int run = 0; run < runsPerParameter; run++) {
                    final FishState state = new FishState(randomSeed + run);
                    state.setScenario(scenario);
                    state.start();
                    while (state.getYear() < yearsToRun)
                        state.schedule.step(state);
                    double total = 0;
                    final DataColumn cashColumn = state.getYearlyDataSet().getColumn("Average Cash-Flow");
                    for (int i = 0; i < cashColumn.size(); i++)
                        total += cashColumn.get(i);
                    performance.accept(total);
                }
                writer.append(alpha + "," + epsilon + "," + performance.getAverage() + "\n");
                writer.flush();
                Logger.getGlobal().info(alpha + "," + epsilon + "," + performance.getAverage() + "\n");


            }

        writer.close();

    }

    public static void defaults(
        final String inputFile, final String outputName,
        final int yearsToRun,
        final int firstValidYear,
        final Path policyFile
    ) throws IOException {


        final HashMap<String, AlgorithmFactory<? extends DestinationStrategy>> strategies = new LinkedHashMap<>();


        final BanditDestinationFactory ucb1 = new BanditDestinationFactory();
        ExponentialMovingAverageFactory ema = new ExponentialMovingAverageFactory();
        ema.setAlpha(new FixedDoubleParameter(0.273822));
        final UCB1BanditFactory bandit = new UCB1BanditFactory();
        bandit.setMinimumReward(new FixedDoubleParameter(0));
        bandit.setMaximumReward(new FixedDoubleParameter(12));
        ucb1.setBandit(bandit);
        ucb1.setAverage(ema);
        final BanditDestinationFactory ucb1Bad = new BanditDestinationFactory();
        ucb1Bad.setAverage(ema);
        ucb1Bad.setBandit(bandit);
        SquaresMapDiscretizerFactory discretizer = new SquaresMapDiscretizerFactory();
        ucb1Bad.setDiscretizer(discretizer);
        discretizer.setHorizontalSplits(new FixedDoubleParameter(49));
        discretizer.setVerticalSplits(new FixedDoubleParameter(49));
        strategies.put("bandit=ucb1", ucb1);
        strategies.put("bandit=ucb1-bad", ucb1Bad);


        final BanditDestinationFactory epsilonGreedy = new BanditDestinationFactory();
        ema = new ExponentialMovingAverageFactory();
        ema.setAlpha(new FixedDoubleParameter(.97));
        final EpsilonGreedyBanditFactory greedy = new EpsilonGreedyBanditFactory();
        greedy.setExplorationRate(new FixedDoubleParameter(0.009583));
        epsilonGreedy.setBandit(greedy);
        epsilonGreedy.setAverage(ema);
        final BanditDestinationFactory epsilonBad = new BanditDestinationFactory();
        epsilonBad.setBandit(greedy);
        epsilonBad.setAverage(ema);
        discretizer = new SquaresMapDiscretizerFactory();
        epsilonBad.setDiscretizer(discretizer);
        discretizer.setHorizontalSplits(new FixedDoubleParameter(49));
        discretizer.setVerticalSplits(new FixedDoubleParameter(49));
        strategies.put("bandit=epsilon", epsilonGreedy);
        strategies.put("bandit=epsilon-bad", epsilonBad);


        final BanditDestinationFactory softmax = new BanditDestinationFactory();
        ema = new ExponentialMovingAverageFactory();
        ema.setAlpha(new FixedDoubleParameter(0.941162));
        softmax.setAverage(ema);
        final SoftmaxBanditFactory algorithm = new SoftmaxBanditFactory();
        softmax.setBandit(algorithm);
        //temperature is never below 1 and it never decays below 1 so that these parameters
        //that the optimizer found means that temperature and decay are not used
        algorithm.setInitialTemperature(new FixedDoubleParameter(0.000138));
        algorithm.setTemperatureDecay(new FixedDoubleParameter(0.000138));
        final BanditDestinationFactory softmaxBad = new BanditDestinationFactory();
        softmaxBad.setAverage(ema);
        softmaxBad.setBandit(algorithm);
        discretizer = new SquaresMapDiscretizerFactory();
        softmaxBad.setDiscretizer(discretizer);
        discretizer.setHorizontalSplits(new FixedDoubleParameter(49));
        discretizer.setVerticalSplits(new FixedDoubleParameter(49));
        strategies.put("bandit=softmax", softmax);
        strategies.put("bandit=softmax-bad", softmaxBad);


        strategies.put("fixed", new RandomFavoriteDestinationFactory());
        strategies.put("random", new RandomThenBackToPortFactory());


        final PerTripImitativeDestinationFactory eei = new PerTripImitativeDestinationFactory();
        eei.setProbability(new FixedProbabilityFactory(.48, 1));
        eei.setStepSize(new FixedDoubleParameter(20));
        strategies.put("eei", eei);


        final GravitationalSearchDestinationFactory gsa = new GravitationalSearchDestinationFactory();
        gsa.setExplorationSize(new FixedDoubleParameter(1));
        gsa.setInitialSpeed(new FixedDoubleParameter(-10));
        gsa.setGravitationalConstant(new FixedDoubleParameter(10));
        strategies.put("gsa", gsa);

        final PerTripParticleSwarmFactory pso = new PerTripParticleSwarmFactory();
        pso.setMemoryWeight(new FixedDoubleParameter(.37));
        pso.setExplorationProbability(new FixedDoubleParameter(0.001661));
        pso.setInertia(new FixedDoubleParameter(0.733));
        pso.setFriendWeight(new FixedDoubleParameter(0.1));
        strategies.put("pso", pso);

        final PerTripImitativeDestinationFactory annealing = new PerTripImitativeDestinationFactory();
        annealing.setProbability(new SocialAnnealingProbabilityFactory());
        annealing.setBacktracksOnBadExploration(false);
        strategies.put("annealing", annealing);


        for (final Map.Entry<String, AlgorithmFactory<? extends DestinationStrategy>>
            strategy : strategies.entrySet()) {

            final StringBuilder output = new StringBuilder();
            output.append("cash");

            Logger.getGlobal().setLevel(Level.INFO);
            Logger.getGlobal().info("starting " + strategy.getKey());
            final String inputScenario = String.join("\n", Files.readAllLines(
                MAIN_DIRECTORY.resolve(inputFile)));

            for (int experiment = 1; experiment < NUMBER_OF_EXPERIMENTS; experiment++) {
                Logger.getGlobal().info("Starting experiment " + experiment);
                final FishYAML yaml = new FishYAML();
                final Scenario scenario = yaml.loadAs(inputScenario, Scenario.class);

                if (scenario instanceof PrototypeScenario)
                    ((PrototypeScenario) scenario).setDestinationStrategy(strategy.getValue());
                else {
                    assert scenario instanceof CaliforniaAbundanceScenario;
                    ((CaliforniaAbundanceScenario) scenario).setDestinationStrategy(strategy.getValue());
                }

                final FishState state = new FishState(experiment);
                state.setScenario(scenario);

                //if there is a policy script, read it now:
                if (policyFile != null) {
                    final String policyScriptString = new String(Files.readAllBytes(policyFile));
                    final PolicyScripts scripts = yaml.loadAs(policyScriptString, PolicyScripts.class);
                    state.registerStartable(scripts);
                }

                state.start();
                while (state.getYear() < yearsToRun)
                    state.schedule.step(state);
                output.append("\n");
                double total = 0;
                final DataColumn cashColumn = state.getYearlyDataSet().getColumn("Average Cash-Flow");
                for (int i = firstValidYear; i < cashColumn.size(); i++)
                    total += cashColumn.get(i);
                output.append(total);
                System.out.println("total cash: " + total);


            }

            Files.write(MAIN_DIRECTORY.resolve(strategy.getKey() + outputName + ".csv"), output.toString().getBytes());


        }


    }


    public static void batchRun(
        final String inputFile, final String outputName,
        final Consumer<Pair<Scenario, AlgorithmFactory<? extends GeographicalRegression<Double>>>>
            strategyAssigner, final int yearsToRun, final int firstValidYear,
        final Path policyFile
    ) throws IOException {


        final HashMap<String, AlgorithmFactory<? extends GeographicalRegression<Double>>> strategies = new LinkedHashMap<>();
        final HashMap<String, String[]> headers = new LinkedHashMap<>();

        //nearest neighbor
        final CompleteNearestNeighborRegressionFactory nn = new CompleteNearestNeighborRegressionFactory();
        nn.setDistanceFromPortBandwidth(new UniformDoubleParameter(1, 1000));
        nn.setHabitatBandwidth(new UniformDoubleParameter(1, 1000));
        nn.setTimeBandwidth(new UniformDoubleParameter(1, 1000));
        nn.setxBandwidth(new UniformDoubleParameter(1, 1000));
        nn.setyBandwidth(new UniformDoubleParameter(1, 1000));
        nn.setNeighbors(new UniformDoubleParameter(1, 10));
        strategies.put("nn", nn);
        headers.put("nn", new String[]{"time", "x", "y", "distance", "habitat", "neighbors"});

        //kalman
        final SimpleKalmanRegressionFactory kalman = new SimpleKalmanRegressionFactory();
        kalman.setDistancePenalty(new UniformDoubleParameter(1, 100));
        kalman.setEvidenceUncertainty(new UniformDoubleParameter(1, 100));
        kalman.setFishingHerePenalty(new UniformDoubleParameter(-0.5, 2));
        kalman.setInitialUncertainty(new FixedDoubleParameter(10000));
        kalman.setOptimism(new UniformDoubleParameter(-2, 2));
        kalman.setDrift(new UniformDoubleParameter(1, 100));
        strategies.put("kalman", kalman);
        headers.put("kalman", new String[]{"distance", "evidence", "drift", "optimism", "penalty"});

        //gwr
        final GeographicallyWeightedRegressionFactory gwr = new GeographicallyWeightedRegressionFactory();
        gwr.setExponentialForgetting(new UniformDoubleParameter(.8, 1));
        gwr.setRbfBandwidth(new UniformDoubleParameter(.1, 50));
        strategies.put("gwr", gwr);
        headers.put("gwr", new String[]{"forgetting", "bandwidth"});

        //good-bad regression
        final GoodBadRegressionFactory goodBad = new GoodBadRegressionFactory();
        goodBad.setBadAverage(new UniformDoubleParameter(-20, 0));
        goodBad.setGoodAverage(new UniformDoubleParameter(10, 30));
        goodBad.setStandardDeviation(new UniformDoubleParameter(10, 30));
        goodBad.setDistancePenalty(new UniformDoubleParameter(.1, 50));
        strategies.put("goodBad", goodBad);
        headers.put("goodBad", new String[]{"bad", "good", "std", "distance"});

        //rbf
        final DefaultKernelRegressionFactory rbf = new DefaultKernelRegressionFactory();
        rbf.setTimeBandwidth(new UniformDoubleParameter(100, 100000));
        rbf.setNumberOfObservations(new FixedDoubleParameter(100));
        rbf.setxBandwidth(new UniformDoubleParameter(50, 500));
        rbf.setyBandwidth(new UniformDoubleParameter(50, 500));
        rbf.setDistanceFromPortBandwidth(new UniformDoubleParameter(50, 500));
        rbf.setHabitatBandwidth(new UniformDoubleParameter(50, 500));
        rbf.setRbfKernel(true);
        strategies.put("rbf", rbf);
        headers.put("rbf", new String[]{"x", "y", "distance", "habitat", "time"});

        //epa
        final DefaultKernelRegressionFactory epa = new DefaultKernelRegressionFactory();
        epa.setTimeBandwidth(new UniformDoubleParameter(100, 100000));
        epa.setNumberOfObservations(new FixedDoubleParameter(100));
        epa.setxBandwidth(new UniformDoubleParameter(50, 500));
        epa.setyBandwidth(new UniformDoubleParameter(50, 500));
        epa.setDistanceFromPortBandwidth(new UniformDoubleParameter(50, 500));
        epa.setHabitatBandwidth(new UniformDoubleParameter(50, 500));
        epa.setRbfKernel(false);
        strategies.put("epa", epa);
        headers.put("epa", new String[]{"x", "y", "distance", "habitat", "time"});


        //kernel
        final DefaultRBFKernelTransductionFactory kernel = new DefaultRBFKernelTransductionFactory();
        kernel.setDistanceFromPortBandwidth(new UniformDoubleParameter(1, 200));
        kernel.setHabitatBandwidth(new UniformDoubleParameter(1, 200));
        kernel.setxBandwidth(new UniformDoubleParameter(1, 200));
        kernel.setyBandwidth(new UniformDoubleParameter(1, 200));
        kernel.setForgettingFactor(new FixedDoubleParameter(.95));
        strategies.put("kernel", kernel);
        headers.put("kernel", new String[]{"x", "y", "distance", "habitat"});


        for (final Map.Entry<String, AlgorithmFactory<? extends GeographicalRegression<Double>>>
            strategy : strategies.entrySet()) {

            final int numberOfParameters = headers.get(strategy.getKey()).length;
            final StringBuilder output = new StringBuilder();
            for (final String parameter : headers.get(strategy.getKey())) {
                output.append(parameter).append(",");
            }
            output.append("cash");

            Logger.getGlobal().setLevel(Level.INFO);
            Logger.getGlobal().info("starting " + strategy.getKey());
            final String inputScenario = String.join("\n", Files.readAllLines(
                MAIN_DIRECTORY.resolve(inputFile)));

            for (int experiment = 1; experiment < NUMBER_OF_EXPERIMENTS; experiment++) {
                Logger.getGlobal().info("Starting experiment " + experiment);
                final FishYAML yaml = new FishYAML();
                final Scenario scenario = yaml.loadAs(inputScenario, Scenario.class);

                strategyAssigner.accept(new Pair<>(scenario, strategy.getValue()));


                final FishState state = new FishState(experiment);
                state.setScenario(scenario);

                //if there is a policy script, read it now:
                if (policyFile != null) {

                    final String policyScriptString = new String(Files.readAllBytes(policyFile));
                    final PolicyScripts scripts = yaml.loadAs(policyScriptString, PolicyScripts.class);
                    state.registerStartable(scripts);
                }
                state.start();
                while (state.getYear() < yearsToRun)
                    state.schedule.step(state);
                output.append("\n");
                for (int i = 0; i < numberOfParameters; i++) {
                    output.append(
                        state.getYearlyDataSet().getLatestObservation("Average Heatmap Parameter " + i)
                    ).append(",");

                }

                double total = 0;
                final DataColumn cashColumn = state.getYearlyDataSet().getColumn("Average Cash-Flow");
                for (int i = firstValidYear; i < cashColumn.size(); i++)
                    total += cashColumn.get(i);
                output.append(total);


            }

            System.out.println(MAIN_DIRECTORY.resolve(strategy.getKey() + outputName + ".csv").toAbsolutePath());
            Files.write(MAIN_DIRECTORY.resolve(strategy.getKey() + outputName + ".csv"), output.toString().getBytes());


        }
    }


}
