package uk.ac.ox.poseidon.burlap.experiments.mera;

import uk.ac.ox.oxfish.model.AdditionalStartable;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.yaml.FishYAML;
import uk.ac.ox.poseidon.burlap.experiments.indonesia.RejectionSampling;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * we run one scenario, 100 times for 1 year. This is to show the match in landings between MERA and POSEIDON
 */
public class Mera718OneYearExample {


    private final static Path MAIN_FILE = Paths.get(
        "docs/mera_hub/slice3_yesgeography_onespecies/hotstarts/1/optimized.yaml");
    private final static Path OUTPUT_FOLDER = MAIN_FILE.getParent().resolve("oneyearexample");

    public static void main(String[] args) throws IOException {

        OUTPUT_FOLDER.toFile().mkdir();

        FishYAML yaml = new FishYAML();
        for (int seed = 0; seed < 100; seed++) {
            Path scenarioToRun = OUTPUT_FOLDER.resolve("inputs").resolve(seed + ".yaml");
            Files.copy(
                MAIN_FILE,
                scenarioToRun
            );

            LinkedHashMap<String, AlgorithmFactory<? extends AdditionalStartable>> policyMap = new LinkedHashMap<>();
            policyMap.put("nothing_" + seed, state -> model -> {

            });
            RejectionSampling.runOneAcceptedScenario(
                scenarioToRun,
                0,
                1,
                seed,
                OUTPUT_FOLDER,
                policyMap,
                yaml.loadAs(
                    new FileReader(
                        MeraOMHotstartsCalibration.MAIN_DIRECTORY.resolve("full_columns_to_print.yaml").toFile()),
                    List.class
                ),
                false,
                null,
                null
            );
        }


    }

}
