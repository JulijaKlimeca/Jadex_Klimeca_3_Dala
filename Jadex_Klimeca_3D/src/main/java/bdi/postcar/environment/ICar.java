package bdi.postcar.environment;

import jadex.bridge.IComponentIdentifier;

public interface ICar extends ILocationObject
{
	String getId();
	ILocation getLocation();
	double getChargestate();
	double getVisionRange();
	IComponentIdentifier getAgentIdentifier();
}
