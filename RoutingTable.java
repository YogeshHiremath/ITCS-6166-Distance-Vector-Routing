import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RoutingTable implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<RTableEntry> table = new ArrayList<RTableEntry>();
	private double infinity = 16.0; //to avoid count to infinity problem
	private HashMap<String, Double> originals = new HashMap<String, Double>();
	private ArrayList<String> immediateNeighbors = new ArrayList<String>();
	private String routerName;
	private transient BufferedReader brObj;
	private String fileName;

	public String getRouterName() {
		return routerName;
	}

	public void setRouterName(String routerName) {
		this.routerName = routerName;
	}

	public ArrayList<String> getImmediateNeighbors() {
		return immediateNeighbors;
	}

	public void setImmediateNeighbors(ArrayList<String> immediateNeighbors) {
		this.immediateNeighbors = immediateNeighbors;
	}

	public ArrayList<RTableEntry> getTable() {
		return table;
	}

	public void setTable(ArrayList<RTableEntry> table) {
		this.table = table;
	}

	public RoutingTable() {

	}

	public RoutingTable(RoutingTable instance) {
		this.table = instance.table;
	}

	//construct table from file
	public void constructInitialTable(String fileName) throws IOException {
		RTableEntry rObj;
		String[] fileParts = fileName.split(Pattern.quote("."));
		String routerName = String.valueOf(fileParts[0].charAt(fileParts[0].length()-1));
		this.fileName = fileName;
		this.routerName = routerName;
		//entry for the same router
		rObj = new RTableEntry(routerName, routerName, "-", 0.0);
		immediateNeighbors.add(routerName);
		table.add(rObj);
		originals.put(routerName, 0.0);

		//reading rest of the entries
		FileReader file = new FileReader(new File(fileName));		
		brObj = new BufferedReader(file);
		String line;
		while((line = brObj.readLine())!=null) {
			String[] lineParts = line.split("\\s+");
			if(lineParts.length == 2) {
				String dest = lineParts[0];
				Double cost = Double.parseDouble(lineParts[1]);
				rObj = new RTableEntry(routerName, dest, dest, cost);
				immediateNeighbors.add(dest);
				table.add(rObj);
				originals.put(dest, cost);
			}
		}
	}

	// REMEMBER!!! The received table here is table being sent from the other router
	public synchronized void updateRouterTable(RoutingTable ownTable, RoutingTable receivedTable) {

		//check for new entries in the received table and add them to own table
		ArrayList<String> receivedNeighbors = new ArrayList<String>(receivedTable.getImmediateNeighbors());
		ArrayList<String> ownNeighbors = new ArrayList<String>(ownTable.getImmediateNeighbors());
		for(String neighbor : receivedNeighbors) {
			if(!ownNeighbors.contains(neighbor)) {
				RTableEntry rObj = new RTableEntry(ownTable.getRouterName(), neighbor, "-", infinity);
				ownTable.table.add(rObj);
				ownTable.immediateNeighbors.add(neighbor);
			}
		}

		//modify received table, add the distance between ownNode and the other node to all entries in the received table
		RoutingTable receivedModified = new RoutingTable(receivedTable);
		Double addThis = 0.0;
		for (RTableEntry entry : receivedTable.getTable()) {
			if(entry.getDest().equals(ownTable.getRouterName())) {
				addThis = entry.getCost();
				break;
			}
		}

		for (RTableEntry entry : receivedModified.getTable()) {
			entry.setNextHop(entry.getSource());
			Double costOriginal = entry.getCost();
			entry.setCost(costOriginal+addThis);
		}

		//fix 2 update problem by advertising the cost as infinity if the next node in table received is the router receiving it
		for (RTableEntry entry : receivedModified.getTable()) {
			if(entry.getNextHop().equals(ownTable.getRouterName())) {
				entry.setCost(Double.MAX_VALUE);
			}
		}

		//compare receivedModified and own table and make updates
		for (RTableEntry entryInReceived : receivedModified.getTable()) {
			String destR = entryInReceived.getDest();
			String hopR = entryInReceived.getNextHop();
			Double costR = entryInReceived.getCost();
			for (RTableEntry entryInOwn : ownTable.getTable()) {
				//for the same destination
				if(entryInOwn.getDest().equals(entryInOwn.getSource())) {
					continue;
				}
				if((entryInOwn.getDest().equals(destR))) {
					if(!entryInOwn.getNextHop().equals(hopR)) {
						if (entryInOwn.getCost() > costR) {
							entryInOwn.setCost(costR);
							entryInOwn.setNextHop(hopR);
						}
					}
					else {
						entryInOwn.setCost(costR);
						entryInOwn.setNextHop(hopR);
					}
				}
			}
		}
	}

	//method to print table in the desired format
	public synchronized void printTable(ArrayList<RTableEntry> t) {
		for (RTableEntry entry : t) {
			System.out.println("Shortest path "+entry.getSource()+"-"+entry.getDest()+": the next hop is "+entry.getNextHop()+" and the cost is "+entry.getCost());
		}
	}

	//method to check link state change
	public synchronized void checkLinkCostChange() throws NumberFormatException, FileNotFoundException, IOException {
		//read the file again and save values in a new RoutingTable object
		RoutingTable newTable = new RoutingTable();
		RTableEntry rObj1;
		//for changed routers
		ArrayList<String> changedRouters = new ArrayList<String>();
		String[] fileParts = this.fileName.split(Pattern.quote("."));
		String routerName = String.valueOf(fileParts[0].charAt(fileParts[0].length()-1));
		//entry for the same router
		rObj1 = new RTableEntry(routerName, routerName, "-", 0.0);
		//newTable.immediateNeighbors.add(routerName);
		newTable.table.add(rObj1);
		//reading rest of the entries
		FileReader file = new FileReader(new File(this.fileName));		
		brObj = new BufferedReader(file);
		String line;
		while((line = brObj.readLine())!=null) {
			String[] lineParts = line.split("\\s+");
			if(lineParts.length == 2) {
				String dest = lineParts[0];
				Double cost = Double.parseDouble(lineParts[1]);
				rObj1 = new RTableEntry(routerName, dest, dest, cost);
				//newTable.immediateNeighbors.add(dest);
				newTable.table.add(rObj1);
			}
		}

		//compare new link costs with original link costs, if change add them to changedRouters arraylist
		for (Map.Entry<String, Double> entry : originals.entrySet()) {
			for (RTableEntry entryNew : newTable.getTable()) {
				if(entryNew.getDest().equals(entry.getKey())) {
					if(Double.compare(entryNew.getCost(), entry.getValue()) != 0) {
						System.out.println("Change in Link State Detected!");
						changedRouters.add(entry.getKey());
					}
				}
			}
		}

		//if the routers are in changedRouters then change their cost to the new cost
		for (RTableEntry entry : table) {
			for (RTableEntry entryNew : newTable.getTable()) {
				if(entryNew.getDest().equals(entry.getDest())) {
					if(changedRouters.contains(entryNew.getDest())) {
						entry.setCost(entryNew.getCost());
						originals.put(entryNew.getDest(), entryNew.getCost());
						System.out.println("Change in Link State Fixed!");
					}
				}
			}
		}
	}
}
