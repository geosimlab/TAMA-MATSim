package runner;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.roadpricing.RoadPricingConfigGroup;
import org.matsim.contrib.roadpricing.RoadPricingModule;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
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

/**
 * @author Golan Ben-Dor
 */
public class RunTamaCalibrated {
	final public static String OUTPUT_FOLDER = "E:\\geosimlab\\MATSim-TAMA\\MATSim-TAMA Output\\";
	
	final public static String INPUT_ROAD_PRICING = "E:\\geosimlab\\MATSim-TAMA\\MATSim-TAMA Input\\road6Toll.xml";
	final public static String INPUT_COUNTS = "E:\\geosimlab\\MATSim-TAMA\\MATSim-TAMA Input\\counts_2013_no_outliers.xml";
	final public static String INPUT_NETWORK = "E:\\geosimlab\\MATSim-TAMA\\MATSim-TAMA Input\\Network_9_model_4_walk_to_train_only_clean_modal_fix_capac.xml";
	final public static String INPUT_FACILITES = "E:\\geosimlab\\MATSim-TAMA\\MATSim-TAMA Input\\facilities.xml";
	final public static String INPUT_POPULATION = "E:\\geosimlab\\MATSim-TAMA\\MATSim-TAMA Input\\‏‏‏‏plans_internal_75_cars_25_pt_no_undefined.xml";
	final public static String INPUT_SUB_POPULATION = "E:\\geosimlab\\MATSim-TAMA\\MATSim-TAMA Input\\personAtrributes-with-subpopulation.xml";
	final public static String INPUT_TRANSIT_SCHEDULE = "E:\\geosimlab\\MATSim-TAMA\\MATSim-TAMA Input\\transitSchedule7_model_4.xml";
	final public static String INPUT_TRANSIT_VEHICLES = "E:\\geosimlab\\MATSim-TAMA\\MATSim-TAMA Input\\Vehicles_4_model_40pop_10_pct.xml";


	final public static String RUN_ID = "/" + 2;

	public static void main(String[] args) {
		// create a new MATSim config for JLM
		Config config = createTamaConfig();

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);

		// Add raptor
		controler.addOverridingModule(new SwissRailRaptorModule());
		// Add roadpricing
		controler.addOverridingModule( new RoadPricingModule());
		
		// Add cadyts	
		addCadyts(config, controler);
		
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
	public static Config createTamaConfig() {
		Config config = ConfigUtils.createConfig();

		config.network().setInputFile(INPUT_NETWORK);
		config.plans().setInputFile(INPUT_POPULATION);
		config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile(true);
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
		config.qsim().setNumberOfThreads(12);
		config.qsim().setSnapshotPeriod(1);
		config.qsim().setStuckTime(10);// 30,60 or multiply by 60
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setTimeStepSize(1);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);// kinematic waves
		config.qsim().setMainModes(Arrays.asList(TransportMode.car));
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);

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
		config.plans().setSubpopulationAttributeName("subpopulation");
		config.plans().setInputPersonAttributeFile(INPUT_SUB_POPULATION);

		// Add sub-tour mode choice
		config.subtourModeChoice()
				.setModes(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk, TransportMode.bike });
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

		// ************************set internal sub-population strategy*************************
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
		timeMutatorInternal
				.setStrategyName(DefaultStrategy.TimeAllocationMutator.toString());
		timeMutatorInternal.setWeight(0.1);
		timeMutatorInternal.setSubpopulation("internalAgent");
		config.strategy().addStrategySettings(timeMutatorInternal);
		
		// sub-tour mode chouce internal agents
		StrategySettings subTourModeChoiceInternal = new StrategySettings();
		subTourModeChoiceInternal.setStrategyName(DefaultStrategy.SubtourModeChoice.toString());
		subTourModeChoiceInternal.setWeight(0.1);
		subTourModeChoiceInternal.setSubpopulation("internalAgent");
		config.strategy().addStrategySettings(subTourModeChoiceInternal);
			
		
		/* Set up the `reroute' subpopulation to consider rerouting as a 
		 * strategy, 20% of the time, and the balance using ChangeExpBeta. */
		StrategySettings rerouteStrategySettings = new StrategySettings( ) ;
		rerouteStrategySettings.setStrategyName(DefaultStrategy.ReRoute );
		rerouteStrategySettings.setSubpopulation("externalAgent");
		rerouteStrategySettings.setWeight(0.1);
		config.strategy().addStrategySettings(rerouteStrategySettings);

		StrategySettings changeExpBetaStrategySettings = new StrategySettings();
		changeExpBetaStrategySettings.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta );
		changeExpBetaStrategySettings.setSubpopulation("externalAgent");
		changeExpBetaStrategySettings.setWeight(0.8);
		config.strategy().addStrategySettings(changeExpBetaStrategySettings);

		// add network modes which are simulated on network in future add more modes
		// config.plansCalcRoute().setNetworkModes(Arrays.asList(TransportMode.car));
		// config.plansCalcRoute().setInsertingAccessEgressWalk(true);

		// // just a place hodler
		// ModeRoutingParams taxiModeRoute = new ModeRoutingParams();
		// taxiModeRoute.setMode(TransportMode.taxi);
		// taxiModeRoute.setTeleportedModeSpeed(100.0);
		// config.plansCalcRoute().addModeRoutingParams(taxiModeRoute);

		// global scoring values taken from TLVM model = SF 14
		config.planCalcScore().setEarlyDeparture_utils_hr(0.0);
		config.planCalcScore().setLateArrival_utils_hr(0);
		config.planCalcScore().setMarginalUtilityOfMoney(0.062);
		config.planCalcScore().setPerforming_utils_hr(0.96);
		config.planCalcScore().setUtilityOfLineSwitch(0);
		config.planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(-0.18);

		// car scoring functions from TLVM model = SF 14
		PlanCalcScoreConfigGroup.ModeParams carCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(
				TransportMode.car);
		carCalcScoreParams.setConstant(-0.562);
		carCalcScoreParams.setMode("car");
		carCalcScoreParams.setMonetaryDistanceRate(-0.0004);
		config.planCalcScore().addModeParams(carCalcScoreParams);

		// PT scoring functions from TLVM model = SF 14
		PlanCalcScoreConfigGroup.ModeParams ptCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(
				TransportMode.pt);
		ptCalcScoreParams.setConstant(-0.124);
		ptCalcScoreParams.setMode("pt");
		ptCalcScoreParams.setMarginalUtilityOfTraveling(-0.18);
		config.planCalcScore().addModeParams(ptCalcScoreParams);

		// Walk scoring functions from TLVM model = SF 14
		PlanCalcScoreConfigGroup.ModeParams walkCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(
				TransportMode.walk);
		walkCalcScoreParams.setMode("walk");
		walkCalcScoreParams.setMarginalUtilityOfTraveling(-1.14);
		config.planCalcScore().addModeParams(walkCalcScoreParams);

		// TODO get values for Taxi scoring
		// Taxi scoring functions place holder (taken from car)
		// PlanCalcScoreConfigGroup.ModeParams TaxiCalcScoreParams = new
		// PlanCalcScoreConfigGroup.ModeParams(TransportMode.taxi);
		// TaxiCalcScoreParams.setConstant(-0.562);
		// TaxiCalcScoreParams.setMode("taxi");
		// TaxiCalcScoreParams.setMonetaryDistanceRate(-0.0004);
		// config.planCalcScore().addModeParams(walkCalcScoreParams);

		// TODO add ride as network mode remove from modechoice
		// Ride scoring functions place holder taken from Berlin MATSim model -
		// monetaryDistanceRate same as car -0.0004
		PlanCalcScoreConfigGroup.ModeParams rideCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(
				TransportMode.ride);
		rideCalcScoreParams.setMode("ride");
		rideCalcScoreParams.setMonetaryDistanceRate(-0.0004);
		config.planCalcScore().addModeParams(rideCalcScoreParams);

		// TODO check with JLM bike - bicyle
		// bike scoring functions place holder taken from Berlin MATSim model of bicyke
		PlanCalcScoreConfigGroup.ModeParams bikeCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(
				TransportMode.bike);
		bikeCalcScoreParams.setConstant(-1.9);
		bikeCalcScoreParams.setMode("bike");
		config.planCalcScore().addModeParams(bikeCalcScoreParams);

		// TODO get activities open hours
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(8 * 60 * 60);
		home.setMinimalDuration(1*60*60);
		home.setLatestStartTime(Time.convertHHMMInteger(2300));
		config.planCalcScore().addActivityParams(home);

		ActivityParams work = new ActivityParams("work");
		work.setOpeningTime(6 * 3600);
		work.setClosingTime(22 * 3600);
		work.setTypicalDuration(8 * 60 * 60);
		work.setMinimalDuration(1*30*60);
		work.setLatestStartTime(Time.convertHHMMInteger(2330));
		config.planCalcScore().addActivityParams(work);

		ActivityParams shopping = new ActivityParams("shopping");
		shopping.setOpeningTime(8 * 3600);
		shopping.setClosingTime(20 * 3600);
		shopping.setTypicalDuration(1 * 60 * 60);
		shopping.setMinimalDuration(1*5*60);
		shopping.setLatestStartTime(Time.convertHHMMInteger(2355));
		config.planCalcScore().addActivityParams(shopping);

		ActivityParams elemntarySchool = new ActivityParams("education_elementaryschool");
		elemntarySchool.setOpeningTime(0 * 3600);
		elemntarySchool.setTypicalDuration(4 * 60 * 60);
		elemntarySchool.setMinimalDuration(1*30*60);
		elemntarySchool.setLatestStartTime(Time.convertHHMMInteger(2330));
		elemntarySchool.setClosingTime(Time.convertHHMMInteger(2359));
		config.planCalcScore().addActivityParams(elemntarySchool);
	
		ActivityParams University = new ActivityParams("education_university");
		University.setOpeningTime(0 * 3600);
		University.setClosingTime(Time.convertHHMMInteger(2359));	
		University.setTypicalDuration(6 * 60 * 60);
		University.setLatestStartTime(Time.convertHHMMInteger(2330));
		University.setMinimalDuration(1*30*60);
		config.planCalcScore().addActivityParams(University);

		ActivityParams highSchool = new ActivityParams("education_highschool");
		highSchool.setOpeningTime(0 * 3600);
		highSchool.setClosingTime(Time.convertHHMMInteger(2359));
		highSchool.setTypicalDuration(6 * 60 * 60);
		highSchool.setLatestStartTime(Time.convertHHMMInteger(2330));
		highSchool.setMinimalDuration(1*30*60);
		config.planCalcScore().addActivityParams(highSchool);
	
		ActivityParams leisure = new ActivityParams("leisure");
		leisure.setOpeningTime(0 * 3600);
		leisure.setClosingTime(Time.convertHHMMInteger(2359));
		leisure.setTypicalDuration(1 * 60 * 60);
		leisure.setMinimalDuration(1*5*60);
		config.planCalcScore().addActivityParams(leisure);
		
		ActivityParams tta = new ActivityParams("tta");
		tta.setTypicalDuration(1 * 60 * 60);
		tta.setMinimalDuration(1*5*60);
		tta.setLatestStartTime(Time.convertHHMMInteger(2355));
		config.planCalcScore().addActivityParams(tta);

		return config;
	}
	

	/**
	 * Add calibration config to simulation
	 * 

	 */
	private static void addCadyts(Config config, Controler controler) {
		//cadyst
        CadytsConfigGroup cadystConfigGroup = ConfigUtils.addOrGetModule(config,  CadytsConfigGroup.class);
        cadystConfigGroup.setStartTime(6*60*60);
        cadystConfigGroup.setEndTime(72000);
//      cadystConfigGroup.setEndTime(108000);
//      cadystConfigGroup.setPreparatoryIterations(5);

        Counts<Link> calibrationCounts = new Counts<>();
        new CountsReaderMatsimV1(calibrationCounts).readFile(INPUT_COUNTS);
        // ---

        CadytsCarModule cadytsCarModule= new CadytsCarModule(calibrationCounts);  
        controler.addOverridingModule(cadytsCarModule);

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject CadytsContext cadytsContext;
			@Inject ScoringParametersForPerson parameters;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters(person);
				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
				scoringFunction.setWeightOfCadytsCorrection(15. * config.planCalcScore().getBrainExpBeta()) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );

				return scoringFunctionAccumulator;
			}
		}) ;
	}
}
