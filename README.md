This repo contains POSEIDON code that depends on the [Brown-UMBC Reinforcement Learning and Planning (BURLAP) library](https://github.com/jmacglashan/burlap).

It contains a lot of the project-specific code that was written for POSEIDON's application to US west coast groundfish and Indonesian snapper fisheries.

See https://github.com/poseidon-fisheries/POSEIDON for the main repository where ongoing development is happening.

## Disclaimer

The code hosted here compiles, but may not run flawlessly, as it might be expecting files to be in different locations from where they now are relative to the project. Those bits should be easy to fix if needed.

Another potential issue is that some of the "algorithm factories" defined here were removed from the various lists of algorithm factories in the main POSEIDON module. This might cause issues when trying to load scenarios from YAML files. If this is required, we would suggest going back to a a version of POSEIDON which pre-dates the current split, either:

- https://github.com/poseidon-fisheries/POSEIDON/releases/tag/pre_modularisation (just before the present repo was created)
- or, https://github.com/poseidon-fisheries/POSEIDON/releases/tag/pre_modularisation (before we started splitting POSEIDON in different projects)