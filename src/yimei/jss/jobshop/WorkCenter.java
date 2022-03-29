package yimei.jss.jobshop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yimei on 24/09/16.
 */
public class WorkCenter {

    private final int id;
    private int numMachines;

    // Attributes for simulation.
    private LinkedList<OperationOption> queue;
    private List<Double> machineReadyTimes;
    private double workInQueue;
    //numOperation in queue    modfied by fzhang 20.4.2018
    private int numOperationInQueue;
    private double busyTime;

    public WorkCenter(int id, int numMachines,
                      LinkedList<OperationOption> queue,
                      List<Double> machineReadyTimes,
                      double workInQueue, double busyTime) {
        this.id = id;
        this.numMachines = numMachines;
        this.queue = queue;
        this.machineReadyTimes = machineReadyTimes;
        this.workInQueue = workInQueue;
        this.busyTime = busyTime;
    }

    public WorkCenter(int id, int numMachines) {
        this(id, numMachines, new LinkedList<>(),
                new ArrayList<>(Collections.nCopies(numMachines, 0.0)),
                0.0, 0.0);
    }

    public WorkCenter(int id) {
        this(id, 1);
    }

    public int getId() {
        return id;
    }

    public int getNumMachines() {
        return numMachines;
    }

    public LinkedList<OperationOption> getQueue() {
        return queue;
    }

    //fzhang 1.6.2018 get the min work (with min processing time):in the queue the queue here is not the jobs before a machine  F
    public double getMinProcessTimeInQueue() {
    	if(getQueue().size() == 0)
    		return 0;
    	
    	double minProcessTime = getQueue().get(0).getProcTime();
        for(int i = 1; i< getQueue().size();i++)
        {
        	if(minProcessTime > getQueue().get(i).getProcTime()) {
        		minProcessTime = getQueue().get(i).getProcTime();
        	}
        }
		return minProcessTime;
    }

    public double getMaxProcessTimeInQueue() {
    	if(getQueue().size() == 0)
    		return 0;
    	
    	double maxProcessTime = getQueue().get(0).getProcTime();
        for(int i = 1; i< getQueue().size();i++)
        {
        	if(maxProcessTime < getQueue().get(i).getProcTime()) {
        		maxProcessTime = getQueue().get(i).getProcTime();
        	}
        }
		return maxProcessTime;
    }
    //==========================================================================================================
    public List<Double> getMachineReadyTimes() {
        return machineReadyTimes;
    }

    public double getMachineReadyTime(int idx) {
        return machineReadyTimes.get(idx);
    }

    public double getWorkInQueue() {
        return workInQueue;
    }

    public double getBusyTime() {
        return busyTime;
    }
    

   /* //Created by fzhang on 18/04/18.
    public double getAverageCostInQueue() {
    	return workInQueue/queue.size();
    }

    //Created by fzhang on 18/04/18.
    double totalAverageCostInQueue = 0;
    public double getTotalAverageCostInQueue() {
    	for(int i = 0; i< numMachines; i++) {
    		//it is not right here, the cost need to multiple a factor according to different mahcines
           totalAverageCostInQueue += workInQueue/queue.size();
    	}
        return totalAverageCostInQueue/numMachines;
    }
    //Created by fzhang on 18/04/18.
    double totalAverageProcessTimeInQueue = 0;
    public double getTotalAverageProcesTimeInQueue() {
    	for(int i = 0; i< numMachines; i++) {
    		totalAverageProcessTimeInQueue += workInQueue/queue.size();
    	}
        return totalAverageProcessTimeInQueue/numMachines;
    }

    //Created by fzhang on 20/04/18.   getAverageProcessTimeInSystem
    double averageProcessTimeInSystem = 0;
    double totalProcessTimeInSystem =0;
    public double getAverageProcesTimeInSystem() {
    	for(int i = 0; i< numMachines; i++) {
    		totalProcessTimeInSystem += workInQueue;
    	}
        return totalProcessTimeInSystem/queue.size();
    }
*/
    public double getReadyTime() {
        double readyTime = machineReadyTimes.get(0);

        for (int i = 1; i < machineReadyTimes.size(); i++) {
            double t = machineReadyTimes.get(i);
            if (readyTime > t)
                readyTime = t;
        }

        return readyTime;
    }

    public void setMachineReadyTime(int idx, double readyTime) {
        machineReadyTimes.set(idx, readyTime);
    }

    // numOperationInQueue
    public int numOpsInQueue() {
        return queue.size();
    }

    public int getNumOpsInQueue() {
        return queue.size();
    }

    public void reset(double readyTime) {
        queue.clear();
        for (int i = 0; i < numMachines; i++) {
            machineReadyTimes.set(i, readyTime);
        }
        workInQueue = 0.0;
        busyTime = readyTime;
    }

    public void reset() {
        reset(0.0);
    }

    public void addToQueue(OperationOption o) {
        queue.add(o);
        workInQueue += o.getProcTime();
    }

    public void removeFromQueue(OperationOption o) {
        queue.remove(o);
       // System.out.println("workInQueue(The last work) " + workInQueue);
        //System.out.println("The time of last operation: "+o.getProcTime());
        //method1: but when we modify the processTime to int, this should be OK.
        //workInQueue -= o.getProcTime();

        //these are for system
        //System.out.println("The workInQueue after delete the last operaiton: "+workInQueue);
        //System.out.println("The number of operation after delete the last operaiton: "+o.getNumOpsRemaining());
       //method2:
      //modified by fzhang 10.5.2018    in orde to avoid negative, also positive (very equal to 0) value of workInQueue
      		if(queue.isEmpty())
      			workInQueue = 0.0;
      		else
      			workInQueue = workInQueue-o.getProcTime();

    }

    public Machine earliestReadyMachine() {
        Machine earliestReadyMachine =
                new Machine(0, this, machineReadyTimes.get(0));
        for (int i = 1; i < machineReadyTimes.size(); i++) {
            if (machineReadyTimes.get(i) < earliestReadyMachine.getReadyTime())
                earliestReadyMachine =
                        new Machine(i, this, machineReadyTimes.get(i));
        }

        return earliestReadyMachine;
    }

    public void incrementBusyTime(double value) {
        busyTime += value;
    }

    @Override
    public String toString() {
        return "W" + id + " [" + numMachines + "]";
    }

    public boolean equals(WorkCenter other) {
        return id == other.id;
    }

    public WorkCenter clone() {
        LinkedList<OperationOption> clonedQ = new LinkedList<>(queue);
        List<Double> clonedMRT = new ArrayList<>(machineReadyTimes);

        return new WorkCenter(id, numMachines,
                clonedQ, clonedMRT, workInQueue, busyTime);
    }

    public String stateToString() {
        String string = "";
        for (int i = 0; i < machineReadyTimes.size(); i++) {
            string += String.format("(M%d,R%.1f) ", i, machineReadyTimes.get(i));
        }
        string += "\n Queue: ";
        for (OperationOption o : queue) {
            string += String.format("(J%d,O%d-%d,R%.1f) ",
                    o.getOperation().getJob().getId(), o.getOperation().getId(),
                    o.getOptionId(), o.getReadyTime());
        }
        string += "\n";

        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkCenter that = (WorkCenter) o;

        if (id != that.id) return false;
        if (numMachines != that.numMachines) return false;
        if (Double.compare(that.workInQueue, workInQueue) != 0) return false;
        if (Double.compare(that.busyTime, busyTime) != 0) return false;
        if (queue != null ? !queue.equals(that.queue) : that.queue != null) return false;
        return machineReadyTimes != null ? machineReadyTimes.equals(that.machineReadyTimes) : that.machineReadyTimes == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id;
        result = 31 * result + numMachines;
        result = 31 * result + (queue != null ? queue.hashCode() : 0);
        result = 31 * result + (machineReadyTimes != null ? machineReadyTimes.hashCode() : 0);
        temp = Double.doubleToLongBits(workInQueue);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(busyTime);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
