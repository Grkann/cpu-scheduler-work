package com.ozdmrgurkan;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

//  Temel Süreç (Process) Sınıfı
class Process implements Cloneable {
    String id;
    int arrivalTime;
    int burstTime;
    String priorityStr;
    int priorityLevel;

    int remainingTime;
    int startTime = -1;
    int completionTime = 0;
    int waitingTime = 0;
    int turnaroundTime = 0;

    public Process(String id, int arrivalTime, int burstTime, String priorityStr) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priorityStr = priorityStr;
        this.remainingTime = burstTime;

        switch (priorityStr.toLowerCase()) {
            case "high": this.priorityLevel = 1; break;
            case "normal": this.priorityLevel = 2; break;
            case "low": this.priorityLevel = 3; break;
            default: this.priorityLevel = 2;
        }
    }

    @Override
    public Process clone() {
        try {
            return (Process) super.clone();
        } catch (CloneNotSupportedException e) {
            return new Process(this.id, this.arrivalTime, this.burstTime, this.priorityStr);
        }
    }
}

//  Zamanlayıcı Arayüzü
abstract class Scheduler implements Runnable {
    protected List<Process> processes;
    protected String algorithmName;
    protected String scenarioName; // EKLENDİ: Hangi dosya (Case1/Case2) olduğunu tutar.
    protected StringBuilder ganttChart = new StringBuilder();
    protected int contextSwitchCount = 0;
    protected final double CONTEXT_SWITCH_TIME = 0.001;

    // Constructorı güncellendik: scenarioName parametresi eklendi.
    public Scheduler(List<Process> processes, String name, String scenarioName) {
        this.processes = processes.stream().map(Process::clone).collect(Collectors.toList());
        this.algorithmName = name;
        this.scenarioName = scenarioName;
    }

    protected abstract void execute();

    @Override
    public void run() {
        execute();
        writeOutput();
        System.out.println(scenarioName + " - " + algorithmName + " tamamlandı.");
    }

    protected void writeOutput() {
        // Dosya ismine scenarioName ekledik.: "FCFS_Case1.txt" gibi
        String outputFilename = algorithmName + "_" + scenarioName + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename))) {
            writer.write("Senaryo: " + scenarioName + "\n");
            writer.write("Algoritma: " + algorithmName + "\n");
            writer.write("--------------------------------------------------\n");

            writer.write("a) Zaman Tablosu:\n");
            writer.write(ganttChart.toString() + "\n\n");

            double totalWait = 0, totalTurnaround = 0;
            int maxWait = Integer.MIN_VALUE, maxTurnaround = Integer.MIN_VALUE;
            int totalBurst = 0;
            int[] throughputCheckpoints = {50, 100, 150, 200};
            int[] throughputCounts = new int[throughputCheckpoints.length];

            for (Process p : processes) {
                p.turnaroundTime = p.completionTime - p.arrivalTime;
                p.waitingTime = p.turnaroundTime - p.burstTime;

                totalWait += p.waitingTime;
                totalTurnaround += p.turnaroundTime;
                totalBurst += p.burstTime;

                if (p.waitingTime > maxWait) maxWait = p.waitingTime;
                if (p.turnaroundTime > maxTurnaround) maxTurnaround = p.turnaroundTime;

                for (int i = 0; i < throughputCheckpoints.length; i++) {
                    if (p.completionTime <= throughputCheckpoints[i]) {
                        throughputCounts[i]++;
                    }
                }
            }

            writer.write(String.format("b) Bekleme Süresi -> Maks: %d, Ort: %.2f\n", maxWait, totalWait / processes.size()));
            writer.write(String.format("c) Tamamlanma Süresi -> Maks: %d, Ort: %.2f\n", maxTurnaround, totalTurnaround / processes.size()));

            writer.write("d) Throughput:\n");
            for (int i = 0; i < throughputCheckpoints.length; i++) {
                writer.write(String.format("   T=%d için: %d işlem\n", throughputCheckpoints[i], throughputCounts[i]));
            }

            double efficiency = (double) totalBurst / (totalBurst + (contextSwitchCount * CONTEXT_SWITCH_TIME));
            writer.write(String.format("e) Ortalama CPU Verimliliği: %.6f\n", efficiency));
            writer.write(String.format("f) Toplam Bağlam Değiştirme: %d\n", contextSwitchCount));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void addToGantt(int startTime, String pId, int endTime) {
        ganttChart.append("[").append(startTime).append("]--").append(pId).append("--[").append(endTime).append("]\n");
    }
}

// Algoritmalar (Constructor'ları güncellendi)

class FCFS extends Scheduler {
    public FCFS(List<Process> processes, String scenario) { super(processes, "FCFS", scenario); }
    @Override
    protected void execute() {
        processes.sort(Comparator.comparingInt(p -> p.arrivalTime));
        int currentTime = 0;
        for (Process p : processes) {
            if (currentTime < p.arrivalTime) {
                addToGantt(currentTime, "IDLE", p.arrivalTime);
                currentTime = p.arrivalTime;
            }
            contextSwitchCount++;
            int start = currentTime;
            currentTime += p.burstTime;
            p.completionTime = currentTime;
            addToGantt(start, p.id, currentTime);
        }
    }
}

class SJF_Preemptive extends Scheduler {
    public SJF_Preemptive(List<Process> processes, String scenario) { super(processes, "SJF_Preemptive", scenario); }
    @Override
    protected void execute() {
        int currentTime = 0;
        int completed = 0;
        int n = processes.size();
        Process currentProcess = null;

        while (completed < n) {
            List<Process> ready = new ArrayList<>();
            for (Process p : processes) {
                if (p.arrivalTime <= currentTime && p.remainingTime > 0) ready.add(p);
            }
            if (ready.isEmpty()) { currentTime++; continue; }

            ready.sort(Comparator.comparingInt(p -> p.remainingTime));
            Process shortest = ready.get(0);

            if (currentProcess != shortest) {
                contextSwitchCount++;
                currentProcess = shortest;
            }
            if (shortest.startTime == -1) shortest.startTime = currentTime;

            shortest.remainingTime--;
            currentTime++;

            if (shortest.remainingTime == 0) {
                shortest.completionTime = currentTime;
                completed++;
                addToGantt(currentTime - shortest.burstTime, shortest.id + "(Done)", currentTime);
            }
        }
    }
}

class SJF_NonPreemptive extends Scheduler {
    public SJF_NonPreemptive(List<Process> processes, String scenario) { super(processes, "SJF_NonPreemptive", scenario); }
    @Override
    protected void execute() {
        int currentTime = 0;
        int completed = 0;
        List<Process> pool = new ArrayList<>(processes);

        while (completed < processes.size()) {
            List<Process> ready = new ArrayList<>();
            for (Process p : pool) {
                if (p.arrivalTime <= currentTime && p.remainingTime > 0) ready.add(p);
            }
            if (ready.isEmpty()) {
                int minArrival = Integer.MAX_VALUE;
                for(Process p : pool) if(p.remainingTime > 0) minArrival = Math.min(minArrival, p.arrivalTime);
                addToGantt(currentTime, "IDLE", minArrival);
                currentTime = minArrival;
                continue;
            }
            ready.sort(Comparator.comparingInt(p -> p.burstTime));
            Process p = ready.get(0);

            contextSwitchCount++;
            int start = currentTime;
            currentTime += p.burstTime;
            p.remainingTime = 0;
            p.completionTime = currentTime;
            completed++;
            addToGantt(start, p.id, currentTime);
        }
    }
}

class RoundRobin extends Scheduler {
    private int quantum = 10;
    public RoundRobin(List<Process> processes, String scenario) { super(processes, "RoundRobin", scenario); }
    @Override
    protected void execute() {
        processes.sort(Comparator.comparingInt(p -> p.arrivalTime));
        Queue<Process> queue = new LinkedList<>();
        int currentTime = 0;
        int completed = 0;
        int i = 0;

        if (!processes.isEmpty()) {
            currentTime = processes.get(0).arrivalTime;
            while(i < processes.size() && processes.get(i).arrivalTime <= currentTime) {
                queue.add(processes.get(i)); i++;
            }
        }

        while (completed < processes.size()) {
            if (queue.isEmpty()) {
                if (i < processes.size()) {
                    int nextArrival = processes.get(i).arrivalTime;
                    addToGantt(currentTime, "IDLE", nextArrival);
                    currentTime = nextArrival;
                    while(i < processes.size() && processes.get(i).arrivalTime <= currentTime) {
                        queue.add(processes.get(i)); i++;
                    }
                } else { break; }
            }

            Process p = queue.poll();
            contextSwitchCount++;
            int start = currentTime;
            int execTime = Math.min(quantum, p.remainingTime);
            p.remainingTime -= execTime;
            currentTime += execTime;
            addToGantt(start, p.id, currentTime);

            while (i < processes.size() && processes.get(i).arrivalTime <= currentTime) {
                queue.add(processes.get(i)); i++;
            }
            if (p.remainingTime > 0) queue.add(p);
            else { p.completionTime = currentTime; completed++; }
        }
    }
}

class Priority_Preemptive extends Scheduler {
    public Priority_Preemptive(List<Process> processes, String scenario) { super(processes, "Priority_Preemptive", scenario); }
    @Override
    protected void execute() {
        int currentTime = 0;
        int completed = 0;
        Process lastProcess = null;
        int startBlock = 0;

        while (completed < processes.size()) {
            List<Process> ready = new ArrayList<>();
            for (Process p : processes) {
                if (p.arrivalTime <= currentTime && p.remainingTime > 0) ready.add(p);
            }
            if (ready.isEmpty()) { currentTime++; continue; }

            ready.sort(Comparator.comparingInt(p -> p.priorityLevel));
            Process current = ready.get(0);

            if (lastProcess != current) {
                if (lastProcess != null) {
                    addToGantt(startBlock, lastProcess.id, currentTime);
                    contextSwitchCount++;
                }
                startBlock = currentTime;
                lastProcess = current;
            }
            current.remainingTime--;
            currentTime++;

            if (current.remainingTime == 0) {
                current.completionTime = currentTime;
                completed++;
                addToGantt(startBlock, current.id, currentTime);
                lastProcess = null;
                startBlock = currentTime;
                contextSwitchCount++;
            }
        }
    }
}

class Priority_NonPreemptive extends Scheduler {
    public Priority_NonPreemptive(List<Process> processes, String scenario) { super(processes, "Priority_NonPreemptive", scenario); }
    @Override
    protected void execute() {
        int currentTime = 0;
        int completed = 0;
        List<Process> pool = new ArrayList<>(processes);
        pool.sort(Comparator.comparingInt(p -> p.arrivalTime));

        while (completed < processes.size()) {
            List<Process> ready = new ArrayList<>();
            for (Process p : pool) {
                if (p.arrivalTime <= currentTime && p.remainingTime > 0) ready.add(p);
            }
            if (ready.isEmpty()) { currentTime++; continue; }

            ready.sort(Comparator.comparingInt(p -> p.priorityLevel));
            Process p = ready.get(0);

            contextSwitchCount++;
            int start = currentTime;
            currentTime += p.burstTime;
            p.remainingTime = 0;
            p.completionTime = currentTime;
            completed++;
            addToGantt(start, p.id, currentTime);
        }
    }
}

// --- Main ---
public class Odev1Main {
    public static void main(String[] args) {
        // İki senaryoyu sırayla çalıştır
        System.out.println(">>> CASE 1 BAŞLIYOR <<<");
        runScenario("odev1_case1.txt", "Case1");

        System.out.println("\n>>> CASE 2 BAŞLIYOR <<<");
        runScenario("odev1_case2.txt", "Case2"); // İkinci dosyanın adının bu olduğundan emin olun!
    }

    // Tek bir dosya için tüm durumu çalıştıran metot.
    public static void runScenario(String filename, String scenarioName) {
        List<Process> allProcesses = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            br.readLine(); // Header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    allProcesses.add(new Process(
                            parts[0].trim(),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim()),
                            parts[3].trim()
                    ));
                }
            }
        } catch (IOException e) {
            System.err.println("HATA: " + filename + " dosyası bulunamadı! Lütfen proje kök dizininde olduğundan emin olun.");
            return;
        }

        System.out.println(filename + " okundu. İşlem sayısı: " + allProcesses.size());

        // Threadleri oluşturduk. (İsimlerine scenarioName ekledik.)
        Thread t1 = new Thread(new FCFS(allProcesses, scenarioName));
        Thread t2 = new Thread(new SJF_Preemptive(allProcesses, scenarioName));
        Thread t3 = new Thread(new SJF_NonPreemptive(allProcesses, scenarioName));
        Thread t4 = new Thread(new RoundRobin(allProcesses, scenarioName));
        Thread t5 = new Thread(new Priority_Preemptive(allProcesses, scenarioName));
        Thread t6 = new Thread(new Priority_NonPreemptive(allProcesses, scenarioName));

        t1.start(); t2.start(); t3.start(); t4.start(); t5.start(); t6.start();

        try {
            t1.join(); t2.join(); t3.join(); t4.join(); t5.join(); t6.join();
            System.out.println(scenarioName + " için tüm algoritmalar tamamlandı.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}