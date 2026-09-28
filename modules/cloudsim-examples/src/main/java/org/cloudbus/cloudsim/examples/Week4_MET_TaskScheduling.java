package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * Week 4 - Minimum Execution Time (MET) task scheduling.
 *
 * Scenario:
 * 1 Datacenter
 * 10 heterogeneous Hosts
 * 20 heterogeneous VMs
 * 1000 Cloudlets from GoCJ
 *
 * MET assigns each Cloudlet to the VM with the smallest nominal
 * execution time: CloudletLength / VM_MIPS.
 *
 * The assignment is bound through DatacenterBroker before simulation.
 */
public class Week4_MET_TaskScheduling {

    private static final int NUM_USERS = 1;
    private static final int NUM_HOSTS = 10;
    private static final int NUM_VMS = 20;
    private static final int MAX_CLOUDLETS = 1000;

    private static final String DEFAULT_DATASET =
            "dataset/GoCJ_Dataset_1000.txt";

    /*
     * Used only when the real dataset file has not been placed yet.
     * Set to false for the final submission run.
     */
    private static final boolean FALLBACK_TO_SYNTHETIC = false;

    private static final int SYNTHETIC_COUNT = 1000;

    private static final DecimalFormat DF = new DecimalFormat("0.####");

    public static void main(String[] args) {
        Log.println("Starting Week4_MET_TaskScheduling...");

        try {
            CloudSim.init(NUM_USERS, Calendar.getInstance(), false);

            Datacenter datacenter = createDatacenter("Datacenter_0");
            DatacenterBroker broker = new DatacenterBroker("MET_Broker");
            int brokerId = broker.getId();

            List<Vm> vmList = createVmList(brokerId);

            String datasetPath = args.length > 0 ? args[0] : DEFAULT_DATASET;
            List<Long> jobLengths;

            try {
                jobLengths = loadGoCJ(Paths.get(datasetPath));
                Log.println("Dataset loaded: " + datasetPath);
            } catch (Exception e) {
                if (!FALLBACK_TO_SYNTHETIC) {
                    throw e;
                }

                Log.println("WARNING: GoCJ dataset could not be loaded.");
                Log.println("Using deterministic synthetic workload for testing only.");
                jobLengths = generateSyntheticWorkload(SYNTHETIC_COUNT);
            }

            if (jobLengths.size() > MAX_CLOUDLETS) {
                jobLengths = new ArrayList<>(jobLengths.subList(0, MAX_CLOUDLETS));
            }

            List<Cloudlet> cloudletList =
                    createCloudletList(brokerId, jobLengths);

            broker.submitGuestList(vmList);

            Map<Integer, Integer> assignment =
                    applyMET(cloudletList, vmList);

            broker.submitCloudletList(cloudletList);

            printConfiguration(jobLengths, vmList, assignment);

            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            List<Cloudlet> results = broker.getCloudletReceivedList();

            printCloudletResults(results);
            printMetrics(results, vmList);

            Log.println("Week4_MET_TaskScheduling finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.println("Simulation failed.");
        }
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();

        for (int hostId = 0; hostId < NUM_HOSTS; hostId++) {
            boolean classA = hostId < 5;

            int hostPes = classA ? 4 : 8;
            int hostMips = classA ? 2000 : 3000;
            int ram = classA ? 8192 : 16384;
            long storage = classA ? 1_000_000L : 2_000_000L;
            int bw = 1_000_000;

            List<Pe> peList = new ArrayList<>();

            for (int peId = 0; peId < hostPes; peId++) {
                peList.add(
                    new Pe(
                        peId,
                        new PeProvisionerSimple(hostMips)
                    )
                );
            }

            hostList.add(
                new Host(
                    hostId,
                    new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw),
                    storage,
                    peList,
                    new VmSchedulerTimeShared(peList)
                )
            );
        }

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double timeZone = 7.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        List<Storage> storageList = new ArrayList<>();

        DatacenterCharacteristics characteristics =
                new DatacenterCharacteristics(
                    arch,
                    os,
                    vmm,
                    hostList,
                    timeZone,
                    cost,
                    costPerMem,
                    costPerStorage,
                    costPerBw
                );

        try {
            return new Datacenter(
                name,
                characteristics,
                new VmAllocationPolicySimple(hostList),
                storageList,
                0
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create datacenter.", e);
        }
    }

    private static List<Vm> createVmList(int brokerId) {
        List<Vm> list = new ArrayList<>();

        /*
         * Five VMs of each type.
         *
         * Type 1: 1 PE, 1000 MIPS, 2 GB RAM, 20 GB storage
         * Type 2: 2 PE, 1500 MIPS, 2 GB RAM, 20 GB storage
         * Type 3: 2 PE, 2000 MIPS, 4 GB RAM, 30 GB storage
         * Type 4: 4 PE, 2500 MIPS, 4 GB RAM, 40 GB storage
         */
        int[] pes =    {1, 2, 2, 4};
        int[] mips =   {1000, 1500, 2000, 2500};
        int[] ram =    {2048, 2048, 4096, 4096};
        long[] size =  {20_000L, 20_000L, 30_000L, 40_000L};

        long bw = 10_000L;
        String vmm = "Xen";

        int vmId = 0;

        for (int type = 0; type < 4; type++) {
            for (int copy = 0; copy < 5; copy++) {
                Vm vm = new Vm(
                    vmId,
                    brokerId,
                    mips[type],
                    pes[type],
                    ram[type],
                    bw,
                    size[type],
                    vmm,
                    new CloudletSchedulerSpaceShared()
                );

                list.add(vm);
                vmId++;
            }
        }

        return list;
    }

    private static List<Cloudlet> createCloudletList(
            int brokerId,
            List<Long> jobLengths) {

        List<Cloudlet> list = new ArrayList<>();

        int pesNumber = 1;
        long fileSize = 300;
        long outputSize = 300;

        UtilizationModel utilizationModel = new UtilizationModelFull();

        for (int i = 0; i < jobLengths.size(); i++) {
            Cloudlet cloudlet = new Cloudlet(
                i,
                jobLengths.get(i),
                pesNumber,
                fileSize,
                outputSize,
                utilizationModel,
                utilizationModel,
                utilizationModel
            );

            cloudlet.setUserId(brokerId);
            list.add(cloudlet);
        }

        return list;
    }

    private static Map<Integer, Integer> applyMET(
            List<Cloudlet> cloudlets,
            List<Vm> vmList) {

        Map<Integer, Integer> assignment = new LinkedHashMap<>();

        for (Cloudlet cloudlet : cloudlets) {
            Vm bestVm = null;
            double minExecutionTime = Double.POSITIVE_INFINITY;

            for (Vm vm : vmList) {
                double executionTime =
                        (double) cloudlet.getCloudletLength()
                        / vm.getMips();

                if (executionTime < minExecutionTime) {
                    minExecutionTime = executionTime;
                    bestVm = vm;
                }
            }

            if (bestVm == null) {
                throw new IllegalStateException(
                    "No VM is available for Cloudlet "
                    + cloudlet.getCloudletId()
                );
            }

            // CloudSimExample1 uses GuestId to bind a Cloudlet to a VM
            // before submitting the Cloudlet list to the Broker.
            cloudlet.setGuestId(bestVm.getId());

            assignment.put(
                cloudlet.getCloudletId(),
                bestVm.getId()
            );
        }

        return assignment;
    }

    private static List<Long> loadGoCJ(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Dataset file not found: " + path);
        }

        List<Long> lengths = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] tokens = line.split("[,;\\t ]+");

                try {
                    long value = Long.parseLong(
                        tokens[0].replace("\"", "").trim()
                    );

                    if (value > 0) {
                        lengths.add(value);
                    }
                } catch (NumberFormatException ignored) {
                    // Skip header or non-numeric rows.
                }
            }
        }

        if (lengths.isEmpty()) {
            throw new IOException(
                "No numeric MI values were found in " + path
            );
        }

        return lengths;
    }

    private static List<Long> generateSyntheticWorkload(int count) {
        long[] values = {
            1000, 2500, 5000, 10000, 15000,
            25000, 50000, 75000, 100000
        };

        List<Long> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            result.add(values[i % values.length]);
        }

        return result;
    }

    private static void printConfiguration(
            List<Long> jobLengths,
            List<Vm> vmList,
            Map<Integer, Integer> assignment) {

        Map<Integer, Integer> countPerVm = new LinkedHashMap<>();

        for (Vm vm : vmList) {
            countPerVm.put(vm.getId(), 0);
        }

        for (int vmId : assignment.values()) {
            countPerVm.put(vmId, countPerVm.get(vmId) + 1);
        }

        Log.println();
        Log.println("========== CONFIGURATION ==========");
        Log.println("Datacenters : 1");
        Log.println("Hosts       : 10 (5 Class A + 5 Class B)");
        Log.println("VMs         : " + vmList.size());
        Log.println("Cloudlets   : " + jobLengths.size());
        Log.println("Algorithm   : Minimum Execution Time (MET)");
        Log.println();
        Log.println("VM assignment distribution:");

        for (Map.Entry<Integer, Integer> entry : countPerVm.entrySet()) {
            Log.println(
                "VM " + entry.getKey()
                + " -> " + entry.getValue() + " cloudlets"
            );
        }

        Log.println();
        Log.println("First 20 MET assignments:");

        int printed = 0;
        for (Map.Entry<Integer, Integer> entry : assignment.entrySet()) {
            Log.println(
                "Cloudlet " + entry.getKey()
                + " -> VM " + entry.getValue()
            );

            printed++;
            if (printed >= 20) {
                break;
            }
        }
    }

    private static void printCloudletResults(List<Cloudlet> list) {
        Log.println();
        Log.println("========== CLOUDLET RESULTS ==========");

        Log.println(
            "Cloudlet ID\tStatus\tVM ID\tExec Time\tStart Time\tFinish Time"
        );

        for (Cloudlet cloudlet : list) {
            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                Log.println(
                    cloudlet.getCloudletId()
                    + "\tSUCCESS\t"
                    + cloudlet.getGuestId()
                    + "\t"
                    + DF.format(cloudlet.getActualCPUTime())
                    + "\t"
                    + DF.format(cloudlet.getExecStartTime())
                    + "\t"
                    + DF.format(cloudlet.getExecFinishTime())
                );
            }
        }
    }

    private static void printMetrics(
            List<Cloudlet> results,
            List<Vm> vmList) {

        if (results.isEmpty()) {
            throw new IllegalStateException(
                "No Cloudlet results were returned."
            );
        }

        double makespan = 0.0;
        double totalWaitingTime = 0.0;

        Map<Integer, Double> loadPerVm = new HashMap<>();

        for (Vm vm : vmList) {
            loadPerVm.put(vm.getId(), 0.0);
        }

        for (Cloudlet cloudlet : results) {
            if (cloudlet.getStatus() != Cloudlet.CloudletStatus.SUCCESS) {
                continue;
            }

            double start = cloudlet.getExecStartTime();
            double finish = cloudlet.getExecFinishTime();
            double actualCpu = cloudlet.getActualCPUTime();

            makespan = Math.max(makespan, finish);
            totalWaitingTime += Math.max(0.0, start);

            int vmId = cloudlet.getGuestId();
            loadPerVm.put(
                vmId,
                loadPerVm.get(vmId) + actualCpu
            );
        }

        double averageLoad =
                loadPerVm.values()
                    .stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

        double maxLoad =
                loadPerVm.values()
                    .stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0.0);

        double minLoad =
                loadPerVm.values()
                    .stream()
                    .mapToDouble(Double::doubleValue)
                    .min()
                    .orElse(0.0);

        double degreeOfImbalance =
                averageLoad == 0.0
                ? 0.0
                : (maxLoad - minLoad) / averageLoad;

        double totalActualCpuTime =
                results.stream()
                    .filter(c -> c.getStatus() == Cloudlet.CloudletStatus.SUCCESS)
                    .mapToDouble(Cloudlet::getActualCPUTime)
                    .sum();

        double averageWaitingTime =
                totalWaitingTime / results.size();

        double throughput =
                makespan == 0.0
                ? 0.0
                : results.size() / makespan;

        double utilization =
                makespan == 0.0
                ? 0.0
                : (totalActualCpuTime
                    / (makespan * vmList.size())) * 100.0;

        Log.println();
        Log.println("========== METRICS ==========");
        Log.println("Completed Cloudlets      : " + results.size());
        Log.println("Makespan                 : " + DF.format(makespan) + " s");
        Log.println("Degree of Imbalance      : " + DF.format(degreeOfImbalance));
        Log.println("Average Waiting Time     : " + DF.format(averageWaitingTime) + " s");
        Log.println("Throughput               : " + DF.format(throughput) + " cloudlets/s");
        Log.println("Average VM Utilization*  : " + DF.format(utilization) + " %");

        Log.println();
        Log.println("* Utilization is calculated from aggregate Cloudlet CPU time");
        Log.println("  over the available VM time in this simplified model.");
    }
}
