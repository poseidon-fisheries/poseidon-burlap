/*
 *     POSEIDON, an agent-based model of fisheries
 *     Copyright (C) 2018  CoHESyS Lab cohesys.lab@gmail.com
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

package uk.ac.ox.poseidon.burlap.experiments.california;

import java.io.IOException;
import java.nio.file.Paths;

import static uk.ac.ox.poseidon.burlap.experiments.california.CaliCatchCalibration.YEARS_PER_RUN;
import static uk.ac.ox.poseidon.burlap.experiments.california.CaliCatchCalibration.runMultipleTimesToBuildHistogram;

public class CaliCatchExperiments {


    public static void main(String[] args) throws IOException {


        runMultipleTimesToBuildHistogram("deriso_hillclimber2",
            null,
            Paths.get(
                "docs",
                "paper3_dts",
                "mark2",
                "exploratory",
                "calibration",
                "histograms"
            ),
            YEARS_PER_RUN, CaliCatchCalibration.RUNS
        );
    }
}
