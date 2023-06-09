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

package uk.ac.ox.poseidon.burlap;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.strategies.discarding.NoDiscardingFactory;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.data.collectors.FisherDailyTimeSeries;
import uk.ac.ox.oxfish.model.data.collectors.FisherYearlyTimeSeries;
import uk.ac.ox.oxfish.model.market.FlexibleAbundanceMarket;
import uk.ac.ox.oxfish.model.regs.factory.FishingSeasonFactory;
import uk.ac.ox.oxfish.model.regs.factory.MultiITQStringFactory;
import uk.ac.ox.oxfish.utility.yaml.FishYAML;
import uk.ac.ox.poseidon.burlap.scenarios.CaliforniaAbundanceScenario;
import uk.ac.ox.poseidon.burlap.scenarios.DerisoCaliforniaScenario;
import uk.ac.ox.poseidon.burlap.scenarios.SimpleCaliforniaScenario;

import java.io.FileReader;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;

/**
 * Created by carrknight on 6/26/17.
 */
public class SimpleCaliforniaScenarioTest {


    //if there is no fishing from the model, the biomass ought to go to about the right level


    @Test
    public void replicateTestSablefishTS() throws Exception {


        SimpleCaliforniaScenario scenario = new SimpleCaliforniaScenario();
        scenario.setLargeFishers(0);
        scenario.setSmallFishers(0);

        //8000t
        scenario.setExogenousSablefishCatches(8000000);

        FishState state = new FishState(System.currentTimeMillis());
        state.setScenario(scenario);
        state.start();
        while (state.getYear() < 10) {

            state.schedule.step(state);
            if (state.getDayOfTheYear() == 1)
                System.out.println(state.getTotalBiomass(state.getSpecies().get(0)) / 1000);
        }
        state.schedule.step(state);
        double finalBiomass = state.getLatestYearlyObservation("Biomass Sablefish");
        System.out.println(finalBiomass / 1000);
        Assert.assertEquals(finalBiomass / 1000, 364137.4, 1);
    }


    @Test
    public void replicateTestSablefishWithRealGeography() throws Exception {


        FishYAML yaml = new FishYAML();
        DerisoCaliforniaScenario scenario = yaml.loadAs(
            new FileReader(
                Paths.get("inputs", "tests", "deriso_comparison.yaml").toFile()),
            DerisoCaliforniaScenario.class
        );
        scenario.setDerisoFileNames("deriso.yaml");
        ;
        LinkedHashMap<String, String> exogenousCatches = new LinkedHashMap<>();
        exogenousCatches.put("Sablefish", "8000000");
        scenario.setExogenousCatches(exogenousCatches);
        scenario.setRegulationPreReset(new FishingSeasonFactory(0, true));
        scenario.setResetBiologyAtYear1(false);


        FishState state = new FishState(System.currentTimeMillis());
        state.setScenario(scenario);
        state.start();
        while (state.getYear() < 10) {

            state.schedule.step(state);
            if (state.getDayOfTheYear() == 1)
                System.out.println(state.getTotalBiomass(state.getSpecies().get(0)) / 1000);
        }
        state.schedule.step(state);
        double finalBiomass = state.getLatestYearlyObservation("Biomass Sablefish");
        System.out.println(finalBiomass / 1000);
        Assert.assertEquals(finalBiomass / 1000, 364137.4, 1);
    }

    @Test
    public void replicateTestYelloweye() throws Exception {


        SimpleCaliforniaScenario scenario = new SimpleCaliforniaScenario();
        scenario.setLargeFishers(0);
        scenario.setSmallFishers(0);

        //8000t
        scenario.setExogenousYelloweyeCatches(20000);

        FishState state = new FishState(System.currentTimeMillis());
        state.setScenario(scenario);
        state.start();
        while (state.getYear() < 10) {

            state.schedule.step(state);
            if (state.getDayOfTheYear() == 1)
                System.out.println(state.getTotalBiomass(state.getSpecies().get(1)) / 1000);
        }
        state.schedule.step(state);
        double finalBiomass = state.getLatestYearlyObservation("Biomass Yelloweye Rockfish");
        System.out.println(finalBiomass / 1000);
        Assert.assertEquals(finalBiomass / 1000, 3092.21, .01);
    }


    @Test
    public void marketStartsUpCorrectly() throws Exception {

        SimpleCaliforniaScenario scenario = new SimpleCaliforniaScenario();
        scenario.setUsePredictors(true);
        scenario.setDiscardingStrategyLarge(new NoDiscardingFactory());
        scenario.setDiscardingStrategySmall(new NoDiscardingFactory());
        MultiITQStringFactory regs = new MultiITQStringFactory();
        regs.setYearlyQuotaMaps("0:22222,1:55");
        regs.setMinimumQuotaTraded("0:100,1:5");
        scenario.setRegulationsToImposeAtStartYear(regs);
        FishState state = new FishState(System.currentTimeMillis());

        state.setScenario(scenario);
        state.start();
        while (state.getYear() < 2)
            state.schedule.step(state);
        state.schedule.step(state);

        Double trades = state.getLatestYearlyObservation("ITQ Volume Of Yelloweye Rockfish");
        System.out.println(trades);
        Assert.assertTrue(trades > 0);


    }

    public void countersAreCorrect() throws Exception {

        CaliforniaAbundanceScenario scenario = new CaliforniaAbundanceScenario();

        FishState state = new FishState(123l);
        state.setScenario(scenario);
        state.start();

        for (int i = 0; i < 366; i++) {
            double landings = 0;
            state.schedule.step(state);
            for (int age = 0; age < 60; age++) {
                // System.out.println(age);
                landings += state.getDailyDataSet()
                    .getLatestObservation("Sablefish " + FisherDailyTimeSeries.CATCHES_COLUMN_NAME + FlexibleAbundanceMarket.AGE_BIN_PREFIX + age);
            }
            assertEquals(
                landings,
                state.getDailyDataSet().getLatestObservation("Sablefish " + FisherDailyTimeSeries.CATCHES_COLUMN_NAME),
                .001
            );


        }
        double sumHours = 0;
        double sumTrips = 0;
        double sumDuration = 0;
        for (Fisher fisher : state.getFishers()) {
            double hours = fisher.getLatestYearlyObservation(FisherYearlyTimeSeries.HOURS_OUT);
            sumHours += hours;
            double trips = fisher.getLatestYearlyObservation(FisherYearlyTimeSeries.TRIPS);
            sumTrips += trips;
            double duration = fisher.getLatestYearlyObservation(FisherYearlyTimeSeries.TRIP_DURATION);
            sumDuration += duration;
            System.out.println(hours + " " + trips + " " + duration + " --> " + (hours / trips));
            assertEquals(hours / trips, duration, .0001);
        }
        System.out.println("===========================================================");
        System.out.println(sumHours + " " + sumTrips + " " + sumDuration / state.getFishers()
            .size() + " --> " + (sumHours / sumTrips));

        //they might not be equal because you need to reweigh them!


        System.out.println("===========================================================");

        double hours = state.getLatestYearlyObservation("Average Hours Out");
        double trips = state.getLatestYearlyObservation("Average Number of Trips");
        double duration = state.getLatestYearlyObservation("Average Trip Duration");
        System.out.println(hours + " " + trips + " " + duration + " --> " + (hours / trips));
        assertEquals(hours / trips, duration, .0001);

    }


}