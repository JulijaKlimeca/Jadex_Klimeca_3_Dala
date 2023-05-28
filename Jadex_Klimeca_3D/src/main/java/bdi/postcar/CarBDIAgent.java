package bdi.postcar;

import bdi.postcar.environment.*;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.runtime.IPlan;
import jadex.bridge.service.annotation.OnStart;
import jadex.micro.annotation.Agent;

@Agent(type = "bdi")
public class CarBDIAgent {
    //sensors, kas palidz agentam uztvert apkartejo vidi
    private SensorActuator actsense = new SensorActuator();

    //agenta, parliecibas par sevi, savu atrasanas vietu un uzledes limeni
    @Belief
    private ICar self = actsense.getSelf();

    @OnStart
    private void exampleBehavior(IBDIAgentFeature bdi) {

        bdi.dispatchTopLevelGoal(new PerformPatrol());
    }

    //Merki:

    @Goal(recur = true, orsuccess = false, recurdelay = 3000)
    class PerformPatrol {
        @Plan(trigger = @Trigger())
        private void moveAround(IPlan plan) {
            System.out.println("Start Plan");
            actsense.moveTo(0.9, 0.9);
            actsense.moveTo(0.1, 0.9);
            actsense.moveTo(0.1, 0.1);
            actsense.moveTo(0.1, 0.9);
        }
    }

    @Plan(trigger = @Trigger(goals = PerformPatrol.class))
    private void performPatrolPlan(IPlan plan) {
        System.out.println("Move around " + plan.getReason());
        actsense.moveTo(Math.random(), Math.random());
    }

}
