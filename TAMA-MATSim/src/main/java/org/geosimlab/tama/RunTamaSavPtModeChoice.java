package org.geosimlab.tama;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

//import org.matsim.contrib.cadyts.car.CadytsCarModule;
//import org.matsim.contrib.cadyts.car.CadytsContext;
//import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
//import org.matsim.contrib.cadyts.general.CadytsScoring;

//import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;

import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup.OperationalScheme;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.optimizer.insertion.ExtensiveInsertionSearchParams;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.roadpricing.RoadPricingConfigGroup;
import org.matsim.contrib.roadpricing.RoadPricingModule;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

import org.matsim.drtSpeedUp.DrtSpeedUpConfigGroup;
import org.matsim.drtSpeedUp.MultiModeDrtSpeedUpModule;

/**
 * @author Golan Ben-Dor
 */
public class RunTamaSavPtModeChoice {
	
	//final public static String INPUT_FOLDER = "E:\\geosimlab\\MATSim-TAMA\\MATSim-TAMA DRT Input\\";
	//final public static String OUTPUT_FOLDER = "E:\\geosimlab\\MATSim-TAMA\\MATSim-TAMA DRT Output\\";

	final public static String INPUT_FOLDER = "/tmp/tama-input/";
	final public static String OUTPUT_FOLDER = "/tmp/tama-output/";
	
	final public static String INPUT_SAV_VEHICLES_FOLDER = INPUT_FOLDER + "vehicles\\";
	final public static String INPUT_SAV_STOPS = INPUT_FOLDER + "drtstops.xml";

	final public static String INPUT_SAMPLE_POPULATION_FOLDER = INPUT_FOLDER + "sample population\\";
	final public static String INPUT_POPULATION = INPUT_FOLDER + "2.output_plans.xml.gz";
//	final public static String INPUT_SUB_POPULATION = INPUT_FOLDER + "personAtrributes-with-subpopulation.xml";

	final public static String INPUT_ROAD_PRICING = INPUT_FOLDER + "2.output_toll.xml.gz";
	final public static String INPUT_COUNTS = INPUT_FOLDER + "2.output_counts.xml.gz";
	final public static String INPUT_NETWORK = INPUT_FOLDER + "2.output_network.xml.gz";	
	final public static String INPUT_FACILITES = INPUT_FOLDER + "2.output_facilities.xml.gz";
	final public static String INPUT_TRANSIT_SCHEDULE = INPUT_FOLDER + "2.output_transitSchedule.xml.gz";
	final public static String INPUT_TRANSIT_VEHICLES = INPUT_FOLDER + "2.output_transitVehicles.xml.gz";

	public static double AlPHA = 1.5;
	public static int WAIT_TIME = 720;
	public static int BETA = 900;
	public static int SEATS = 10;
	public static int NUM_OF_VEHICLES = 1000;
	public static boolean IS_REJECTION = true;
	public static boolean IS_REBALANCE = true;
	public static boolean IS_STOP_BASED = true;
	public static boolean IS_SAMPLE_POPULATION = true;
	public static double SAMPLE_POPUALTION_FACTOR = 0.01;

	final public static String RUN_ID = "/" + 3;

	public static void main(String[] args) {
		
		// create a new MATSim config for JLM
		Config config = createTamaConfig(IS_SAMPLE_POPULATION);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		//controler = DrtControlerCreator.createControlerWithSingleModeDrt(config, false);
		controler = DrtControlerCreator.createControler(config, false);
		
		// Add raptor
		controler.addOverridingModule(new SwissRailRaptorModule());
		// Add roadpricing
		controler.addOverridingModule(new RoadPricingModule());
		// Add drt-speed-up
		controler.addOverridingModule(new MultiModeDrtSpeedUpModule());
		controler.run();
	}

	/**
	 * Create a MATSim Config file
	 * 
	 * @return MATSim Config
	 */
	/**
	 * @return
	 */
	public static Config createTamaConfig(boolean isSamplePopulation) {
		Config config = ConfigUtils.createConfig();
		
		config.network().setInputFile(createNetworkSav(INPUT_NETWORK));

		if (isSamplePopulation == true) {
			String samplePop = samplePopulation(SAMPLE_POPUALTION_FACTOR);
			config.plans().setInputFile(samplePop);
		} else {
			config.plans().setInputFile(INPUT_POPULATION);
		}
//		config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile(true);
		config.facilities().setInputFile(INPUT_FACILITES);

		// modify controler
		config.controler().setWriteEventsInterval(100);
		config.controler().setWritePlansInterval(100);
		config.controler().setEventsFileFormats(EnumSet.of(EventsFileFormat.xml));
		config.controler().setOutputDirectory(OUTPUT_FOLDER + RUN_ID + "/");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(300);
		config.controler().setMobsim("qsim");
		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
		config.controler().setRunId(RUN_ID);

		// modify Qsim
		config.qsim().setStartTime(0.0);
		config.qsim().setEndTime(30 * 3600);
		config.qsim().setFlowCapFactor(0.08);
		config.qsim().setStorageCapFactor(Math.pow(0.08, 0.75));
		config.qsim().setNumberOfThreads(1);
		config.qsim().setSnapshotPeriod(1);
		config.qsim().setStuckTime(10);// 30,60 or multiply by 60
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setTimeStepSize(1);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);// kinematic waves
		config.qsim().setMainModes(Arrays.asList(TransportMode.car));
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);

		// Add roadpricing
		RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class);
		rpConfig.setTollLinksFile(INPUT_ROAD_PRICING);

		// modify counts
		config.counts().setInputFile(INPUT_COUNTS);
		config.counts().setCountsScaleFactor(10);

		// modify global
		config.global().setCoordinateSystem("EPSG:2039");
		config.global().setNumberOfThreads(16);
		config.global().setRandomSeed(4711);

		// Add transit
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile(INPUT_TRANSIT_SCHEDULE);
		config.transit().setVehiclesFile(INPUT_TRANSIT_VEHICLES);
		Set<String> modes = new HashSet<String>();
		modes.add("pt");
		modes.add("bus");
		config.transit().setTransitModes(modes);
		config.transitRouter().setSearchRadius(500);
		config.transitRouter().setMaxBeelineWalkConnectionDistance(300);
		config.transitRouter().setExtensionRadius(500);

		// Add raptor config
		SwissRailRaptorConfigGroup raptorConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);

		// Add sub-population
		//config.plans().setSubpopulationAttributeName("subpopulation");
//		config.plans().setInputPersonAttributeFile(INPUT_SUB_POPULATION);

		// Add sub-tour mode choice
		config.subtourModeChoice().setModes(new String[] { TransportMode.drt, TransportMode.car, TransportMode.pt,
				TransportMode.walk, TransportMode.bike });
		config.subtourModeChoice().setChainBasedModes(new String[] { TransportMode.car });
		config.subtourModeChoice().setConsiderCarAvailability(true);

		// Add timeAllocationMutator
		config.timeAllocationMutator().setMutationRange(3600);

		// Add strategy
		config.strategy().setMaxAgentPlanMemorySize(10);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		// Add strategy - plan selector
		StrategySettings changeExpStrategy = new StrategySettings();
		changeExpStrategy.setDisableAfter(-1);
		changeExpStrategy.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		changeExpStrategy.setWeight(0.8);
		config.strategy().addStrategySettings(changeExpStrategy);

		// ************************set internal sub-population
		// strategy*************************
		StrategySettings changeExpBetaInternal = new StrategySettings();
		// plan selector - internal agents (without disable)
		changeExpBetaInternal.setDisableAfter(-1);
		changeExpBetaInternal.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		changeExpBetaInternal.setWeight(0.8);
		changeExpBetaInternal.setSubpopulation("internalAgent");
		config.strategy().addStrategySettings(changeExpBetaInternal);

		// reroute internal agents
		StrategySettings reRouteInternal = new StrategySettings();
		reRouteInternal.setStrategyName(DefaultStrategy.ReRoute.toString());
		reRouteInternal.setWeight(0.1);
		reRouteInternal.setSubpopulation("internalAgent");
		config.strategy().addStrategySettings(reRouteInternal);

		// time-mutation internal agents
		StrategySettings timeMutatorInternal = new StrategySettings();
		timeMutatorInternal.setStrategyName(DefaultStrategy.TimeAllocationMutator.toString());
		timeMutatorInternal.setWeight(0.1);
		timeMutatorInternal.setSubpopulation("internalAgent");
		config.strategy().addStrategySettings(timeMutatorInternal);

		// sub-tour mode chouce internal agents
		StrategySettings subTourModeChoiceInternal = new StrategySettings();
		subTourModeChoiceInternal.setStrategyName(DefaultStrategy.SubtourModeChoice.toString());
		subTourModeChoiceInternal.setWeight(0.1);
		subTourModeChoiceInternal.setSubpopulation("internalAgent");
		config.strategy().addStrategySettings(subTourModeChoiceInternal);

		/*
		 * Set up the `reroute' subpopulation to consider rerouting as a strategy, 20%
		 * of the time, and the balance using ChangeExpBeta.
		 */
		StrategySettings rerouteStrategySettings = new StrategySettings();
		rerouteStrategySettings.setStrategyName(DefaultStrategy.ReRoute);
		rerouteStrategySettings.setSubpopulation("externalAgent");
		rerouteStrategySettings.setWeight(0.1);
		config.strategy().addStrategySettings(rerouteStrategySettings);

		StrategySettings changeExpBetaStrategySettings = new StrategySettings();
		changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		changeExpBetaStrategySettings.setSubpopulation("externalAgent");
		changeExpBetaStrategySettings.setWeight(0.8);
		config.strategy().addStrategySettings(changeExpBetaStrategySettings);

		// add network modes which are simulated on network in future add more modes
		// config.plansCalcRoute().setNetworkModes(Arrays.asList(TransportMode.car));
		// config.plansCalcRoute().setInsertingAccessEgressWalk(true);

		// global scoring values taken from TLVM model = SF 14
		config.planCalcScore().setEarlyDeparture_utils_hr(0.0);
		config.planCalcScore().setLateArrival_utils_hr(0);
		config.planCalcScore().setMarginalUtilityOfMoney(0.062);
		config.planCalcScore().setPerforming_utils_hr(0.96);
		config.planCalcScore().setUtilityOfLineSwitch(0);
		config.planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(-0.18);

		// car scoring functions from TLVM model = SF 14
		PlanCalcScoreConfigGroup.ModeParams carCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.car);
		carCalcScoreParams.setConstant(-0.562);
		carCalcScoreParams.setMode("car");
		carCalcScoreParams.setMonetaryDistanceRate(-0.0004);
		config.planCalcScore().addModeParams(carCalcScoreParams);

		// PT scoring functions from TLVM model = SF 14
		PlanCalcScoreConfigGroup.ModeParams ptCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.pt);
		ptCalcScoreParams.setConstant(-0.124);
		ptCalcScoreParams.setMode("pt");
		ptCalcScoreParams.setMarginalUtilityOfTraveling(-0.18);
		config.planCalcScore().addModeParams(ptCalcScoreParams);

		// Walk scoring functions from TLVM model = SF 14
		PlanCalcScoreConfigGroup.ModeParams walkCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.walk);
		walkCalcScoreParams.setMode("walk");
		walkCalcScoreParams.setMarginalUtilityOfTraveling(-1.14);
		config.planCalcScore().addModeParams(walkCalcScoreParams);

		// PT scoring functions from TLVM model = SF 14
		PlanCalcScoreConfigGroup.ModeParams drtCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.drt);
		drtCalcScoreParams.setConstant(-0.124);
		drtCalcScoreParams.setMode("drt");
		drtCalcScoreParams.setMarginalUtilityOfTraveling(-0.18);
		config.planCalcScore().addModeParams(drtCalcScoreParams);

		// TODO add ride as network mode remove from modechoice
		// Ride scoring functions place holder taken from Berlin MATSim model -
		// monetaryDistanceRate same as car -0.0004
		PlanCalcScoreConfigGroup.ModeParams rideCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.ride);
		rideCalcScoreParams.setMode("ride");
		rideCalcScoreParams.setMonetaryDistanceRate(-0.0004);
		config.planCalcScore().addModeParams(rideCalcScoreParams);

		// TODO check with JLM bike - bicyle
		// bike scoring functions place holder taken from Berlin MATSim model of bicyke
		PlanCalcScoreConfigGroup.ModeParams bikeCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.bike);
		bikeCalcScoreParams.setConstant(-1.9);
		bikeCalcScoreParams.setMode("bike");
		config.planCalcScore().addModeParams(bikeCalcScoreParams);

		// TODO get activities open hours
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(8 * 60 * 60);
		home.setMinimalDuration(1 * 60 * 60);
		home.setLatestStartTime(Time.convertHHMMInteger(2300));
		config.planCalcScore().addActivityParams(home);

		ActivityParams work = new ActivityParams("work");
		work.setOpeningTime(6 * 3600);
		work.setClosingTime(22 * 3600);
		work.setTypicalDuration(8 * 60 * 60);
		work.setMinimalDuration(1 * 30 * 60);
		work.setLatestStartTime(Time.convertHHMMInteger(2330));
		config.planCalcScore().addActivityParams(work);

		ActivityParams shopping = new ActivityParams("shopping");
		shopping.setOpeningTime(8 * 3600);
		shopping.setClosingTime(20 * 3600);
		shopping.setTypicalDuration(1 * 60 * 60);
		shopping.setMinimalDuration(1 * 5 * 60);
		shopping.setLatestStartTime(Time.convertHHMMInteger(2355));
		config.planCalcScore().addActivityParams(shopping);

		ActivityParams elemntarySchool = new ActivityParams("education_elementaryschool");
		elemntarySchool.setOpeningTime(0 * 3600);
		elemntarySchool.setTypicalDuration(4 * 60 * 60);
		elemntarySchool.setMinimalDuration(1 * 30 * 60);
		elemntarySchool.setLatestStartTime(Time.convertHHMMInteger(2330));
		elemntarySchool.setClosingTime(Time.convertHHMMInteger(2359));
		config.planCalcScore().addActivityParams(elemntarySchool);

		ActivityParams University = new ActivityParams("education_university");
		University.setOpeningTime(0 * 3600);
		University.setClosingTime(Time.convertHHMMInteger(2359));
		University.setTypicalDuration(6 * 60 * 60);
		University.setLatestStartTime(Time.convertHHMMInteger(2330));
		University.setMinimalDuration(1 * 30 * 60);
		config.planCalcScore().addActivityParams(University);

		ActivityParams highSchool = new ActivityParams("education_highschool");
		highSchool.setOpeningTime(0 * 3600);
		highSchool.setClosingTime(Time.convertHHMMInteger(2359));
		highSchool.setTypicalDuration(6 * 60 * 60);
		highSchool.setLatestStartTime(Time.convertHHMMInteger(2330));
		highSchool.setMinimalDuration(1 * 30 * 60);
		config.planCalcScore().addActivityParams(highSchool);

		ActivityParams leisure = new ActivityParams("leisure");
		leisure.setOpeningTime(0 * 3600);
		leisure.setClosingTime(Time.convertHHMMInteger(2359));
		leisure.setTypicalDuration(1 * 60 * 60);
		leisure.setMinimalDuration(1 * 5 * 60);
		config.planCalcScore().addActivityParams(leisure);

		ActivityParams tta = new ActivityParams("tta");
		tta.setTypicalDuration(1 * 60 * 60);
		tta.setMinimalDuration(1 * 5 * 60);
		tta.setLatestStartTime(Time.convertHHMMInteger(2355));
		config.planCalcScore().addActivityParams(tta);

		addDrtConfigGroup(config, AlPHA, WAIT_TIME, BETA, IS_REJECTION, IS_REBALANCE, SEATS, NUM_OF_VEHICLES, IS_STOP_BASED);
		
		//ConfigUtils.addOrGetModule(config, DrtSpeedUpConfigGroup.class);
		//MultiModeDrtSpeedUpModule.addTeleportedDrtMode(config);

		return config;
	}

	/**
	 * Add DRT to simulation default is door-2-door
	 * 
	 * 
	 */
	public static void addDrtConfigGroup(Config config, double alpha, int waitTime, int beta, boolean isRejection, boolean isReblance, int seats, int numOfVehicles, boolean isStopBased) {
		
		MultiModeDrtConfigGroup multiDRT = new MultiModeDrtConfigGroup();
		DrtConfigGroup drtConfigGroup = (DrtConfigGroup) multiDRT.createParameterSet(DrtConfigGroup.GROUP_NAME);

		//	drtConfigGroup.setMode(TransportMode.car); creates bugs if uncommented

		// stopbased, door2door, serviceAreaBased
		if (isStopBased == true) {
			drtConfigGroup.setOperationalScheme(OperationalScheme.stopbased);
			drtConfigGroup.setStopDuration(60.0);
			drtConfigGroup.setTransitStopFile(INPUT_SAV_STOPS);
			drtConfigGroup.setMaxWalkDistance(500);
		}

		drtConfigGroup.setMaxTravelTimeAlpha(alpha);
		drtConfigGroup.setMaxTravelTimeBeta(beta);
		drtConfigGroup.setMaxWaitTime(waitTime);
		drtConfigGroup.setRejectRequestIfMaxWaitOrTravelTimeViolated(isRejection);
		String vehiclesFile = createVehiclesFile(config, seats, numOfVehicles);
		drtConfigGroup.setVehiclesFile(vehiclesFile);
		drtConfigGroup.setPlotDetailedCustomerStats(true);
		drtConfigGroup.setNumberOfThreads(1);
		drtConfigGroup.setPlotDetailedCustomerStats(true);
		drtConfigGroup.setIdleVehiclesReturnToDepots(false);

		/*if (isReblance == true) {
			MinCostFlowRebalancingParams rebalance = new MinCostFlowRebalancingParams();
			rebalance.setTargetAlpha(0.5);
			rebalance.setTargetBeta(0.5);
			rebalance.setInterval(60 * 5);
			rebalance.setCellSize(1000);
			drtConfigGroup.addParameterSet(rebalance);
		}*/
		
		ConfigGroup extensiveInsertionSearchParamSet = drtConfigGroup.createParameterSet(ExtensiveInsertionSearchParams.SET_NAME);
		drtConfigGroup.addParameterSet(extensiveInsertionSearchParamSet);
		multiDRT.addParameterSet(drtConfigGroup);
		
		DrtFaresConfigGroup faresConfigGroup = new DrtFaresConfigGroup();
		ConfigGroup fareParamSet = faresConfigGroup.createParameterSet(DrtFareConfigGroup.GROUP_NAME);
		faresConfigGroup.addParameterSet(fareParamSet);
		config.addModule(faresConfigGroup);
		
		config.addModule(multiDRT);
		DvrpConfigGroup dvrp = new DvrpConfigGroup();
		config.addModule(dvrp);

	}

	private static String createVehiclesFile(Config config, int seats, int numOfVehicles) {		

		double operationStartTime = config.qsim().getStartTime().orElse(0);
		double operationEndTime = config.qsim().getEndTime().orElse(0);
		Random random = MatsimRandom.getRandom();

		Scenario scenario = ScenarioUtils.loadScenario(config);
		final int[] i = { 0 };
		final String allowedMode = TransportMode.car;
		Stream<DvrpVehicleSpecification> vehicleSpecificationStream = scenario.getNetwork().getLinks().entrySet()
				.stream().filter(entry -> entry.getValue().getAllowedModes().contains(allowedMode)) // drt can only start on links with Transport mode 'car'
				.sorted((e1, e2) -> (random.nextInt(2) - 1)) // shuffle links
				.limit(numOfVehicles) // select the first *numberOfVehicles* links
				.map(entry -> ImmutableDvrpVehicleSpecification.newBuilder()
						.id(Id.create("drt_" + i[0]++, DvrpVehicle.class)).startLinkId(entry.getKey()).capacity(seats)
						.serviceBeginTime(operationStartTime).serviceEndTime(operationEndTime).build());
		final String fileName = "vehicles-" + numOfVehicles + "-" + seats + ".xml";
		final String outPath = INPUT_SAV_VEHICLES_FOLDER + fileName;
		new FleetWriter(vehicleSpecificationStream).write(outPath);
		return outPath;
	}

	public static String samplePopulation(double sampleSize) {
		final String outPath = INPUT_SAMPLE_POPULATION_FOLDER + sampleSize + ".xml";

		File tempFile = new File(outPath);
		boolean exists = tempFile.exists();
		if (exists == true) {
			return outPath;

		} else {
			// create an empty scenario using an empty configuration
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

			// the writer will be called by the reader and write the new population file. As
			// parameter the fraction of the input population is passed. 
			StreamingPopulationWriter writer = new StreamingPopulationWriter(sampleSize);

			// the reader will read in an existing population file
			StreamingPopulationReader reader = new StreamingPopulationReader(scenario);
			reader.addAlgorithm(writer);

			try {
				writer.startStreaming(outPath);
				reader.readFile(INPUT_POPULATION);
			} finally {
				writer.closeStreaming();
			}

			return outPath;
		}
	}

	public static String createNetworkSav(String inputNetwork) {

		final String outPath = INPUT_FOLDER + "sav_network" + ".xml";

		MutableScenario  scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputNetwork);

		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> linkAllowedModes = new HashSet<String>();
			for (String mode : link.getAllowedModes()) {
				linkAllowedModes.add(mode);
				linkAllowedModes.add(TransportMode.car);
//				linkAllowedModes.add(TransportMode.drt);
			}

			link.setAllowedModes(linkAllowedModes);
		}
				new NetworkWriter(scenario.getNetwork()).write(outPath);
		return outPath;

}


}
