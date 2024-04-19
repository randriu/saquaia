package gui.simulator;

import benchmarking.simulatorconfiguration.HybridConfig;
import benchmarking.simulatorconfiguration.ODEConfig;
import benchmarking.simulatorconfiguration.SegmentalConfig;
import benchmarking.simulatorconfiguration.SimulatorConfig;
import benchmarking.simulatorconfiguration.TAUConfig;
import javax.swing.JOptionPane;

/**
 *
 * @author Martin
 */
public class SimulatorConfigEditor {

    public static SimulatorConfig edit(SimulatorConfig initial) {
        switch (initial.type) {
            case SSA:
                JOptionPane.showMessageDialog(null, "SSA simulators do not have parameters.");
                break;
            case TAU:
                TAUConfig tau_config = (TAUConfig) initial;
                while (true) {
                    String input = JOptionPane.showInputDialog(null, "Enter the epsilon parameter:", tau_config.getEpsilon());
                    if (input == null) {
                        return null;
                    }
                    try {
                        double epsilon = Double.parseDouble(input);
                        if (!(0 < epsilon && epsilon < Double.POSITIVE_INFINITY)) {
                            JOptionPane.showMessageDialog(null, "Could not parse input.\nMake sure to enter a positive float.");
                            continue;
                        }
                        return new TAUConfig().setEpsilon(epsilon);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Could not parse input.\nMake sure to enter a positive float.");
                    }
                }
            case HYB:
                HybridSimulatorPanel panelHybrid = new HybridSimulatorPanel((HybridConfig) initial);
                if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(null, panelHybrid, "Hybrid Simulator Settings", JOptionPane.OK_CANCEL_OPTION)) {
                    return panelHybrid.getHybridConfig();
                }
                break;
            case SEG:
                SegmentalSimulatorPanel panelSegmental = new SegmentalSimulatorPanel((SegmentalConfig) initial);
                if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(null, panelSegmental, "Segmental Simulator Settings", JOptionPane.OK_CANCEL_OPTION)) {
                    return panelSegmental.getSegmentalConfiguration();
                }
                break;
            case ODE:
                ODESimulatorPanel panelODE = new ODESimulatorPanel((ODEConfig) initial);
                if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(null, panelODE, "ODE Simulator Settings", JOptionPane.OK_CANCEL_OPTION)) {
                    return panelODE.getODEConfig();
                }
                break;
            default:
                JOptionPane.showMessageDialog(null, initial.type + " parameter editing is not supported in GUI.");
        }
        return null;
    }
}
