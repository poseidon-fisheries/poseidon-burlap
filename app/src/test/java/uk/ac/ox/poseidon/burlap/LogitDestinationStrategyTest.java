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

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Envelope;
import ec.util.MersenneTwisterFast;
import org.jfree.util.Log;
import org.junit.Test;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomVectorField;
import sim.field.grid.ObjectGrid2D;
import uk.ac.ox.oxfish.biology.ConstantLocalBiology;
import uk.ac.ox.oxfish.biology.EmptyLocalBiology;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.heatmap.regression.extractors.GridXExtractor;
import uk.ac.ox.oxfish.fisher.heatmap.regression.extractors.InterceptExtractor;
import uk.ac.ox.oxfish.fisher.heatmap.regression.extractors.ObservationExtractor;
import uk.ac.ox.oxfish.fisher.strategies.destination.FavoriteDestinationStrategy;
import uk.ac.ox.oxfish.geography.EquirectangularDistance;
import uk.ac.ox.oxfish.geography.NauticalMap;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.geography.discretization.MapDiscretization;
import uk.ac.ox.oxfish.geography.discretization.SquaresMapDiscretizer;
import uk.ac.ox.oxfish.geography.habitat.TileHabitat;
import uk.ac.ox.oxfish.geography.pathfinding.StraightLinePathfinder;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.poseidon.burlap.strategies.LogitDestinationStrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * The logit regression has come to town.
 * Created by carrknight on 12/6/16.
 */
public class LogitDestinationStrategyTest {


    public static FishState generateSimple4x4Map() {
        final ObjectGrid2D grid2D = new ObjectGrid2D(4, 4);
        //2x2, first column sea, second  column land
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                grid2D.field[i][j] = new SeaTile(i, j, -100, new TileHabitat(0d));

        final GeomGridField rasterBathymetry = new GeomGridField(grid2D);
        rasterBathymetry.setMBR(new Envelope(0, 1, 0, 1));

        //great
        final NauticalMap map = new NauticalMap(rasterBathymetry, new GeomVectorField(),
            new EquirectangularDistance(0.0, 1), new StraightLinePathfinder()
        );
        return initModel(map);
    }

    private static FishState initModel(final NauticalMap map) {
        final FishState model = mock(FishState.class, RETURNS_DEEP_STUBS);
        when(model.getMap()).thenReturn(map);
        when(model.getStepsPerDay()).thenReturn(1);
        when(model.getHoursPerStep()).thenReturn(24d);
        return model;
    }

    @Test
    public void logitDestinationStrategy() throws Exception {

        Log.info("logit regression  is tasked to split  and decide over a 4x4 map");

        MersenneTwisterFast random = new MersenneTwisterFast();

        FishState state = generateSimple4x4Map();
        MapDiscretization discretization = new MapDiscretization(new SquaresMapDiscretizer(0, 1));
        discretization.discretize(state.getMap());
        //make sure the discretization is correct
        assertTrue(discretization.isValid(0));
        assertTrue(discretization.isValid(1));
        assertEquals(2, discretization.getNumberOfGroups());
        for (int i = 0; i < 100; i++) {
            SeaTile tile = discretization.getGroup(0).get(random.nextInt(discretization.getGroup(0).size()));
            assertTrue(tile.getGridX() <= 1);
        }

        //create the logistic
        //give it more betas than necessary, it will be okay
        //0 has about 75% of being selected  compared to 1
        double[][] beta = new double[3][];
        beta[0] = new double[]{1};
        beta[1] = new double[]{0};
        beta[2] = new double[]{-1}; //should get ignored

        ObservationExtractor[][] extractors = new ObservationExtractor[3][];
        extractors[0] = new ObservationExtractor[]{
            new InterceptExtractor(1d)
        };
        extractors[1] = extractors[0];
        extractors[2] = null;

        LogitDestinationStrategy strategy = new LogitDestinationStrategy(
            beta, extractors,
            Lists.newArrayList(0, 1, 2),
            discretization,
            new FavoriteDestinationStrategy(state.getMap().getSeaTile(3, 3)),
            random,
            false, false
        );

        int counter = 0;
        for (int i = 0; i < 1000; i++) {
            Fisher fisher = mock(Fisher.class);
            when(fisher.isAllowedAtSea()).thenReturn(true);

            strategy.adapt(state, random, fisher);
            SeaTile tile = strategy.getCurrentTarget();
            if (tile.getGridX() <= 1)
                counter++;
        }

        System.out.println(counter);
        assertTrue(counter > 600);
        assertTrue(counter < 900);


    }

    @Test
    public void avoidWastelands() throws Exception {

        //0 would have a higher chance of being selected but it's composed exclusively of wastelands!

        Log.info("logit regression  is tasked to split  and decide over a 4x4 map");

        MersenneTwisterFast random = new MersenneTwisterFast();

        FishState state = generateSimple4x4Map();
        MapDiscretization discretization = new MapDiscretization(new SquaresMapDiscretizer(0, 1));
        discretization.discretize(state.getMap());
        //make sure the discretization is correct
        assertTrue(discretization.isValid(0));
        assertTrue(discretization.isValid(1));
        assertEquals(2, discretization.getNumberOfGroups());
        for (int i = 0; i < 100; i++) {
            SeaTile tile = discretization.getGroup(0).get(random.nextInt(discretization.getGroup(0).size()));
            assertTrue(tile.getGridX() <= 1);
        }


        for (SeaTile tile : discretization.getGroup(0)) {
            tile.setBiology(new EmptyLocalBiology());
        }
        for (SeaTile tile : discretization.getGroup(1)) {
            tile.setBiology(new ConstantLocalBiology(100));
        }
        //create the logistic
        //give it more betas than necessary, it will be okay
        //0 has about 75% of being selected  compared to 1
        double[][] beta = new double[3][];
        beta[0] = new double[]{1};
        beta[1] = new double[]{0};
        beta[2] = new double[]{-1}; //should get ignored

        ObservationExtractor[][] extractors = new ObservationExtractor[3][];
        extractors[0] = new ObservationExtractor[]{
            new InterceptExtractor(1d)
        };
        extractors[1] = extractors[0];
        extractors[2] = null;

        LogitDestinationStrategy strategy = new LogitDestinationStrategy(
            beta, extractors,
            Lists.newArrayList(0, 1, 2),
            discretization,
            new FavoriteDestinationStrategy(state.getMap().getSeaTile(3, 3)),
            random,
            false, true
        );

        int counter = 0;
        for (int i = 0; i < 1000; i++) {
            Fisher fisher = mock(Fisher.class);
            when(fisher.isAllowedAtSea()).thenReturn(true);

            strategy.adapt(state, random, fisher);
            SeaTile tile = strategy.getCurrentTarget();
            if (tile.getGridX() <= 1)
                counter++;
        }

        System.out.println(counter);
        assertEquals(counter, 0);


    }

    @Test
    public void flipOrderOfSites() throws Exception {


        Log.info("same logistic problem, this time the order of columns is different");

        MersenneTwisterFast random = new MersenneTwisterFast();

        FishState state = generateSimple4x4Map();
        MapDiscretization discretization = new MapDiscretization(new SquaresMapDiscretizer(0, 1));
        discretization.discretize(state.getMap());
        //make sure the discretization is correct
        assertTrue(discretization.isValid(0));
        assertTrue(discretization.isValid(1));
        assertEquals(2, discretization.getNumberOfGroups());
        for (int i = 0; i < 100; i++) {
            SeaTile tile = discretization.getGroup(0).get(random.nextInt(discretization.getGroup(0).size()));
            assertTrue(tile.getGridX() <= 1);
        }

        //create the logistic
        //give it more betas than necessary, it will be okay
        //0 has about 75% of being selected  compared to 1
        double[][] beta = new double[3][];
        beta[0] = new double[]{1};
        beta[1] = new double[]{0};
        beta[2] = new double[]{-1}; //should get ignored

        ObservationExtractor[][] extractors = new ObservationExtractor[3][];
        extractors[0] = new ObservationExtractor[]{
            new InterceptExtractor(1d)
        };
        extractors[1] = extractors[0];
        extractors[2] = null;

        LogitDestinationStrategy strategy = new LogitDestinationStrategy(
            beta, extractors,
            Lists.newArrayList(1, 0, 2),
            discretization,
            new FavoriteDestinationStrategy(state.getMap().getSeaTile(3, 3)),
            random,
            false, false
        );

        int counter = 0;
        for (int i = 0; i < 10000; i++) {
            Fisher fisher = mock(Fisher.class);
            when(fisher.isAllowedAtSea()).thenReturn(true);
            strategy.adapt(state, random, fisher);
            SeaTile tile = strategy.getCurrentTarget();
            if (tile.getGridX() <= 1)
                counter++;
        }

        System.out.println(counter);
        assertTrue(counter > 1000);
        assertTrue(counter < 3000);

    }

    @Test
    public void tileExtractorWorks() throws Exception {
        Log.info("logit regression extractor is a function of the X of the seatile, making the second group more likely");

        MersenneTwisterFast random = new MersenneTwisterFast();

        FishState state = generateSimple4x4Map();
        MapDiscretization discretization = new MapDiscretization(new SquaresMapDiscretizer(0, 1));
        discretization.discretize(state.getMap());
        //make sure the discretization is correct
        assertTrue(discretization.isValid(0));
        assertTrue(discretization.isValid(1));
        assertEquals(2, discretization.getNumberOfGroups());
        for (int i = 0; i < 100; i++) {
            SeaTile tile = discretization.getGroup(0).get(random.nextInt(discretization.getGroup(0).size()));
            assertTrue(tile.getGridX() <= 1);
        }

        //create the logistic
        //give it more betas than necessary, it will be okay
        //0 is unlikely to be chosen
        double[][] beta = new double[3][];
        beta[0] = new double[]{1};
        beta[1] = new double[]{1};
        beta[2] = new double[]{-1}; //should get ignored

        ObservationExtractor[][] extractors = new ObservationExtractor[3][];
        extractors[0] = new ObservationExtractor[]{
            new GridXExtractor()
        };
        extractors[1] = extractors[0];
        extractors[2] = null;

        int counter = 0;

        LogitDestinationStrategy strategy = new LogitDestinationStrategy(
            beta, extractors,
            Lists.newArrayList(1, 0, 2),
            discretization,
            new FavoriteDestinationStrategy(state.getMap().getSeaTile(3, 3)),
            random,
            false, false
        );

        for (int i = 0; i < 1000; i++) {
            Fisher fisher = mock(Fisher.class);
            when(fisher.isAllowedAtSea()).thenReturn(true);
            strategy.adapt(state, random, fisher);
            SeaTile tile = strategy.getCurrentTarget();
            if (tile.getGridX() <= 1)
                counter++;
        }

        System.out.println(counter);
        assertTrue(counter > 50);
        assertTrue(counter < 200);
    }
}