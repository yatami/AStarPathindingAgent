package tt;
/**
 *  Strategy Engine for Programming Intelligent Agents (SEPIA)
    Copyright (C) 2012 Case Western Reserve University

    This file is part of SEPIA.

    SEPIA is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SEPIA is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SEPIA.  If not, see <http://www.gnu.org/licenses/>.
 */
//package edu.cwru.sepia.agent;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.experiment.Configuration;
import edu.cwru.sepia.experiment.ConfigurationValues;
import edu.cwru.sepia.agent.Agent;

/**
 * This agent will first collect gold to produce a peasant,
 * then the two peasants will collect gold and wood separately until reach goal.
 * @author Feng
 *
 */
public class ResourceCollectionAgent extends Agent {
	private static final long serialVersionUID = -4047208702628325380L;
	private static final Logger logger = Logger.getLogger(ResourceCollectionAgent.class.getCanonicalName());

	private int goldRequired;
	private int woodRequired;
	
	private int step;
	int buildOrder=0;
	
	public ResourceCollectionAgent(int playernum, String[] arguments)
	{
		super(playernum);
		
		goldRequired = 2500;
		woodRequired = Integer.parseInt(arguments[1]);
		
	}

	StateView currentState;
	
	@Override
	public Map<Integer, Action> initialStep(StateView newstate, History.HistoryView statehistory) 
	{
		step = 0;
		return middleStep(newstate, statehistory);
	}

	@Override
	public Map<Integer,Action> middleStep(StateView newState, History.HistoryView statehistory) {
		step++;
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("=> Step: " + step);
		}
		
		Map<Integer,Action> builder = new HashMap<Integer,Action>();
		currentState = newState;
		
		
		
		int currentGold = currentState.getResourceAmount(0, ResourceType.GOLD);
		int currentWood = currentState.getResourceAmount(0, ResourceType.WOOD);
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Gold: " + currentGold);
		}
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Wood: " + currentWood);
		}
		List<Integer> allUnitIds = currentState.getAllUnitIds();
		 List<Integer> enemyUnitIDs = currentState.getUnitIds(1);
		System.out.println(currentState.getUnit(enemyUnitIDs.get(0)).getTemplateView().getName() + " the one "); 
		List<Integer> peasantIds = new ArrayList<Integer>();
		List<Integer> townhallIds = new ArrayList<Integer>();
		List<Integer> barracksIds = new ArrayList<Integer>();
		List<Integer> footmanIds = new ArrayList<Integer>();
		for(int i=0; i<allUnitIds.size(); i++) {
			int id = allUnitIds.get(i);
			UnitView unit = currentState.getUnit(id);
			String unitTypeName = unit.getTemplateView().getName();
			System.out.println(unitTypeName);
			if(unitTypeName.equals("TownHall"))
				townhallIds.add(id);
			if(unitTypeName.equals("Peasant"))
				peasantIds.add(id);
			if(unitTypeName.equals("Barracks"))
				barracksIds.add(id);
			if(unitTypeName.equals("Footman"))
				footmanIds.add(id);
		}
		
		if(peasantIds.size()>=3) {// collect resources
			if(currentWood<woodRequired && buildOrder<2) 
			{
				int peasantId = peasantIds.get(1);
				int townhallId = townhallIds.get(0);
				Action b = null;
				if(currentState.getUnit(peasantId).getCargoAmount()>0)
					b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townhallId);
				else 
				{
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.TREE);
					b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(peasantId, b);
			}
			else if(buildOrder == 2)
			{
				int peasantId = peasantIds.get(1);
				int townhallId = townhallIds.get(0);
				Action b = null;
				if(currentState.getUnit(peasantId).getCargoType() == ResourceType.GOLD && currentState.getUnit(peasantId).getCargoAmount()>0)
					b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townhallId);
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(peasantId, b);
				
			}
			if(currentGold<goldRequired) {
				int peasantId = peasantIds.get(0);
				int peasantId2 = peasantIds.get(2);
				int townhallId = townhallIds.get(0);
				Action b = null;
				Action c = null;
				
				if(currentState.getUnit(peasantId).getCargoType() == ResourceType.GOLD && currentState.getUnit(peasantId).getCargoAmount()>0)
					b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townhallId);
				else 
				{
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				if(currentState.getUnit(peasantId2).getCargoType() == ResourceType.GOLD && currentState.getUnit(peasantId2).getCargoAmount()>0)
					c = new TargetedAction(peasantId2, ActionType.COMPOUNDDEPOSIT, townhallId);
				else 
				{
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					c = new TargetedAction(peasantId2, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(peasantId, b);
				builder.put(peasantId2, c);
			}
			// create farm when enough gold and wood collected
			if(currentGold>500 && currentWood>250)
			{
				if(buildOrder==0)
				{
					System.out.println("got in");
					TemplateView farmTemplate = currentState.getTemplate(playernum, "Farm");
					int farmtemp = farmTemplate.getID();
					int peasantId = peasantIds.get(0);
					UnitView unitview = currentState.getUnit(peasantId);
					
					System.out.println(builder.get(peasantId));
					builder.put(peasantId, Action.createCompoundBuild(peasantId, farmtemp, unitview.getXPosition(), unitview.getYPosition()));
					buildOrder++;	
				}
			}
			//create barracks when enough gold and wood collected
			if(currentGold>700 && currentWood>400)
			{
				if(buildOrder==1)
				{
				TemplateView barracksTemplate = currentState.getTemplate(playernum, "Barracks");
				int barrackstemp = barracksTemplate.getID();
				int peasantId = peasantIds.get(0);
				UnitView unitview = currentState.getUnit(peasantId);
				
				builder.put(peasantId, Action.createCompoundBuild(peasantId, barrackstemp, unitview.getXPosition(), unitview.getYPosition()));
				buildOrder++;
				}
			}
		
			
		}
		else {  // build 3 peasants
			if(currentGold>=400) {
				if(logger.isLoggable(Level.FINE))
				{
					logger.fine("already have enough gold to produce a new peasant.");
				}
				TemplateView peasanttemplate = currentState.getTemplate(playernum, "Peasant");
				int peasanttemplateID = peasanttemplate.getID();
				if(logger.isLoggable(Level.FINE))
				{
					logger.fine(String.valueOf(peasanttemplate.getID()));
				}
				int townhallID = townhallIds.get(0);
					builder.put(townhallID, Action.createCompoundProduction(townhallID, peasanttemplateID));
			} else {
				//	System.out.println("Collecting gold");
				int peasantId = peasantIds.get(0);
				int townhallId = townhallIds.get(0);
				Action b = null;
				if(currentState.getUnit(peasantId).getCargoType() == ResourceType.GOLD && currentState.getUnit(peasantId).getCargoAmount()>0)
					b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townhallId);
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(peasantId, b);
			}
		}
		// build two footman
		if(currentGold>800 && footmanIds.size()!=3 && barracksIds.get(0) != null)
		{
			TemplateView footmenTemplate = currentState.getTemplate(playernum, "Footman");
			int footmenTemplateID = footmenTemplate.getID();
			int barracksID = barracksIds.get(0);
			builder.put(barracksID, Action.createCompoundProduction(barracksID, footmenTemplateID));
			
		}
		System.out.println(footmanIds.size());
		// command footmans to kill enemy footman
		if(footmanIds.size()==3)
		{
			int footmenTemplateID = footmanIds.get(0);
			int footmenTemplateID2 = footmanIds.get(2);
			
			
			builder.put(footmenTemplateID, Action.createCompoundAttack(footmenTemplateID, enemyUnitIDs.get(0)));
			
		    builder.put(footmenTemplateID2, Action.createCompoundAttack(footmenTemplateID2, enemyUnitIDs.get(0)));
		}
		
		return builder;
	}

	@Override
	public void terminalStep(StateView newstate, History.HistoryView statehistory)
	{
		step++;
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("=> Step: " + step);
		}
		
		int currentGold = newstate.getResourceAmount(0, ResourceType.GOLD);
		int currentWood = newstate.getResourceAmount(0, ResourceType.WOOD);
		
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Gold: " + currentGold);
		}
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Wood: " + currentWood);
		}
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Congratulations! You have finished the task!");
		}
	}
	
	public static String getUsage() 
	{
		return "Two arguments, amount of gold to gather and amount of wood to gather";
	}
	@Override
	public void savePlayerData(OutputStream os) {
		//this agent lacks learning and so has nothing to persist.
		
	}
	@Override
	public void loadPlayerData(InputStream is) {
		//this agent lacks learning and so has nothing to persist.
	}
}
