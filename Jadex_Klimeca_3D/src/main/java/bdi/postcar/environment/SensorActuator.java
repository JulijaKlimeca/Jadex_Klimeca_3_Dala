package bdi.postcar.environment;

import bdi.postcar.environment.impl.*;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.impl.ExecutionComponentFeature;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.clock.ITimedObject;
import jadex.commons.ErrorException;
import jadex.commons.SUtil;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

//SensorActuator klase kas parvalda agenta parliecibas
public class SensorActuator {
    //klases mainigie
    public IInternalAccess agent;
    private Car self;
    private Location target;
    private Set<ICar> cars;
    private Future<Void> recharging;

    public SensorActuator() {
        this.agent = ExecutionComponentFeature.LOCAL.get();
        if (agent == null) {
            throw new IllegalStateException("Failed to find any agent for sensorActuator");
        }

        self = Environment.getInstance().createCar(agent);
        this.cars = new LinkedHashSet<>();
    }

    //Agenta parstavniciba par sevi
    public ICar getSelf() {
        if (!agent.getFeature(IExecutionFeature.class).isComponentThread()) {
            throw new IllegalStateException("Error: Agent must be called in a thread");
        }

        return self;
    }
    //agents atbrauc iz noradito vietu pec x y koordinatiem
    public void moveTo(double x, double y) {
        if (!agent.getFeature(IExecutionFeature.class).isComponentThread()) {
            throw new IllegalStateException("Error: Agent must be called in a thread");
        }
        if (target != null) {
            throw new IllegalStateException("Cant  go to the multy targets at once. Target is: " + target);
        }
        //parbauda agenta uzlades limeni
        if (self.getChargestate() <= 0) {
            if (recharging == null) {
                recharging = new Future<Void>();
            }
            agent.getLogger().warning("moveTo() called with empty battery -> blocking until recharged.");
            recharging.get();
        }

        this.target = new Location(x, y);

        final Future<Void> reached = new Future<>();
        //agents ir netalu no noteiktas vietas
        if (self.getLocation().isNear(target)) {
            reached.setResultIfUndone(null);
        } else {

            final IClockService clock = agent.getFeature(IRequiredServicesFeature.class).getLocalService(new ServiceQuery<>(IClockService.class));
            clock.createTickTimer(new ITimedObject() {
                long lasttime = clock.getTime();
                ITimedObject timer = this;

                @Override
                public void timeEventOccurred(long currenttime) {
                    if (!reached.isDone()) {

                        agent.getFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>() {
                            @Override
                            public IFuture<Void> execute(IInternalAccess ia) {

                                double delta = (currenttime - lasttime) / 1000.0;

                                double chargestate = self.getChargestate() - delta / 100;
                                if (chargestate < 0) {
                                    self.setChargestate(0);
                                    return new Future<>(new IllegalStateException("Run of battery!"));
                                }
                                self.setChargestate(chargestate);
                                //apekina distanciju lidz objektam
                                double total_dist = self.getLocation().getDistance(target);
                                double move_dist = Math.min(total_dist, 0.1 * delta);
                                //updejto x un y koordinates
                                double dx = (target.getX() - self.getLocation().getX()) * move_dist / total_dist;
                                double dy = (target.getY() - self.getLocation().getY()) * move_dist / total_dist;
                                //updejto agenta koordinates
                                self.setLocation(new Location(self.getLocation().getX() + dx, self.getLocation().getY() + dy));
                                //agenta atrasanas vieta vide
                                Environment.getInstance().updateCar(self);

                                update();

                                if (self.getLocation().isNear(target)) {
                                    reached.setResultIfUndone(null);
                                } else {
                                    lasttime = currenttime;
                                    clock.createTickTimer(timer);
                                }
                                return IFuture.DONE;
                            }
                        }).addResultListener(new IResultListener<Void>() {
                            @Override
                            public void exceptionOccurred(Exception exception) {
                                reached.setExceptionIfUndone(exception);
                            }

                            @Override
                            public void resultAvailable(Void result) {
                            }
                        });
                    }
                }
            });
        }

        try {
            reached.get();
        } catch (Throwable t) {
            reached.setExceptionIfUndone(t instanceof Exception ? (Exception) t : new ErrorException((Error) t));
            SUtil.throwUnchecked(t);
        } finally {
            target = null;
            //Updejto/izdzes objekta koordinated, kad agents jau atbrauca uz noteikto vietu
        }
    }
    //atjaunina agenta uzskatu uz vidi
    void update() {
        updateObjects(cars, Environment.getInstance().getCars());
    }

    //atjaunina atrasanas vietu objektiem
    <T extends ILocationObject> void updateObjects(Set<T> oldset, T[] newset) {
        Map<T, T> newmap = new LinkedHashMap<>();
        for (T o : newset) {
            if (o.equals(self)) {
                self.update((Car) o);
            } else {
                newmap.put(o, o);
            }
        }

        for (LocationObject oldObject : oldset.toArray(new LocationObject[oldset.size()])) {
            LocationObject newobj = (LocationObject) newmap.remove(oldObject);
            if (oldObject.getLocation().getDistance(self.getLocation()) <= self.getVisionRange()
                    && (newobj == null || newobj.getLocation().getDistance(self.getLocation()) > self.getVisionRange())) {
                oldset.remove(oldObject);
            }
            if (newobj != null && newobj.getLocation().getDistance(self.getLocation()) <= self.getVisionRange()) {
                oldObject.update(newobj);
            }
        }
        for (T newobj : newmap.values()) {
            if (newobj.getLocation().getDistance(self.getLocation()) <= self.getVisionRange()) {
                oldset.add(newobj);
            }
        }
    }
}
