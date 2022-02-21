"""
Jameson Albers
CS 6500, Spring 2022
Lab 4

This file imports the CSV files created from the response test program and
calculates the mean, median, and 99th percentile latencies for the responses.
Additionally, it plots the latencies over time.
"""
from concurrent.futures import thread
import csv
import os
import importlib
from shutil import which
from unittest import result
try:
    import matplotlib.pyplot as plt
    import numpy as np
except ImportError:
    import subprocess
    import sys
    subprocess.call([sys.executable,'-m','pip','install','matplotlib'])
finally:
    plt = importlib.import_module('matplotlib.pyplot')
    np = importlib.import_module('numpy')
import statistics as stat

def ns_to_ms(measurement: int) -> float:
    '''Converts a nanoseconds time measurement to seconds.'''
    return measurement / 1000000

def ns_to_s(measurement):
    """Converts a nanoseconds time measurement to seconds."""
    return measurement / 1000000000

def get_metrics(filepath:str) -> list:
    '''Returns a list of metrics
    - Format: [
        eventual success %,
        initial failure %,
        throughput,
        mean latency,
        median latency,
        99th percentile latency
        ]'''
    # List to store latencies - used form mean, median, 99th percentile
    latencies = []
    with open(filepath) as file:
        lines = file.readlines()
    test_time = ns_to_s(int(lines[0]))
    total_requests = int(lines[1])
    successful_requests = int(lines[2])
    failures = int(lines[3])
    latencies = [ns_to_ms(int(x)) for x in lines[3:]]
    success_percentage = total_requests / successful_requests * 100
    initial_failure_percentage = (
        failures / (successful_requests + failures) * 100
    )
    throughput = successful_requests / test_time
    mean_latency = stat.mean(latencies)
    median_latency = stat.median(latencies)
    p99_latency = stat.quantiles(latencies,n=100)[98]
    return [success_percentage, initial_failure_percentage, throughput,
        mean_latency, median_latency, p99_latency]

def get_baseline(filepath: str) -> float:
    with open(filepath) as file:
        baseline_string = file.readline().strip()
    return float(baseline_string)

def sort_by_threads(item:str) -> int:
    """Key functinon for sorting filenames by number of threads."""
    return int(item.split(' ')[0])

def main():
    # Get filenames of result files
    result_filenames = []
    for file in os.listdir():
        if file.endswith('Threads.txt'):
            result_filenames.append(file)
    result_filenames.sort(key=sort_by_threads)

    # Get list of thread counts
    thread_counts = []
    for filename in result_filenames:
        thread_counts.append(int(filename.split(" ")[0]))
    thread_counts.sort()

    # Make threadcounts numpy array
    thread_counts_np = np.array(thread_counts)

    # Get baseline throughput
    baseline = get_baseline('baseline.txt')
    #print(baseline)

    # Calculate metrics for each file
    metrics_list = [get_metrics(x) for x in result_filenames]
    
    # Get statistics arrays
    mean_list = [x[3] for x in metrics_list]
    median_list = [x[4] for x in metrics_list]
    p99_list = [x[5] for x in metrics_list]

    mean_np = np.array(mean_list)
    median_np = np.array(median_list)
    p99_np= np.array(p99_list)

    throughputs = [x[2] for x in metrics_list]

    for i in range(0, len(thread_counts)):
        throughputs[i] /= thread_counts[i]
    
    throughputs_np = np.array(throughputs)

    failure_p_np = np.array([x[1] * 100 for x in metrics_list])

    plt.rcParams["figure.figsize"] = (10,4.5)
    plt.plot(thread_counts_np,mean_np, label='Mean')
    plt.plot(thread_counts_np,median_np,label='Median')
    plt.plot(thread_counts_np, p99_np,label='99th Percentile')
    plt.legend()
    plt.xlabel('# of Threads')
    plt.ylabel('Latency (ms)')
    plt.title('Multithreaded System Latency')
    plt.grid(which='both', axis='y')
    plt.savefig('stats_figure.png')

    plt.close()
    plt.bar(thread_counts_np, failure_p_np, width=10, color='red')
    plt.xlabel('# of Threads')
    plt.ylabel('Initial Request Failure %')
    plt.title('Multithreaded Request Initial Failure Rate')
    plt.grid(which='both', axis='y')
    plt.savefig('failure_figure.png')

    plt.close()
    plt.plot(thread_counts_np, throughputs_np)
    plt.xlabel('# of Threads')
    plt.ylabel('Throughput (Requests per second)')
    plt.title('Throughput vs Baseline')
    plt.grid(which='both', axis='y')
    plt.savefig('througput_fig.png')

if __name__ == '__main__':
    abspath = os.path.abspath(__file__)
    dname = os.path.dirname(abspath)
    os.chdir(dname)
    main()