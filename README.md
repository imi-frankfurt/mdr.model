# MIG MDR Model

This iteration of MIG MDR Model is based on the fundamental concepts of the [Samply.MDR](https://bitbucket.org/medicalinformatics/mig.samply.mdr.gui).
Based on the experience gained, the concept was revised and redeveloped, with particular emphasis on
the separation of the backend and frontend.

MIG MDR model serves as a layer between mdr.dal and mdr.rest.

## Build

Use maven to build the `jar` file:

```
mvn clean package
```