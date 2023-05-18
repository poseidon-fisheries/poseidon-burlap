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

package uk.ac.ox.poseidon.burlap.experiments.indonesia;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.selfanalysis.CashFlowObjective;
import uk.ac.ox.oxfish.fisher.selfanalysis.DiscreteRandomAlgorithm;
import uk.ac.ox.oxfish.fisher.strategies.discarding.DiscardingStrategy;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.FisherStartable;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.adaptation.Actuator;
import uk.ac.ox.oxfish.utility.adaptation.ExploreImitateAdaptation;
import uk.ac.ox.oxfish.utility.adaptation.Sensor;
import uk.ac.ox.oxfish.utility.adaptation.probability.FixedProbability;

import java.util.List;
import java.util.function.Predicate;


class PeriodicUpdateDiscarding implements FisherStartable {


    private final BiMap<Class<? extends DiscardingStrategy>,
        AlgorithmFactory<? extends DiscardingStrategy>> options;


    public PeriodicUpdateDiscarding(
        List<Class<? extends DiscardingStrategy>> discards,
        List<AlgorithmFactory<? extends DiscardingStrategy>> factories
    ) {

        Preconditions.checkArgument(discards.size() == factories.size());

        options = HashBiMap.create(discards.size());
        for (int i = 0; i < discards.size(); i++) {
            options.put(discards.get(i), factories.get(i));

        }

    }


    @Override
    public void start(FishState model, Fisher fisher) {

        fisher.addBiMonthlyAdaptation(
            new ExploreImitateAdaptation<Class<? extends DiscardingStrategy>>(
                new Predicate<Fisher>() {
                    @Override
                    public boolean test(Fisher fisher1) {
                        return true;
                    }
                },
                new DiscreteRandomAlgorithm<Class<? extends DiscardingStrategy>>(
                    Lists.newArrayList(options.keySet())),
                new Actuator<Fisher, Class<? extends DiscardingStrategy>>() {
                    @Override
                    public void apply(
                        Fisher subject, Class<? extends DiscardingStrategy> policy, FishState model
                    ) {

                        subject.setDiscardingStrategy(
                            options.get(policy).apply(model)
                        );
                    }
                },
                new Sensor<Fisher, Class<? extends DiscardingStrategy>>() {
                    @Override
                    public Class<? extends DiscardingStrategy> scan(Fisher system) {
                        return system.getDiscardingStrategy().getClass();
                    }
                },
                new CashFlowObjective(60),
                new FixedProbability(.1, .8),
                new Predicate<Class<? extends DiscardingStrategy>>() {
                    @Override
                    public boolean test(Class<? extends DiscardingStrategy> aClass) {
                        return true;
                    }
                }

            )
        );


    }

    @Override
    public void turnOff(Fisher fisher) {

    }
}
