package yimei.jss.gp.terminal;

import org.apache.commons.lang3.math.NumberUtils;
import yimei.jss.jobshop.OperationOption;
import yimei.jss.jobshop.WorkCenter;
import yimei.jss.simulation.state.SystemState;

import java.util.*;

/**
 * The attributes of the job shop.
 * NOTE: All the attributes are relative to the current time.
 *       This is for making the decision making process memoryless,
 *       i.e. independent of the current time.
 *
 * @author yimei
 */

public enum JobShopAttribute {
    CURRENT_TIME("t"), // the current time

    // The machine-related attributes (independent of the jobs in the queue of the machine).
    NUM_OPS_IN_QUEUE("NIQ"), // the number of operations in the queue
    WORK_IN_QUEUE("WIQ"), // the work in the queue
    MACHINE_READY_TIME("MRT"), // the ready time of the machine

    // The job/operation-related attributes (depend on the jobs in the queue).
    PROC_TIME("PT"), // the processing time of the operation
    NEXT_PROC_TIME("NPT"), // the processing time of the next operation

    //modified by fzhang 31.5.2018
    LEAST_NEXT_PROC_TIME("LNPT"),
    MAX_NEXT_PROC_TIME("MNPT"),
    MEDIAN_NEXT_PROC_TIME("DNPT"),

    OP_READY_TIME("ORT"), // the ready time of the operation
    //NEXT_READY_TIME("NRT"), // the ready time of the next machine
    WORK_REMAINING("WKR"), // the work remaining
    NUM_OPS_REMAINING("NOR"), // the number of operations remaining
    //WORK_IN_NEXT_QUEUE("WINQ"), // the work in the next queue
    //NUM_OPS_IN_NEXT_QUEUE("NINQ"), // number of operations in the next queue
    //FLOW_DUE_DATE("FDD"), // the flow due date
    DUE_DATE("DD"), // the due date
    WEIGHT("W"), // the job weight
    ARRIVAL_TIME("AT"), // the arrival time

    // Relative version of the absolute time attributes
    MACHINE_WAITING_TIME("MWT"), // the waiting time of the machine = t - MRT
    OP_WAITING_TIME("OWT"), // the waiting time of the operation = t - ORT ---that is, time in queue(TIQ), how long an operation 
    //will in a queue
    //NEXT_WAITING_TIME("NWT"), // the waiting time for the next machine to be ready = NRT - t
    RELATIVE_FLOW_DUE_DATE("rFDD"), // the relative flow due date = FDD - t
    RELATIVE_DUE_DATE("rDD"), // the relative due date = DD - t

    //modified by fzhang 31.5.2018
    //information about jobs
    INTER_ARRIVAL_TIME_MEAN("IATM"),

    //modified by fzhang 31.5.2018 job-related
    DEVIATION_OF_JOB_IN_QUEUE("DJ"),

    //modified by fzhang  24.5.2018
	//information about the whole system  routing
    MACHINE_WORKLOAD_RATIO("MWR"),

   /* LEAST_MACHINE_WORKLOAD_RATIO("LWR"),  //for one machine, the workload over the workInSystem,ratio
    MAX_MACHINE_WORKLOAD_RATIO("MWR"),
    AVE_MACHINE_WORKLOAD_RATIO("AWR"),*/

    MACHINE_NUM_OPERATION_RATIO("MNR"),
    NUM_CANDIATE_MACHINE("NCM"), //for each job, if it has more options, maybe do not need to assign it to a machine, low chosen cost
                                 //for job has very limited candiate machines, maybe need give more attention
    AVE_PROC_TIME_IN_QUEUE("APTQ"),        //for one machine, the avearge needed time to finifsh all current jobs

    AVE_WORKLOAD_IN_SYSTEME("AWIS"), //the average workload for each machine (in current system)
    AVE_NUM_OPERATION_IN_SYSTEME("AOIS"), //the average number of operations for each machine (in current system)

    //look-ahead
    LEAST_WORK_IN_NEXT_QUEUE("LWINQ"), //among the candiate machines, which one has the least work in queue
    MAX_WORK_IN_NEXT_QUEUE("MWINQ"),  //among the candiate machines, which one has the max work in queue
    AVE_WORK_IN_NEXT_QUEUE("AWINQ"),  //for candiate machines, the average work in queue

    LEAST_NUM_OPERATIOM_IN_NEXT_QUEUE("LOINQ"),
    MAX_NUM_OPERATIOM_IN_NEXT_QUEUE("MOINQ"),
    AVE_NUM_OPERATIOM_IN_NEXT_QUEUE("AOINQ"),
    
    //fzhang 19.7.2018 current information
    TOTAL_WORK_IN_SYSTEM("TWIS"),
    TOTAL_OPERATION_IN_SYSTEM("TOIS"),
    
    //fzhang 19.7.2018 history terminals
    BUSY_TIME("BT"),
    AVERAGE_BUSY_TIME("ABT"),
    NUM_COMPLETED_JOB("NCJ"),
    
    // Used in Su's paper
    TIME_IN_SYSTEM("TIS"), // time in system = t - releaseTime
    SLACK("SL"); // the slack

    private final String name;

    JobShopAttribute(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // Reverse-lookup map
    private static final Map<String, JobShopAttribute> lookup = new HashMap<>();

    static {
        for (JobShopAttribute a : JobShopAttribute.values()) {
            lookup.put(a.getName(), a);
        }
    }

    public static JobShopAttribute get(String name) {
        return lookup.get(name);
    }

    public double value(OperationOption op, WorkCenter workCenter, SystemState systemState
    		) {
        double value = -1;

        switch (this) {
            case CURRENT_TIME:
                value = systemState.getClockTime();
                break;
            case NUM_OPS_IN_QUEUE:
                value = workCenter.getQueue().size();
                break;
            case WORK_IN_QUEUE:
                value = workCenter.getWorkInQueue();
                break;
            case MACHINE_READY_TIME:
                value = workCenter.getReadyTime();
                break;
            case MACHINE_WAITING_TIME:
                value = systemState.getClockTime() - workCenter.getReadyTime();
                break;
            case PROC_TIME:
                value = op.getProcTime();
                break;

            //modified by fzhang 31.5.2018  next processing time
            case NEXT_PROC_TIME:
                value = op.getNextProcTime();
                break;

            case LEAST_NEXT_PROC_TIME:
                value = systemState.getMinNextProcessTime(op.getOperation());
                break;
            case MAX_NEXT_PROC_TIME:
                value = systemState.getMaxNextProcessTime(op.getOperation());
                break;
            case MEDIAN_NEXT_PROC_TIME:
                value = systemState.getMedianNextProcessTime(op.getOperation());
                break;

            case OP_READY_TIME:
                value = systemState.getClockTime();
                break;
            case OP_WAITING_TIME:
                value = systemState.getClockTime() - op.getReadyTime();
                //System.out.println("OWT" + value);
                break;
//            case NEXT_READY_TIME:
//                value = systemState.nextReadyTime(op);
//                break;
//            case NEXT_WAITING_TIME:
//                value = systemState.nextReadyTime(op) - systemState.getClockTime();
//                break;
            case WORK_REMAINING:
                value = op.getWorkRemaining();
                break;
            case NUM_OPS_REMAINING:
                value = op.getNumOpsRemaining();
                break;
//            case WORK_IN_NEXT_QUEUE:
//                value = systemState.workInNextQueue(op);
//                break;
//            case NUM_OPS_IN_NEXT_QUEUE:
//                value = systemState.numOpsInNextQueue(op);
//                break;
//            case FLOW_DUE_DATE:
//                value = op.getFlowDueDate();
//                break;
            case RELATIVE_FLOW_DUE_DATE:
                value = op.getFlowDueDate() - systemState.getClockTime();
                break;
            case DUE_DATE:
                value = op.getJob().getDueDate();
                break;
            case RELATIVE_DUE_DATE:
                value = op.getJob().getDueDate() - systemState.getClockTime();
                break;
            case WEIGHT:
                value = op.getJob().getWeight();
                break;
            case ARRIVAL_TIME:
                value = op.getJob().getArrivalTime();
                break;
            case TIME_IN_SYSTEM:
                value = systemState.getClockTime() - op.getJob().getReleaseTime();
                break;
            case SLACK:
                value = op.getJob().getDueDate() - systemState.getClockTime() - op.getWorkRemaining();
                break;

            //==============================================================================
            case MACHINE_WORKLOAD_RATIO:
                value =  workCenter.getWorkInQueue()/systemState.getWorkInSystem();
                break;

             case MACHINE_NUM_OPERATION_RATIO:
                 value = workCenter.getNumOpsInQueue()/systemState.getNumOfOperationInSystem();
                 break;
             case NUM_CANDIATE_MACHINE:
                 value = op.getOperation().getOperationOptions().size();
                 break;
             case AVE_PROC_TIME_IN_QUEUE:
            	 if(workCenter.getWorkInQueue() == 0 ||workCenter.getNumOpsInQueue() == 0)
            		 value = 0;
            	 else
            		 value = workCenter.getWorkInQueue()/workCenter.getNumOpsInQueue();
                 break;

             //information of systemstate
             case AVE_WORKLOAD_IN_SYSTEME:
            	 value = systemState.getWorkInSystem()/systemState.getWorkCenters().size();
                 break;
             case AVE_NUM_OPERATION_IN_SYSTEME:
            	 value = systemState.getNumOfOperationInSystem()/systemState.getWorkCenters().size();
            	 break;

              //look-ahead, work in next queue (WINQ) and number of operations in queue (NOINQ)
             case LEAST_WORK_IN_NEXT_QUEUE:
                 value = systemState.getMinWorkInNextQueue(op.getOperation());
                 break;
             case MAX_WORK_IN_NEXT_QUEUE:
            	 value = systemState.getMaxWorkInNextQueue(op.getOperation());
                 break;
             case AVE_WORK_IN_NEXT_QUEUE:
            	 value = systemState.getAvgWorkInNextQueue(op.getOperation());
                 break;

             case LEAST_NUM_OPERATIOM_IN_NEXT_QUEUE:
                 value = systemState.getMinNumOperationInNextQueue(op.getOperation());
                 break;
             case MAX_NUM_OPERATIOM_IN_NEXT_QUEUE:
            	 value = systemState.getMaxNumOperationInNextQueue(op.getOperation());
                 break;
             case AVE_NUM_OPERATIOM_IN_NEXT_QUEUE:
            	 value = systemState.getAveNumOperationInNextQueue(op.getOperation());
                 break;

             case DEVIATION_OF_JOB_IN_QUEUE:
            	 value = workCenter.getMinProcessTimeInQueue()/workCenter.getMaxProcessTimeInQueue();
            	 break;
            	 
             //fzhang 19.7.2018  add information of current system
             case TOTAL_WORK_IN_SYSTEM:
            	 value = systemState.getWorkInSystem();
            	 break;
             case TOTAL_OPERATION_IN_SYSTEM:
            	 value = systemState.getNumOfOperationInSystem();
            	 break;
            //fzhang 19.7.2018  add history information
             case BUSY_TIME:
            	 value = workCenter.getBusyTime();
            	 break;
             case AVERAGE_BUSY_TIME:
            	 value = systemState.getTotalBusyTime()/systemState.getWorkCenters().size();
            	 break;         
             case NUM_COMPLETED_JOB:
            	 value = systemState.getJobsCompleted().size();
            	 break;
            	 
           default:
                System.err.println("Undefined attribute " + name);
                System.exit(1);
        }

        return value;
    }

    public static double valueOfString(String attribute, OperationOption op, WorkCenter workCenter,
                                       SystemState systemState,
                                       List<JobShopAttribute> ignoredAttributes) {
        JobShopAttribute a = get(attribute);
        if (a == null) {
            if (NumberUtils.isNumber(attribute)) {
                return Double.valueOf(attribute);
            } else {
                System.err.println(attribute + " is neither a defined attribute nor a number.");
                System.exit(1);
            }
        }

        if (ignoredAttributes.contains(a)) {
            return 1.0;
        } else {
        	  return a.value(op, workCenter, systemState);
        }
    }

    /**
     * Return the basic attributes.
     * @return the basic attributes.
     */
    public static JobShopAttribute[] basicAttributes() {
        return new JobShopAttribute[]{
                JobShopAttribute.CURRENT_TIME,
                JobShopAttribute.NUM_OPS_IN_QUEUE,
                JobShopAttribute.WORK_IN_QUEUE,
                JobShopAttribute.MACHINE_READY_TIME,
                JobShopAttribute.PROC_TIME,
                JobShopAttribute.NEXT_PROC_TIME,
                JobShopAttribute.OP_READY_TIME,
                //JobShopAttribute.NEXT_READY_TIME,
                JobShopAttribute.WORK_REMAINING,
                JobShopAttribute.NUM_OPS_REMAINING,
                //JobShopAttribute.WORK_IN_NEXT_QUEUE,
                //JobShopAttribute.NUM_OPS_IN_NEXT_QUEUE,
                //JobShopAttribute.FLOW_DUE_DATE,
                JobShopAttribute.DUE_DATE,
                JobShopAttribute.WEIGHT,

                JobShopAttribute.ARRIVAL_TIME,
                JobShopAttribute.SLACK
        };
    }

    /**
     * The attributes relative to the current time.
     * @return the relative attributes.
     */
    //for flexible JSSP
    //fzhang 19.7.2018 for flexible, the next processing time do not know, because we do not know the next operation will
    //be allocated in which machine:  baseline
    public static JobShopAttribute[] relativeAttributes() {
        return new JobShopAttribute[]{
                JobShopAttribute.NUM_OPS_IN_QUEUE,
                JobShopAttribute.WORK_IN_QUEUE,
                JobShopAttribute.MACHINE_WAITING_TIME,
                JobShopAttribute.PROC_TIME,
                JobShopAttribute.NEXT_PROC_TIME,
                JobShopAttribute.OP_WAITING_TIME,
                JobShopAttribute.WORK_REMAINING,
                JobShopAttribute.NUM_OPS_REMAINING,
                JobShopAttribute.WEIGHT,
                JobShopAttribute.TIME_IN_SYSTEM,
        };
    }

    //fzhang 19.7.2018 consider other current attributes: baseline
    public static JobShopAttribute[] relativeCurrentAttributes() {
        return new JobShopAttribute[]{
                JobShopAttribute.NUM_OPS_IN_QUEUE,
                JobShopAttribute.WORK_IN_QUEUE,
                JobShopAttribute.MACHINE_WAITING_TIME,
                JobShopAttribute.PROC_TIME,
                //JobShopAttribute.NEXT_PROC_TIME,
                JobShopAttribute.OP_WAITING_TIME,
                JobShopAttribute.WORK_REMAINING,
                JobShopAttribute.NUM_OPS_REMAINING,
                JobShopAttribute.WEIGHT,
                JobShopAttribute.TIME_IN_SYSTEM,
                
                //new attribute
                JobShopAttribute.NUM_CANDIATE_MACHINE,  //4
                JobShopAttribute.MACHINE_WORKLOAD_RATIO, //5
                JobShopAttribute.MACHINE_NUM_OPERATION_RATIO,//6

                JobShopAttribute.AVE_PROC_TIME_IN_QUEUE, //7

                JobShopAttribute.AVE_WORKLOAD_IN_SYSTEME, //8
                JobShopAttribute.AVE_NUM_OPERATION_IN_SYSTEME, //9
                JobShopAttribute.DEVIATION_OF_JOB_IN_QUEUE, //16
                
                JobShopAttribute.TOTAL_WORK_IN_SYSTEM,
                JobShopAttribute.TOTAL_OPERATION_IN_SYSTEM,             
        };
    }
    
    //fzhang 19.7.2018 consider other current attributes: baseline
    public static JobShopAttribute[] relativeFutureAttributes() {
        return new JobShopAttribute[]{
                JobShopAttribute.NUM_OPS_IN_QUEUE,
                JobShopAttribute.WORK_IN_QUEUE,
                JobShopAttribute.MACHINE_WAITING_TIME,
                JobShopAttribute.PROC_TIME,
                //JobShopAttribute.NEXT_PROC_TIME,
                JobShopAttribute.OP_WAITING_TIME,
                JobShopAttribute.WORK_REMAINING,
                JobShopAttribute.NUM_OPS_REMAINING,
                JobShopAttribute.WEIGHT,
                JobShopAttribute.TIME_IN_SYSTEM,
                
                //new terminals
                //next processing time   fzhang 31.5.2018 for flexible
                JobShopAttribute.LEAST_NEXT_PROC_TIME,  //1
                JobShopAttribute.MAX_NEXT_PROC_TIME,    //2
                JobShopAttribute.MEDIAN_NEXT_PROC_TIME, //3
                
                //Work in next queue
                JobShopAttribute.LEAST_WORK_IN_NEXT_QUEUE, //10
                JobShopAttribute.MAX_WORK_IN_NEXT_QUEUE, //11
                JobShopAttribute.AVE_WORK_IN_NEXT_QUEUE, //12

                //number of operations in next queue
                JobShopAttribute.LEAST_NUM_OPERATIOM_IN_NEXT_QUEUE, //13
                JobShopAttribute.MAX_NUM_OPERATIOM_IN_NEXT_QUEUE, //14
                JobShopAttribute.AVE_NUM_OPERATIOM_IN_NEXT_QUEUE, //15
        };
    }
    
    //fzhang 19.7.2018 consider other current attributes: baseline
    public static JobShopAttribute[] relativeHistoryAttributes() {
        return new JobShopAttribute[]{
                JobShopAttribute.NUM_OPS_IN_QUEUE,
                JobShopAttribute.WORK_IN_QUEUE,
                JobShopAttribute.MACHINE_WAITING_TIME,
                JobShopAttribute.PROC_TIME,
                //JobShopAttribute.NEXT_PROC_TIME,
                JobShopAttribute.OP_WAITING_TIME,
                JobShopAttribute.WORK_REMAINING,
                JobShopAttribute.NUM_OPS_REMAINING,
                JobShopAttribute.WEIGHT,
                JobShopAttribute.TIME_IN_SYSTEM,
                
                //new terminals
                JobShopAttribute.BUSY_TIME,
                JobShopAttribute.AVERAGE_BUSY_TIME,
                JobShopAttribute.NUM_COMPLETED_JOB,
               
        };
    }
    
    //fzhang  ignore weigth in non-weight objective, finally found that the result has no obvious difference.
    public static JobShopAttribute[] relativeWithoutWeightAttributes() {
        return new JobShopAttribute[]{
                JobShopAttribute.NUM_OPS_IN_QUEUE,
                JobShopAttribute.WORK_IN_QUEUE,
                JobShopAttribute.MACHINE_WAITING_TIME,
                JobShopAttribute.PROC_TIME,
                JobShopAttribute.NEXT_PROC_TIME,
                JobShopAttribute.OP_WAITING_TIME,
                JobShopAttribute.WORK_REMAINING,
                JobShopAttribute.NUM_OPS_REMAINING,
                JobShopAttribute.TIME_IN_SYSTEM,
        };
    }
    
    //modified by fzhang  24.5.2018  add some terminals for flexible job shop scheduling: especially terminals related to the system
    public static JobShopAttribute[] systemstateAttributes() {
        return new JobShopAttribute[]{
        		
                JobShopAttribute.NUM_OPS_IN_QUEUE,
                JobShopAttribute.WORK_IN_QUEUE,
                JobShopAttribute.MACHINE_WAITING_TIME,
                JobShopAttribute.PROC_TIME,
                //JobShopAttribute.NEXT_PROC_TIME,

                //next processing time   fzhang 31.5.2018 for flexible
                JobShopAttribute.LEAST_NEXT_PROC_TIME,  //1
                JobShopAttribute.MAX_NEXT_PROC_TIME,    //2
                JobShopAttribute.MEDIAN_NEXT_PROC_TIME, //3
                //-----------------------------------------------------

                JobShopAttribute.OP_WAITING_TIME,
                JobShopAttribute.WORK_REMAINING,
                JobShopAttribute.NUM_OPS_REMAINING,
                JobShopAttribute.WEIGHT,
                JobShopAttribute.TIME_IN_SYSTEM,
                //modified by fzhang 26.5.2018
                //new terminals
                JobShopAttribute.NUM_CANDIATE_MACHINE,  //4
                JobShopAttribute.MACHINE_WORKLOAD_RATIO, //5
                JobShopAttribute.MACHINE_NUM_OPERATION_RATIO,//6

                JobShopAttribute.AVE_PROC_TIME_IN_QUEUE, //7

                JobShopAttribute.AVE_WORKLOAD_IN_SYSTEME, //8
                JobShopAttribute.AVE_NUM_OPERATION_IN_SYSTEME, //9

                //Work in next queue
                JobShopAttribute.LEAST_WORK_IN_NEXT_QUEUE, //10
                JobShopAttribute.MAX_WORK_IN_NEXT_QUEUE, //11
                JobShopAttribute.AVE_WORK_IN_NEXT_QUEUE, //12

                //number of operations in next queue
                JobShopAttribute.LEAST_NUM_OPERATIOM_IN_NEXT_QUEUE, //13
                JobShopAttribute.MAX_NUM_OPERATIOM_IN_NEXT_QUEUE, //14
                JobShopAttribute.AVE_NUM_OPERATIOM_IN_NEXT_QUEUE, //15

                JobShopAttribute.DEVIATION_OF_JOB_IN_QUEUE, //16
        };
    }

    /**
     * The attributes for minimising mean weighted tardiness (Su's paper).
     * @return the attributes.
     */
    public static JobShopAttribute[] mwtAttributes() {
        return new JobShopAttribute[]{
                JobShopAttribute.TIME_IN_SYSTEM,
                JobShopAttribute.OP_WAITING_TIME,
                JobShopAttribute.NUM_OPS_REMAINING,
                JobShopAttribute.WORK_REMAINING,
                JobShopAttribute.PROC_TIME,
                JobShopAttribute.DUE_DATE,
                JobShopAttribute.SLACK,
                JobShopAttribute.WEIGHT,
                JobShopAttribute.NEXT_PROC_TIME,
                //JobShopAttribute.WORK_IN_NEXT_QUEUE
        };
    }

    public static JobShopAttribute[] countAttributes() {
        return new JobShopAttribute[] {
                JobShopAttribute.NUM_OPS_IN_QUEUE,
                JobShopAttribute.NUM_OPS_REMAINING,
                //JobShopAttribute.NUM_OPS_IN_NEXT_QUEUE
        };
    }

    public static JobShopAttribute[] weightAttributes() {
        return new JobShopAttribute[] {
                JobShopAttribute.WEIGHT
        };
    }

    public static JobShopAttribute[] timeAttributes() {
        return new JobShopAttribute[] {
                JobShopAttribute.MACHINE_WAITING_TIME,
                JobShopAttribute.OP_WAITING_TIME,
                //JobShopAttribute.NEXT_READY_TIME,
                //JobShopAttribute.FLOW_DUE_DATE,
                JobShopAttribute.DUE_DATE,

                JobShopAttribute.WORK_IN_QUEUE,
                JobShopAttribute.PROC_TIME,
                JobShopAttribute.NEXT_PROC_TIME,
                JobShopAttribute.WORK_REMAINING,
                //JobShopAttribute.WORK_IN_NEXT_QUEUE,

                JobShopAttribute.TIME_IN_SYSTEM,
                JobShopAttribute.SLACK
        };
    }
}
