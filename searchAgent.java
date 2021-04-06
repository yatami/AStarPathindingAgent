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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
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

class Graph
{
	Node[] nodes;
	int size = 0;
	Graph()
	{
		nodes = new Node[100];
	}
	class Node
	{
		int value;
		ArrayList<Node> childeren;
		Node()
		{
			value = 99999;
			childeren = new ArrayList<Node>();
		}
	}
	
	public void addConnection(int p1,int p2)
	{
		nodes[p1].childeren.add(nodes[p2]);
		nodes[p2].childeren.add(nodes[p1]);
	}
	public Node getNode(int index)
	{
		return nodes[index];
	}
	public void addNode(int index, int value)
	{
		nodes[index].value = value;
	}
}

class Entry implements Comparable<Entry>
{
    public int key;
    public int value;
    public int posX;
    public int posY;
    public int edgeValue;
    public String dir;
    public Entry parent;
    
    
	public Entry(int key, int value, int x, int y, int eValue, String d,Entry p) 
	{
		super();
		this.key = key;
		this.value = value;
		posX=x;
		posY=y;
		edgeValue = eValue;
		dir =d;
		parent =p;
	}

	@Override
	public int compareTo(Entry other) 
	{
		int result = -1;
		
		if(this.value>other.value)
			result=1;
		
		return result;
	}
}


public class searchAgent extends Agent {
	private static final long serialVersionUID = -4047208702628325380L;
	private static final Logger logger = Logger.getLogger(searchAgent.class.getCanonicalName());

	private int nodeCount=0;
	private boolean pathCalculated = false;
	private int goldRequired;
	private int woodRequired;
	Stack<Entry> pathStack = null;
	private int step;
	int buildOrder=0;
	
	public searchAgent(int playernum)
	{
		super(playernum);
		
		goldRequired = 2500;
		woodRequired = 5;
		
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
	
	
		List<Integer> townhallIds = new ArrayList<Integer>();
		List<Integer> footmanIds = new ArrayList<Integer>();
		List<Integer> resourceIds = currentState.getResourceNodeIds(Type.TREE);
		
		for(int i=0; i<allUnitIds.size(); i++) 
		{
			int id = allUnitIds.get(i);
			UnitView unit = currentState.getUnit(id);
			String unitTypeName = unit.getTemplateView().getName();
			System.out.println(unitTypeName);
			if(unitTypeName.equals("TownHall"))
				townhallIds.add(id);
			if(unitTypeName.equals("Footman"))
				footmanIds.add(id);
		}
		
		// use A* to calculate shortest path
		if(!pathCalculated)
		{
			// Arrays to keep track of trees and nodes that added to graph
			boolean[][] treeArr = new boolean[currentState.getXExtent()][currentState.getYExtent()];// update size make it dynamic to maps
			boolean[][] nodeArr = new boolean[currentState.getXExtent()][currentState.getYExtent()];// update size make it dynamic to maps
			for(int i=0; i<resourceIds.size(); i++) 
			{
				ResourceView tree = currentState.getResourceNode(resourceIds.get(i));
				treeArr[tree.getXPosition()][tree.getYPosition()] = true;
				//System.out.println("x: " + tree.getXPosition() + "y: " + tree.getYPosition());
			}
			UnitView townhall = currentState.getUnit(townhallIds.get(0));
			UnitView footman = currentState.getUnit(footmanIds.get(0));
			//System.out.println("x: " + townhall.getXPosition() + "y: " + townhall.getYPosition());
			int footmenTemplateID = footmanIds.get(0);
			PriorityQueue<Entry> q = new PriorityQueue<>();
			
			
			// start with footmans position, create and iterate through other nodes
			Entry root = new Entry(0,0,footman.getXPosition(),footman.getYPosition(),0,"",null);
			extendNode(footman.getXPosition(),footman.getYPosition(),treeArr,nodeArr,q,townhall.getXPosition(),townhall.getYPosition(),0,"",root);
			System.out.println("key : " + q.peek().key + " value : " + q.peek().value + " posX: " + q.peek().posX + " posY: " + q.peek().posY + " eValue : " + q.peek().edgeValue + " direction : " + q.peek().dir);
			int eValue = q.peek().edgeValue;
			int posX = q.peek().posX;
			int posY = q.peek().posY;
			String dir = q.peek().dir;
			Entry parent = q.peek();
			q.poll();
			extendNode(posX,posY,treeArr,nodeArr,q,townhall.getXPosition(),townhall.getYPosition(),eValue,dir,parent);
			System.out.println("key : " + q.peek().key + " value : " + q.peek().value + " posX: " + q.peek().posX + " posY: " + q.peek().posY + " eValue : " + q.peek().edgeValue + " direction : " + q.peek().dir);
			 eValue = q.peek().edgeValue;
			 posX = q.peek().posX;
			 posY = q.peek().posY;
			q.poll();
			boolean solExist = false;
			Entry sol = null;
			// iterate until queue is empty or find the position of town center
			for(int i =0;i<1500;i++)
			{
				if(q.isEmpty())
				 {
					 break;
				 }
				extendNode(posX,posY,treeArr,nodeArr,q,townhall.getXPosition(),townhall.getYPosition(),eValue,dir,parent);
				System.out.println("key : " + q.peek().key + " value : " + q.peek().value + " posX: " + q.peek().posX + " posY: " + q.peek().posY + " eValue : " + q.peek().edgeValue + " direction : " + q.peek().dir);
				 eValue = q.peek().edgeValue;
				 posX = q.peek().posX;
				 posY = q.peek().posY;
				 dir = q.peek().dir;
				 parent = q.peek();
				 if(posX==townhall.getXPosition() && Math.abs(posY-townhall.getYPosition()) == 1)
				 {
					 sol = parent;
					 solExist = true;
					 break;
				 }
				 if(posY==townhall.getYPosition() && Math.abs(posX-townhall.getXPosition()) == 1)
				 {
					 sol = parent;
					 solExist = true;
					 break;
				 }
				 
				q.poll();
			}
			
			// if sol exist backtrack from last node to first node to find the shortest path
			pathStack = new Stack<Entry>(); 
			if(solExist)
			{
				while(sol !=null)
				{
					System.out.println(" sol ends with : " + sol.posX + "   " + sol.posY);
					pathStack.push(sol);
					sol = sol.parent;	
				}
			}
			pathCalculated = true;
		}
		// make footman move through the shortest path by poping the stack then attack the town center
		if(!pathStack.isEmpty())
		{
			if(pathStack.size() !=1)
			{
				int posX = pathStack.peek().posX;
				int posY = pathStack.peek().posY;
				UnitView footman = currentState.getUnit(footmanIds.get(0));
				System.out.println(footman.getXPosition());
				if(footman.getXPosition() == posX && footman.getYPosition() == posY)
				{
					pathStack.pop();
					posX = pathStack.peek().posX;
					posY = pathStack.peek().posY;
					System.out.println(posX + " " + posY);
					builder.put(footmanIds.get(0), Action.createCompoundMove(footmanIds.get(0), posX, posY));
				}
			}
			else
			{
				builder.put(footmanIds.get(0), Action.createCompoundAttack(footmanIds.get(0), townhallIds.get(0)));
			}
			
		}
		else
		{
			System.out.println("No available path.");
		}
		
		
		return builder;
	}

	// A method to expand nodes. It creates nodes from a node by using heuristic and cumulative path length.
	private void extendNode(int sX,int sY, boolean[][] treeArr,boolean[][] nodeArr, PriorityQueue<Entry> q, int tX,int tY, int cumulativeValue,String dir, Entry parent) 
	{
		
	
		
		if(nodeArr[sX][sY] == true)
		{
			System.out.println(sX + "  " + sY);
			return;
		}
			
		
		//for X+ 
		int counter=1;
		int x = sX+1;
		int y = sY;
		if(!(dir.equals("left")))
		{
		if(y-1>=0 && y<15 && x<=15)
		{
		if(treeArr[x][y] == false )
		{
			boolean shouldStop = false;
			do{
				if(treeArr[x][y+1] == false || treeArr[x][y-1] == false || treeArr[x+1][y] == true || x+1>15)
					break;
				counter++;
				x++;
			}while(!shouldStop);
			q.add(new Entry(nodeCount,Math.abs(x-sX) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue+Math.abs(x-sX),"right",parent));
			nodeCount++;
		}
		}
		else if(y-1<0  && x<=15)
		{
			if(treeArr[x][y] == false )
			{
				boolean shouldStop = false;
				do{
					if(treeArr[x][y+1] == false || treeArr[x+1][y] == true || x+1>15)
						break;
					counter++;
					x++;
				}while(!shouldStop);
				q.add(new Entry(nodeCount,Math.abs(x-sX) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue+Math.abs(x-sX),"right",parent));
				nodeCount++;
			}
		}
		else if(y == 15 && x<=15 )
		{
			if(treeArr[x][y] == false )
			{
				boolean shouldStop = false;
				do{
					if(treeArr[x][y-1] == false || treeArr[x+1][y] == true || x+1>15)
						break;
					counter++;
					x++;
				}while(!shouldStop);
				q.add(new Entry(nodeCount,Math.abs(x-sX) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue+Math.abs(x-sX),"right", parent));
				nodeCount++;
			}
		}
		}
		//for X-
				counter=1;
				x = sX-1;
				y = sY;
				if(!(dir.equals("right")))
				{
				if(y-1>=0 && y<15 && x>=0)
				{
					if(treeArr[x][y] == false)
					{
						boolean shouldStop = false;
						do{
							if(treeArr[x][y+1] == false || treeArr[x][y-1] == false || treeArr[x-1][y] == true || x-1<0)
								break;
							counter++;
							x--;
						}while(!shouldStop);
						q.add(new Entry(nodeCount,Math.abs(x-sX) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue+Math.abs(x-sX),"left", parent));
						nodeCount++;
					}
				}
				else if(y-1<0 && x>=0)
				{
					if(treeArr[x][y] == false)
					{
						boolean shouldStop = false;
						do{
							if(treeArr[x][y+1] == false || treeArr[x-1][y] == true ||  x-1<0)
								break;
							counter++;
							x--;
						}while(!shouldStop);
						q.add(new Entry(nodeCount,Math.abs(x-sX) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue+Math.abs(x-sX),"left", parent));
						nodeCount++;
					}
				}
				else if(y == 15 && x>=0)
				{
					if(treeArr[x][y] == false )
					{
						boolean shouldStop = false;
						do{
							if(treeArr[x][y-1] == false || treeArr[x-1][y] == true ||  x-1<0)
								break;
							counter++;
							x--;
						}while(!shouldStop);
						q.add(new Entry(nodeCount,Math.abs(x-sX) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue+Math.abs(x-sX),"left", parent));
						nodeCount++;
					}
				}
				}
		//for Y+
				 counter=1;
				 x = sX;
				 y = sY+1;
				 if(!(dir.equals("down")))
					{
				if(x-1>=0 && x<15 && y<=15)
				{
					if(treeArr[x][y] == false )
					{
						boolean shouldStop = false;
						do{
							if(treeArr[x+1][y] == false || treeArr[x-1][y] == false || treeArr[x][y+1] == true || y+1>15)
								break;
							counter++;
							y++;
						}while(!shouldStop);
						q.add(new Entry(nodeCount, Math.abs(y-sY) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue + Math.abs(y-sY),"up", parent ));
						nodeCount++;
					}
				}
				else if(x-1<0 && y<=15)
				{
					if(treeArr[x][y] == false)
					{
						boolean shouldStop = false;
						do{
							if(treeArr[x+1][y] == false || treeArr[x][y+1] == true || y+1>15)
								break;
							counter++;
							y++;
						}while(!shouldStop);
						q.add(new Entry(nodeCount, Math.abs(y-sY) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue + Math.abs(y-sY),"up", parent ));
						nodeCount++;
					}	
				}
				else if(x == 15 && y<=15)
				{
					if(treeArr[x][y] == false)
					{
						boolean shouldStop = false;
						do{
							if(treeArr[x-1][y] == false || treeArr[x][y+1] == true || y+1>15)
								break;
							counter++;
							y++;
						}while(!shouldStop);
						q.add(new Entry(nodeCount, Math.abs(y-sY) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue + Math.abs(y-sY),"up", parent ));
						nodeCount++;
					}
				}
					}
				 
				//for Y-
				 counter=1;
				 x = sX;
				 y = sY-1;
				 if(!(dir.equals("up")))
					{
				if(x-1>=0 && x<15 && y>=0)
				{
					if(treeArr[x][y] == false)
					{
						boolean shouldStop = false;
						do{
							if(treeArr[x+1][y] == false || treeArr[x-1][y] == false || treeArr[x][y-1] == true || y-1==0)
								break;
							counter++;
							y--;
						}while(!shouldStop);
						q.add(new Entry(nodeCount, Math.abs(y-sY) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue + Math.abs(y-sY),"down" , parent));
						nodeCount++;
					}
				}
				else if(x-1<0 && y>=0)
				{
					if(treeArr[x][y] == false)
					{
						boolean shouldStop = false;
						do{
							
							if(treeArr[x+1][y] == false || treeArr[x][y-1] == true || y-1==0)
								break;
							counter++;
							y--;
						}while(!shouldStop);
						q.add(new Entry(nodeCount, Math.abs(y-sY) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue + Math.abs(y-sY),"down" , parent));
						nodeCount++;
					}	
				}
				else if(x == 15 && y>=0)
				{
					if(treeArr[x][y] == false)
					{
						boolean shouldStop = false;
						do{
							if(treeArr[x-1][y] == false || treeArr[x][y-1] == true || y-1==0)
								break;
							counter++;
							y--;
						}while(!shouldStop);
						q.add(new Entry(nodeCount, Math.abs(y-sY) + cumulativeValue + Math.max(Math.abs(tY-y),Math.abs(tX-x)),x,y,cumulativeValue + Math.abs(y-sY),"down" , parent));
						nodeCount++;
					}
				}
					}
				 
			nodeArr[sX][sY] = true;
		
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
