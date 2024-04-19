package benchmarking.simulatorconfiguration;

import core.model.Setting;
import core.simulation.simulators.HybridSimulator;

/**
 *
 * @author Martin
 */
public class HybridConfig extends SimulatorConfig{
    
    int threshold_tau = 5;
    int threshold_ode = 400;
    double factor_SSA_to_TAU = 2;
    double factor_TAU_to_ODE = 2;
    
    SSAConfig ssa_config = new SSAConfig();
    TAUConfig tau_config = new TAUConfig();
    ODEConfig ode_config = new ODEConfig();
    
    public HybridConfig(){
        super(SimulatorConfig.Type.HYB);
    }

    public int getThresholdTAU() {
        return threshold_tau;
    }

    public int getThresholdODE() {
        return threshold_ode;
    }

    public double getFactorSSA2TAU() {
        return factor_SSA_to_TAU;
    }

    public double getFactorTAU2ODE() {
        return factor_TAU_to_ODE;
    }

    public SSAConfig getSSAConfig() {
        return ssa_config;
    }

    public TAUConfig getTAUConfig() {
        return tau_config;
    }

    public ODEConfig getODEConfig() {
        return ode_config;
    }
    
    public HybridConfig setThresholdTau(int x) {
        this.threshold_tau = x;
        return this;
    }
    
    public HybridConfig setThresholdODE(int x) {
        this.threshold_ode = x;
        return this;
    }
    
    public HybridConfig setFactorSSAToTAU(double x) {
        this.factor_SSA_to_TAU = x;
        return this;
    }
    
    public HybridConfig setFactorTAUToODE(double x) {
        this.factor_TAU_to_ODE = x;
        return this;
    }
    
    public HybridConfig setSSAConfig(SSAConfig x) {
        this.ssa_config = x;
        return this;
    }
    
    public HybridConfig setTAUConfig(TAUConfig x) {
        this.tau_config = x;
        return this;
    }
    
    public HybridConfig setODEConfig(ODEConfig x) {
        this.ode_config = x;
        return this;
    }
    
    

    @Override
    public HybridSimulator createSimulator(Setting setting) {
        return new HybridSimulator(
                setting, 
                ssa_config.createSimulator(setting), 
                tau_config.createSimulator(setting), 
                ode_config.createSimulator(setting), 
                threshold_tau, 
                threshold_ode, 
                factor_SSA_to_TAU, 
                factor_TAU_to_ODE);
    }
}

