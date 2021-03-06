/**

DERT is a viewer for digital terrain models created from data collected during NASA missions.

DERT is Released in under the NASA Open Source Agreement (NOSA) found in the “LICENSE” folder where you
downloaded DERT.

DERT includes 3rd Party software. The complete copyright notice listing for DERT is:

Copyright © 2015 United States Government as represented by the Administrator of the National Aeronautics and
Space Administration.  No copyright is claimed in the United States under Title 17, U.S.Code. All Other Rights
Reserved.

Desktop Exploration of Remote Terrain (DERT) could not have been written without the aid of a number of free,
open source libraries. These libraries and their notices are listed below. Find the complete third party license
listings in the separate “DERT Third Party Licenses” pdf document found where you downloaded DERT in the
LICENSE folder.
 
JogAmp Ardor3D Continuation
Copyright © 2008-2012 Ardor Labs, Inc.
 
JogAmp
Copyright 2010 JogAmp Community. All rights reserved.
 
JOGL Portions Sun Microsystems
Copyright © 2003-2009 Sun Microsystems, Inc. All Rights Reserved.
 
JOGL Portions Silicon Graphics
Copyright © 1991-2000 Silicon Graphics, Inc.
 
Light Weight Java Gaming Library Project (LWJGL)
Copyright © 2002-2004 LWJGL Project All rights reserved.
 
Tile Rendering Library - Brian Paul 
Copyright © 1997-2005 Brian Paul. All Rights Reserved.
 
OpenKODE, EGL, OpenGL , OpenGL ES1 & ES2
Copyright © 2007-2010 The Khronos Group Inc.
 
Cg
Copyright © 2002, NVIDIA Corporation
 
Typecast - David Schweinsberg 
Copyright © 1999-2003 The Apache Software Foundation. All rights reserved.
 
PNGJ - Herman J. Gonzalez and Shawn Hartsock
Copyright © 2004 The Apache Software Foundation. All rights reserved.
 
Apache Harmony - Open Source Java SE
Copyright © 2006, 2010 The Apache Software Foundation.
 
Guava
Copyright © 2010 The Guava Authors
 
GlueGen Portions
Copyright © 2010 JogAmp Community. All rights reserved.
 
GlueGen Portions - Sun Microsystems
Copyright © 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 
SPICE
Copyright © 2003, California Institute of Technology.
U.S. Government sponsorship acknowledged.
 
LibTIFF
Copyright © 1988-1997 Sam Leffler
Copyright © 1991-1997 Silicon Graphics, Inc.
 
PROJ.4
Copyright © 2000, Frank Warmerdam

LibJPEG - Independent JPEG Group
Copyright © 1991-2018, Thomas G. Lane, Guido Vollbeding
 

Disclaimers

No Warranty: THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY KIND,
EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY
THAT THE SUBJECT SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY
WARRANTY THAT THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE. THIS AGREEMENT
DOES NOT, IN ANY MANNER, CONSTITUTE AN ENDORSEMENT BY GOVERNMENT AGENCY OR ANY
PRIOR RECIPIENT OF ANY RESULTS, RESULTING DESIGNS, HARDWARE, SOFTWARE PRODUCTS OR
ANY OTHER APPLICATIONS RESULTING FROM USE OF THE SUBJECT SOFTWARE.  FURTHER,
GOVERNMENT AGENCY DISCLAIMS ALL WARRANTIES AND LIABILITIES REGARDING THIRD-PARTY
SOFTWARE, IF PRESENT IN THE ORIGINAL SOFTWARE, AND DISTRIBUTES IT "AS IS."

Waiver and Indemnity:  RECIPIENT AGREES TO WAIVE ANY AND ALL CLAIMS AGAINST THE UNITED
STATES GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY PRIOR
RECIPIENT.  IF RECIPIENT'S USE OF THE SUBJECT SOFTWARE RESULTS IN ANY LIABILITIES,
DEMANDS, DAMAGES, EXPENSES OR LOSSES ARISING FROM SUCH USE, INCLUDING ANY DAMAGES
FROM PRODUCTS BASED ON, OR RESULTING FROM, RECIPIENT'S USE OF THE SUBJECT SOFTWARE,
RECIPIENT SHALL INDEMNIFY AND HOLD HARMLESS THE UNITED STATES GOVERNMENT, ITS
CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT, TO THE EXTENT
PERMITTED BY LAW.  RECIPIENT'S SOLE REMEDY FOR ANY SUCH MATTER SHALL BE THE IMMEDIATE,
UNILATERAL TERMINATION OF THIS AGREEMENT.

**/

package gov.nasa.arc.dert.lighting;

import gov.nasa.arc.dert.camera.BasicCamera;
import gov.nasa.arc.dert.landscape.Landscape;
import gov.nasa.arc.dert.render.ShadowEffects;
import gov.nasa.arc.dert.render.ShadowMap;
import gov.nasa.arc.dert.scene.World;
import gov.nasa.arc.dert.util.StateUtil;
import gov.nasa.arc.dert.util.StringUtil;
import gov.nasa.arc.dert.util.TimeUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TimeZone;

import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.event.DirtyType;

public class Lighting {

	public static ReadOnlyColorRGBA defaultBackgroundColor = ColorRGBA.LIGHT_GRAY;
	public static String DATE_FORMAT;
	public static final double HALF_PI = Math.PI/2;

	// Defaults for light position (artificial)
	public static double defaultAz = 0, defaultEl = HALF_PI;

	// Table of LMST epochs for each globe
	protected static Hashtable<String, Object> lmstTable = new Hashtable<String, Object>();

	// Default epoch ( 01Jan1970)
	protected static Date defaultEpoch = new Date(0);

	// Main light attributes
	protected ColorRGBA globalAmbientIntensity = new ColorRGBA(0.2f, 0.2f, 0.2f, 1);
	protected ColorRGBA diffuseIntensity = new ColorRGBA(0.85f, 0.85f, 0.85f, 1);
	protected ColorRGBA ambientIntensity = new ColorRGBA(0.25f, 0.25f, 0.25f, 1);

	// Headlight attributes
	protected ColorRGBA headlightIntensity = new ColorRGBA(0.85f, 0.85f, 0.85f, 1);

	// Visibility flags
	protected boolean isLamp = true, headlightEnabled = false;

	// Main light position (artificial)
	protected double azimuth, elevation;

	// Main light direction
	protected Vector3 direction = new Vector3();
	
	// Main light reference location on surface (used to compute light direction from Sun)
	protected Vector3 refLoc = null;

	// The epoch for LMST
	protected Date epoch;
	
	// Current time
	protected long timeUTC = 0;

	// The background color
	private ColorRGBA backgroundColor;
	private ReadOnlyColorRGBA background;
	private float backgroundSat = 1;

	// Global light state
	protected transient LightState lightState;

	// Lights
	protected transient DirectionalLight headlight;
	protected transient DirectionalLightNode light;

	// Shadow objects
	protected transient ShadowMap shadowMap;
	protected transient ShadowEffects shadowEffects;

	// Time helper
	protected transient TimeUtil timeUtil;

	/**
	 * Constructor
	 */
	public Lighting() {
		azimuth = defaultAz;
		elevation = defaultEl;
		direction = new Vector3(Vector3.NEG_UNIT_Z);
		background = defaultBackgroundColor;
		backgroundColor = new ColorRGBA(background);
	}

	/**
	 * Constructor from hash map
	 */
	public Lighting(HashMap<String,Object> map) {
		azimuth = StateUtil.getDouble(map, "Azimuth", defaultAz);
		elevation = StateUtil.getDouble(map, "Elevation", defaultEl);
		direction = StateUtil.getVector3(map, "Direction", Vector3.NEG_UNIT_Z);
		globalAmbientIntensity = StateUtil.getColorRGBA(map, "GlobalAmbientIntensity", globalAmbientIntensity);
		diffuseIntensity = StateUtil.getColorRGBA(map, "DiffuseIntensity", diffuseIntensity);
		ambientIntensity = StateUtil.getColorRGBA(map, "AmbientIntensity", ambientIntensity);
		headlightIntensity = StateUtil.getColorRGBA(map, "HeadlightIntensity", headlightIntensity);
		isLamp = StateUtil.getBoolean(map, "IsLamp", isLamp);
		headlightEnabled = StateUtil.getBoolean(map, "HeadlightEnabled", headlightEnabled);
		refLoc = StateUtil.getVector3(map, "ReferenceLocation", refLoc);
		epoch = (Date)map.get("Epoch");
		timeUTC = StateUtil.getLong(map, "TimeUTC", timeUTC);
		background = StateUtil.getColorRGBA(map, "BackgroundColor", defaultBackgroundColor);
		backgroundColor = new ColorRGBA(background);
	}

	/**
	 * Set up OpenGL lighting
	 */
	public void initialize() {
		lightState = new LightState();
		lightState.setGlobalAmbient(globalAmbientIntensity);
		lightState.setEnabled(true);
		headlight = new DirectionalLight();
		headlight.setDiffuse(headlightIntensity);
		headlight.setSpecular(new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));
		headlight.setEnabled(headlightEnabled);
		lightState.attach(headlight);
		light = new DirectionalLightNode("Sol", azimuth, elevation, direction);
		light.getLight().setAmbient(ambientIntensity);
		light.getLight().setDiffuse(diffuseIntensity);
	}

	/**
	 * Set the target object of the lights and shadows
	 * 
	 * @param target
	 */
	public void setTarget(Spatial target) {
		light.setTarget(target);
		light.setEnabled(true);
		shadowEffects = new ShadowEffects();
		shadowEffects.setEnabled(true);
		target.setRenderState(shadowEffects);
	}

	/**
	 * Dispose of lighting and shadow resources
	 */
	public void dispose() {
		if (shadowMap != null) {
			shadowMap.dispose();
		}
	}

	/**
	 * Make the shadows visible
	 * 
	 * @param enabled
	 */
	public void enableShadow(boolean enabled) {
		getShadowMap().setEnabled(enabled);
		shadowEffects.shadowEnabled = enabled;
	}

	/**
	 * Find out if shadows are visible
	 * 
	 * @return
	 */
	public boolean isShadowEnabled() {
		if (shadowMap == null) {
			return (false);
		}
		return (shadowMap.getEnabled());
	}

	/**
	 * Set main light to artifical
	 * 
	 * @param mode
	 */
	public void setLampMode(boolean mode) {
		isLamp = mode;
		if (!isLamp) {
			World.getInstance().setTime(World.getInstance().getTime());
		} else {
			light.setPositionFromAzEl();
		}
	}

	/**
	 * Find out if main light is artificial
	 * 
	 * @return
	 */
	public final boolean isLampMode() {
		return (isLamp);
	}

	/**
	 * Show headlight
	 * 
	 * @param enable
	 */
	public void enableHeadlight(boolean enable) {
		headlightEnabled = enable;
		headlight.setEnabled(enable);
		World.getInstance().getRoot().markDirty(DirtyType.RenderState);
	}

	/**
	 * Find out if headlight is visible
	 * 
	 * @return
	 */
	public final boolean isHeadlightEnabled() {
		return (headlightEnabled);
	}

	/**
	 * Change the global OpenGL lighting attributes
	 * 
	 * @param twoSidedLighting
	 * @param separateSpecularLighting
	 * @param ambientLighting
	 */
	public void setGlobalLightState(boolean twoSidedLighting, boolean separateSpecularLighting,
		ColorRGBA ambientLighting) {
		lightState.setTwoSidedLighting(twoSidedLighting);
		lightState.setSeparateSpecular(separateSpecularLighting);
		globalAmbientIntensity.set(ambientLighting);
		lightState.setGlobalAmbient(globalAmbientIntensity);
	}

	/**
	 * Get the light state object
	 * 
	 * @return
	 */
	public LightState getGlobalLightState() {
		return (lightState);
	}

	/**
	 * Get the main light
	 * 
	 * @return
	 */
	public DirectionalLightNode getLight() {
		return (light);
	}

	/**
	 * Set the time for the globe
	 * 
	 * @param timeUTC
	 * @param globeName
	 * @param timeLonLat
	 */
	public void setTime(long timeUTC) {
		this.timeUTC = timeUTC;
		if (!isLamp) {
			light.setTime(timeUTC, Landscape.getInstance().getGlobeName(), "Sun", getRefLoc());
			setBackgroundSaturation(light.getElevation());
//			CelestialBody.update(timeUTC);
		}
		World.getInstance().getMarble().setSolarDirection(getLightDirection());
	}

	/**
	 * Pre-render this light (for shadows and headlight direction)
	 * 
	 * @param camera
	 * @param renderer
	 * @param worldChanged
	 */
	public void prerender(BasicCamera camera, Renderer renderer, boolean worldChanged) {
		if (headlightEnabled) {
			headlight.setDirection(camera.getDirection());
		}
		if (isShadowEnabled() && worldChanged) {
			shadowMap.updateLightDirection(light.getDirection());
			shadowMap.doPrerender(renderer);
		}
	}

	/**
	 * Post-render this lighting (update orb position and render)
	 * 
	 * @param camera
	 * @param renderer
	 * @param worldChanged
	 */
	public void postrender(BasicCamera camera, Renderer renderer, boolean worldChanged) {
		light.updateOrb(camera);
		light.drawOrb(renderer);
	}

	/**
	 * Get the direction to the light
	 * 
	 * @return
	 */
	public ReadOnlyVector3 getLightDirection() {
		return (light.getDirectionToLight());
	}

	/**
	 * Set the light position
	 * 
	 * @param az
	 * @param el
	 */
	public void setLightPosition(double az, double el) {
		light.setAzEl(az, el);
		light.setPositionFromAzEl();
		setBackgroundSaturation(el);
		World.getInstance().getMarble().setSolarDirection(getLightDirection());
	}

	/**
	 * Get the shadow map
	 * 
	 * @return
	 */
	public ShadowMap getShadowMap() {
		if (shadowMap == null) {
			Landscape landscape = Landscape.getInstance();
			Vector3 smCenter = new Vector3(getRefLoc());
			landscape.sphericalToLocalCoordinate(smCenter);
			shadowMap = new ShadowMap(landscape.getCenter(), landscape.getWorldBound().getRadius(), World.getInstance()
				.getContents(), World.getInstance().getContents());
			shadowMap.setPolygonOffsetFactor(2);
			shadowMap.setPolygonOffsetUnits(2);
		}
		return (shadowMap);
	}

	/**
	 * Prepare lighting for persistence
	 */
	public HashMap<String,Object> saveAsHashMap() {
//		azimuth = light.getAzimuth();
//		elevation = light.getElevation();
//		direction = light.getDirection();
		HashMap<String,Object> map = new HashMap<String,Object>();
		map.put("Azimuth", new Double(light.getAzimuth()));
		map.put("Elevation", new Double(light.getElevation()));
		StateUtil.putVector3(map, "Direction", direction);
		StateUtil.putColorRGBA(map, "GlobalAmbientIntensity", globalAmbientIntensity);
		StateUtil.putColorRGBA(map, "DiffuseIntensity", diffuseIntensity);
		StateUtil.putColorRGBA(map, "AmbientIntensity", ambientIntensity);
		StateUtil.putColorRGBA(map, "HeadlightIntensity", headlightIntensity);
		map.put("IsLamp", new Boolean(isLamp));
		map.put("HeadlightEnabled", new Boolean(headlightEnabled));
		if (refLoc != null)
			StateUtil.putVector3(map, "ReferenceLocation", refLoc);
		map.put("Epoch", epoch);
		map.put("TimeUTC", new Long(timeUTC));
		StateUtil.putColorRGBA(map, "BackgroundColor", background);
		return(map);
	}

	/**
	 * Get the background color
	 * 
	 * @return
	 */
	public ReadOnlyColorRGBA getBackgroundColor() {
		return (backgroundColor);
	}

	/**
	 * Set the background color
	 * 
	 * @param color
	 */
	public void setBackgroundColor(ReadOnlyColorRGBA color) {
		background = color;
		setBackgroundSaturation(backgroundSat);
	}
	
	public void setBackgroundSaturation(double el) {
		// Start to darken at 3 degrees from horizon.		
		if (el <= 0.05) {
			el /= 0.05f;
			backgroundSat = (float)(Math.max(Math.sin(HALF_PI*el), 0));
		}
		else
			backgroundSat = 1;
		backgroundColor.set(background.getRed()*backgroundSat, background.getGreen()*backgroundSat, background.getBlue()*backgroundSat, background.getAlpha());
		World.getInstance().getRoot().markDirty(DirtyType.RenderState);
	}

	/**
	 * Set the diffuse intensity of the main light
	 * 
	 * @param val
	 */
	public void setDiffuseIntensity(double val) {
		diffuseIntensity.set((float) val, (float) val, (float) val, 1);
		light.getLight().setDiffuse(diffuseIntensity);
		World.getInstance().getRoot().markDirty(DirtyType.RenderState);
	}

	/**
	 * Get the diffuse intensity of the main light
	 * 
	 * @return
	 */
	public double getDiffuseIntensity() {
		return (diffuseIntensity.getRed());
	}

	/**
	 * Set the ambient intensity of the main light
	 * 
	 * @param val
	 */
	public void setAmbientIntensity(double val) {
		ambientIntensity.set((float) val, (float) val, (float) val, 1);
		light.getLight().setAmbient(ambientIntensity);
		World.getInstance().getRoot().markDirty(DirtyType.RenderState);
	}

	/**
	 * Get the ambient intensity of the main light
	 * 
	 * @return
	 */
	public double getAmbientIntensity() {
		return (ambientIntensity.getRed());
	}

	/**
	 * Set the headlight diffuse intensity
	 * 
	 * @param val
	 */
	public void setHeadlightIntensity(double val) {
		headlightIntensity.set((float) val, (float) val, (float) val, 1);
		headlight.setDiffuse(headlightIntensity);
		World.getInstance().getRoot().markDirty(DirtyType.RenderState);
	}

	/**
	 * Get the headlight diffuse intensity
	 * 
	 * @return
	 */
	public double getHeadlightIntensity() {
		return (headlightIntensity.getRed());
	}

	/**
	 * Set the global ambient intensity
	 * 
	 * @param val
	 */
	public void setGlobalIntensity(double val) {
		globalAmbientIntensity.set((float) val, (float) val, (float) val, 1);
		light.setGlobalAmbient(globalAmbientIntensity);
		lightState.setGlobalAmbient(globalAmbientIntensity);
		World.getInstance().getRoot().markDirty(DirtyType.RenderState);
	}

	/**
	 * Get the global ambient intensity
	 * 
	 * @return
	 */
	public double getGlobalIntensity() {
		return (globalAmbientIntensity.getRed());
	}

	/**
	 * Position the main light overhead
	 */
	public void overhead() {
		light.setAzEl(0f, Math.PI / 2);
		light.setPositionFromAzEl();
	}

	/**
	 * Get the current epoch
	 * 
	 * @return
	 */
	public Date getEpoch() {
		if (epoch == null) {
			String key = Landscape.getInstance().getGlobeName();
			Date date = (Date) lmstTable.get(key + ".epoch");
			if (date == null) {
				return (defaultEpoch);
			}
			return (date);
		}
		return (epoch);
	}

	private double getToEarth() {
		String key = Landscape.getInstance().getGlobeName();
		Double toEarth = (Double) lmstTable.get(key + ".toEarth");
		if (toEarth == null) {
			return (1);
		}
		return (toEarth);
	}

	/**
	 * Set the current epoch
	 * 
	 * @param epoch
	 */
	public void setEpoch(Date epoch) {
		this.epoch = epoch;
		timeUtil = new TimeUtil(epoch.getTime(), getToEarth());
	}

	/**
	 * Convert a date to LMST
	 * 
	 * @param date
	 * @return
	 */
	public int[] dateToLMST(Date date) {
		if (timeUtil == null) {
			timeUtil = new TimeUtil(getEpoch().getTime(), getToEarth());
		}
		return (timeUtil.time2LMST(date.getTime()));
	}

	/**
	 * Convert LMST to a Date
	 * 
	 * @param sol
	 * @param hour
	 * @param minute
	 * @param second
	 * @return
	 */
	public Date lmstToDate(int sol, int hour, int minute, int second) {
		if (timeUtil == null) {
			timeUtil = new TimeUtil(getEpoch().getTime(), getToEarth());
		}
		return (new Date(timeUtil.lmst2Time(sol, hour, minute, second)));
	}
	
	/**
	 * Set the light surface reference (lon/lat/alt).
	 */
	public void setRefLoc(ReadOnlyVector3 refLoc) {
		if (this.refLoc == null)
			this.refLoc = new Vector3(refLoc);
		else
			this.refLoc.set(refLoc);
		setTime(timeUTC);
	}
	
	/**
	 * Get the light surface reference (lon/lat/alt).
	 */
	public ReadOnlyVector3 getRefLoc() {
		if (refLoc == null)
			return(Landscape.getInstance().getCenterLonLat());
		return(refLoc);
	}

	/**
	 * Load the lighting properties
	 * 
	 * @param properties
	 */
	public static void loadProperties(Properties properties) {
		double[] dArray = StringUtil.getDoubleArray(properties, "LampLocation", new double[] { 45, 45 }, false);
		defaultAz = (float) Math.toRadians(dArray[0]);
		defaultEl = (float) Math.toRadians(dArray[1]);

		DATE_FORMAT = properties.getProperty("DateFormat", "ddMMMyy HH:mm:ss");
		SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Enumeration<String> keys = (Enumeration<String>) properties.propertyNames();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (key.startsWith("LMST.")) {
				String k = key.substring(5, key.length());
				if (k.endsWith(".epoch")) {
					Date date = null;
					try {
						date = dateFormatter.parse(properties.getProperty(key));
						lmstTable.put(k, date);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (k.endsWith(".toEarth")) {
					Double toEarth = null;
					try {
						double d = Double.parseDouble(properties.getProperty(key));
						toEarth = new Double(d);
						lmstTable.put(k, toEarth);
					} catch (Exception e) {
						lmstTable.put(k, new Double(1));
					}
				}
			}
		}
	}

}
