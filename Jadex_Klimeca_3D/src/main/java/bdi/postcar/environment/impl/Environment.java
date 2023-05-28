package bdi.postcar.environment.impl;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.types.cms.CMSStatusEvent;
import jadex.bridge.service.types.cms.CMSStatusEvent.CMSTerminatedEvent;
import jadex.bridge.service.types.cms.SComponentManagementService;
import jadex.commons.future.IntermediateEmptyResultListener;
import bdi.postcar.environment.ILocationObject;

import java.lang.reflect.Array;
import java.util.*;

public class Environment {
    private static Environment instance;
    private Map<IComponentIdentifier, Car> cars;

    private Environment() {
        this.cars = new LinkedHashMap<IComponentIdentifier, Car>();
    }

    public static synchronized Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
        }
        return instance;
    }

    public Car createCar(IInternalAccess agent) {
        IComponentIdentifier cid = agent.getId();
        Car ret;
        boolean create;
        synchronized (this) {
            ret = cars.get(cid);
            create = ret == null;
            if (create) {

                ret = new Car(cid, new Location(0.1, 0.9), null, 0.1, 0.8);

                cars.put(cid, ret);
            }
        }

        if (create) {
            SComponentManagementService.listenToComponent(cid, agent)
                    .addResultListener(new IntermediateEmptyResultListener<CMSStatusEvent>() {
                        @Override
                        public void intermediateResultAvailable(CMSStatusEvent cse) {
                            if (cse instanceof CMSTerminatedEvent) {
                                synchronized (Environment.this) {
                                    cars.remove(cid);
                                }
                            }
                        }
                    });
        } else {
            throw new IllegalStateException("Car for agent " + cid + " already exists");
        }

        return ret.clone();
    }

    public synchronized Car[] getCars() {
        return cloneList(cars.values(), Car.class);
    }

    public synchronized void updateCar(Car car) {
        cars.put(car.getAgentIdentifier(), car.clone());
    }

    public static <T extends ILocationObject> T[] cloneList(Collection<T> list, Class<T> type) {
        List<ILocationObject> ret = new ArrayList<>();
        for (ILocationObject o : list) {
            ret.add(((LocationObject) o).clone());
        }
        @SuppressWarnings("unchecked")
        T[] aret = ret.toArray((T[]) Array.newInstance(type, list.size()));
        return aret;
    }
}
