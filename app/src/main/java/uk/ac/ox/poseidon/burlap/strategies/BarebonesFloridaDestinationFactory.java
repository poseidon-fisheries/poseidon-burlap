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

package uk.ac.ox.poseidon.burlap.strategies;

import uk.ac.ox.oxfish.fisher.heatmap.regression.extractors.ObservationExtractor;
import uk.ac.ox.oxfish.fisher.heatmap.regression.extractors.PeriodHabitBooleanExtractor;
import uk.ac.ox.oxfish.geography.discretization.CentroidMapFileFactory;
import uk.ac.ox.oxfish.geography.discretization.MapDiscretization;

import java.nio.file.Paths;

/**
 * The simplest possible regression we fit to WFS data
 * Created by carrknight on 4/18/17.
 */
public class BarebonesFloridaDestinationFactory extends BarebonesLogitDestinationFactory
{


    //variables here are the handliner default
    {
        ((CentroidMapFileFactory) discretizer).setFilePath(Paths.get("temp_wfs", "areas.txt").toString());
        ((CentroidMapFileFactory) discretizer).setxColumnName("eastings");
        ((CentroidMapFileFactory) discretizer).setyColumnName("northings");
    }


    @Override
    public ObservationExtractor buildHabitExtractor(MapDiscretization discretization, final int period) {
        return new PeriodHabitBooleanExtractor(discretization, period);
    }


}
