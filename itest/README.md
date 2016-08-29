## Running integration tests

To run the entire integration testsuite run:

> mvn verify

To run a single combination of CDI implementation / OSGi container run e.g.:

> mvn verify -Pmatrix,weld2,felix

or

> mvn verify -Pmatrix,owb1,equinox
