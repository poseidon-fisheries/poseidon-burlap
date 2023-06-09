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

import sim.engine.SimState;
import sim.engine.Steppable;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.heatmap.regression.extractors.ObservationExtractor;
import uk.ac.ox.oxfish.fisher.log.DiscretizedLocationMemory;
import uk.ac.ox.oxfish.fisher.log.LogisticLog;
import uk.ac.ox.oxfish.fisher.log.LogisticLogs;
import uk.ac.ox.oxfish.fisher.log.PseudoLogisticLogger;
import uk.ac.ox.oxfish.fisher.log.initializers.LogbookInitializer;
import uk.ac.ox.oxfish.geography.discretization.MapDiscretization;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.StepOrder;
import uk.ac.ox.oxfish.model.data.DiscretizationHistogrammer;
import uk.ac.ox.poseidon.burlap.strategies.LogitDestinationStrategy;

/**
 * Creates a logistic logbook
 * Created by carrknight on 2/17/17.
 */
public class LogisticLogbookInitializer implements LogbookInitializer {


    private final MapDiscretization discretization;

    private final ObservationExtractor[] commonExtractor;

    private final String[] extractorNames;
    private final int histogrammerStartYear;
    /**
     * useful to discriminate between multiple outputs
     */
    private final String identifier;
    /**
     * the object doing the actual logbooking (actually a container for the individual logbook makers)
     */
    private LogisticLogs logger;
    /**
     * an additional output of the simulation, a histogram of trips to each spot
     */
    private DiscretizationHistogrammer histogrammer;


    public LogisticLogbookInitializer(
        MapDiscretization discretization,
        ObservationExtractor[] commonExtractor,
        String[] extractorNames, int histogrammerStartYear
    ) {
        this(discretization, commonExtractor, extractorNames, histogrammerStartYear, "");
    }


    public LogisticLogbookInitializer(
        MapDiscretization discretization,
        ObservationExtractor[] commonExtractor,
        String[] extractorNames,
        int histogrammerStartYear, String identifier
    ) {
        this.discretization = discretization;
        this.commonExtractor = commonExtractor;
        this.extractorNames = extractorNames;
        this.histogrammerStartYear = histogrammerStartYear;
        this.identifier = identifier;
    }

    /**
     * this gets called by the fish-state right after the scenario has started. It's useful to set up steppables
     * or just to percolate a reference to the model
     *
     * @param model the model
     */
    @Override
    public void start(FishState model) {


        logger = new LogisticLogs();
        logger.setFileName(identifier + logger.getFileName());
        //let it build, we won't start it until it's time though
        histogrammer = new DiscretizationHistogrammer(
            discretization, false);
        histogrammer.setFileName(identifier + histogrammer.getFileName());

        model.getOutputPlugins().add(logger);
        model.getOutputPlugins().add(histogrammer);


    }

    /**
     * tell the startable to turnoff,
     */
    @Override
    public void turnOff() {

    }

    @Override
    public void add(Fisher fisher, FishState state) {


        LogisticLog log = new LogisticLog(extractorNames, fisher.getID());

        PseudoLogisticLogger pseudoLogger = new PseudoLogisticLogger(
            discretization,
            commonExtractor,
            log,
            fisher,
            state,
            state.getRandom()
        );

        fisher.addTripListener(pseudoLogger);
        //add histogrammer now or when it is time!
        if (histogrammerStartYear >= 0) { //don't do anything if the start year is negative!
            if (state.getYear() >= histogrammerStartYear)
                fisher.addTripListener(histogrammer);
            else
                state.scheduleOnceAtTheBeginningOfYear(new Steppable() {
                    @Override
                    public void step(SimState simState) {
                        fisher.addTripListener(histogrammer);
                    }
                }, StepOrder.DAWN, histogrammerStartYear);
        }
        logger.add(log);


        //todo move this somewhere less unsavory
        if (!(fisher.getDestinationStrategy() instanceof LogitDestinationStrategy))
            fisher.setDiscretizedLocationMemory(
                new DiscretizedLocationMemory(discretization));

    }
}
