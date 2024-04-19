import json
import pprint
import os
import matplotlib.pyplot as plt
import numpy as np
import sys
import statistics


def get_num_of_switches_TS(trajectory_data, graph_file_path, create_graphs):
    switch_threshold = 5000
    number_of_switches = 0
    pA_idx = 4
    pB_idx = 5
    dominant_species_idx = -1
    graph_data = []
    peak_heights = []
    current_peak_time = 0
    current_peak = 0
    graph_data_extremes = []
    for trajectory_point in trajectory_data:
        time = trajectory_point["right"]
        pA_num = trajectory_point["left"][pA_idx]
        pB_num = trajectory_point["left"][pB_idx]
        graph_data.append([time, pA_num])
        if pA_num > switch_threshold and pA_idx != dominant_species_idx:
            if dominant_species_idx != -1:
                number_of_switches += 1
                dominant_species_idx = pA_idx
                peak_heights.append(current_peak)
                graph_data_extremes.append([current_peak_time, current_peak])
                current_peak = 0
            else:
                dominant_species_idx = pA_idx
        if pB_num > switch_threshold and pB_idx != dominant_species_idx:
            if dominant_species_idx != -1:
                number_of_switches += 1
                dominant_species_idx = pB_idx
                peak_heights.append(current_peak)
                graph_data_extremes.append([current_peak_time, current_peak])
                current_peak = 0
            else:
                dominant_species_idx = pB_idx
        if pA_idx == dominant_species_idx:
            if pA_num > current_peak:
                current_peak_time = time
                current_peak = pA_num
        if pB_idx == dominant_species_idx:
            if pB_num > current_peak:
                current_peak = pB_num
                current_peak_time = time
    if dominant_species_idx != -1:
        peak_heights.append(current_peak)
        graph_data_extremes.append([time, current_peak])

    points = np.array(graph_data)
    if create_graphs:
        print(graph_file_path)
        plt.clf()
        for extreme in graph_data_extremes:
            plt.plot(np.array([extreme[0], extreme[0]]), np.array([0, 15000]), 'o:r')
        plt.plot(points[:, 0], points[:, 1])
        plt.savefig(graph_file_path)

    return number_of_switches, peak_heights


def who_dies_PP(trajectory_data):
    pred = trajectory_data[-1]["left"][0]
    prey = trajectory_data[-1]["left"][1]
    if pred < 1 and prey < 1:
        return "prey"
    elif pred < 1:
        return "pred"
    elif prey < 1:
        return "prey"
    else:
        return None

def dominance(trajectory_data):
    last_state = trajectory_data[-1]["left"]
    max_val = max(last_state)
    return last_state.index(max_val)


def is_viral_early_death_and_DNA_RNA_P_V(trajectory_data):
    s1 = trajectory_data[-1]["left"][0]
    s2 = trajectory_data[-1]["left"][1]
    s3 = trajectory_data[-1]["left"][2]
    s4 = trajectory_data[-1]["left"][3]

    for trajectory_point in reversed(trajectory_data):
        time = trajectory_point["right"]
        if time <= 200.0:
            return s1 < 1 and s2 < 1 and s3 < 1 and s4 < 1, trajectory_point["left"][0], trajectory_point["left"][1], trajectory_point["left"][2], trajectory_point["left"][3]

    assert False # Shall not happen with the correct data set


def zero_Ribosome_peak_lacZ_EC(trajectory_data):
    ribosome_death_time = 0
    ribosome_dead = False
    lacZ_peak = 0
    lacZ_peak_time = 0
    product_count_at_2000 = trajectory_data[-1]["left"][22]
    lactose_count_at_2000 = trajectory_data[-1]["left"][20]
    for trajectory_point in trajectory_data:
        time = trajectory_point["right"]
        ribosome = trajectory_point["left"][9]
        lacZ = trajectory_point["left"][14]
        if not ribosome_dead and ribosome < 1:
            ribosome_dead = True
            ribosome_death_time = time
        if lacZ > lacZ_peak:
            lacZ_peak = lacZ
            lacZ_peak_time = time

    return ribosome_death_time, lacZ_peak_time, lacZ_peak, product_count_at_2000, lactose_count_at_2000


def get_peaks(trajectory_data, threshold):
    # split by points where value of the given species is 0
    size = len(trajectory_data)
    idx_list = [idx + 1 for idx, val in
                enumerate(trajectory_data) if val[1] < threshold]

    trajectory_data_splitted = [trajectory_data[i: j] for i, j in
           zip([0] + idx_list, idx_list +
               ([size] if idx_list[-1] != size else []))]

    #pprint.pprint(trajectory_data_splitted)
    # filter the borders
    if not trajectory_data_splitted:
        return []

    if len(trajectory_data_splitted[0]) != 1 or trajectory_data_splitted[0][0][1] == 0:
        trajectory_data_splitted.pop(0)
        if not trajectory_data_splitted:
            return []

    if len(trajectory_data_splitted[-1]) != 1 or trajectory_data_splitted[-1][0][1] == 0:
        trajectory_data_splitted.pop()

    # filter zeros and runts
    significant_trajectory_parts = \
        filter(lambda part: max(list(zip(*part))[1]) > threshold, trajectory_data_splitted)

    peaks = []
    for trajectory_part in significant_trajectory_parts:
        max_y = -1
        assert trajectory_part
        peak_point = []
        for point in trajectory_part:
            if point[1] > max_y:
                max_y = point[1]
                peak_point = point
        peaks.append(peak_point)

    return peaks


def analyse_freq_RP(trajectory_data, graph_file_path, create_graphs):
    zoom = []
    graph_data_pA = []
    graph_data_sA = []
    for trajectory_point in trajectory_data:
        x_coord = trajectory_point["right"]
        y_coord_pA = trajectory_point["left"][3] #pA
        y_coord_sA = trajectory_point["left"][0] #sA
        graph_data_pA.append([x_coord, y_coord_pA])
        graph_data_sA.append([x_coord, y_coord_sA])

    # recursive search
    graph_data_extremes_pA = get_peaks(graph_data_pA, 50)
    graph_data_extremes_sA = get_peaks(graph_data_sA, 3)
##    if zoom:
##        graph_data_extremes = list(filter(lambda c: zoom[0] < c[0] < zoom[1], graph_data_extremes))
##        graph_data = list(filter(lambda c: zoom[0] < c[0] < zoom[1], graph_data))
##    max_y = max(list(zip(*graph_data))[1])
##    points = np.array(graph_data)
##    if create_graphs:
##        print(graph_file_path)
##        plt.clf()
##        for extreme in graph_data_extremes:
##            plt.plot(np.array([extreme[0], extreme[0]]), np.array([0, max_y]), 'o:r')
##        plt.plot(points[:, 0], points[:, 1])
##        plt.savefig(graph_file_path)
    # plt.show()
    return graph_data_extremes_pA, graph_data_extremes_sA


def same_sign(x, y):
    return (y >= 0) if (x >= 0) else (y < 0)


def find_real_extremes(local_extremes, starting_index, search_interval):
    return left_search(local_extremes, starting_index, 3.2, True) \
            + [local_extremes[starting_index]] + \
            right_search(local_extremes, starting_index, 3.2, True)


def left_search(local_extremes, starting_index, search_interval, search_min):
    found_extreme = local_extremes[starting_index]
    found_extreme_index = starting_index
    search_index = starting_index - 1
    interval_mult = 1.7 if search_min else 2.5
    while search_index >= 0 and \
            (starting_index - 1 == search_index or
             local_extremes[starting_index][0] - local_extremes[search_index][0] < interval_mult * search_interval or
             found_extreme_index == starting_index or
             abs(local_extremes[starting_index][1] - local_extremes[found_extreme_index][1]) < 30):
        if search_min:
            if local_extremes[search_index][1] < found_extreme[1]:
                found_extreme = local_extremes[search_index]
                found_extreme_index = search_index
            if local_extremes[starting_index][1] < local_extremes[search_index][1] and \
                    local_extremes[starting_index][0] - local_extremes[search_index][0] > 2. * search_interval:
                break  # already reached the other maximum
        else:
            if local_extremes[search_index][1] > found_extreme[1]:
                found_extreme = local_extremes[search_index]
                found_extreme_index = search_index
            if local_extremes[starting_index][1] > local_extremes[search_index][1] and \
                    local_extremes[starting_index][0] - local_extremes[search_index][0] > 2. * search_interval:
                break  # already reached the other minimum
        search_index -= 1

    if search_index < 0:
        return []  # data at the beginning of the graph is often unclear, rather skip the last extreme
    else:
        return left_search(local_extremes, found_extreme_index, search_interval, not search_min) \
               + ([found_extreme] if not search_min else [])


def right_search(local_extremes, starting_index, search_interval, search_min):
    found_extreme = local_extremes[starting_index]
    found_extreme_index = starting_index
    search_index = starting_index + 1
    interval_mult = 2.5 if search_min else 1.7
    while search_index < len(local_extremes) and \
            (starting_index + 1 == search_index or
             local_extremes[search_index][0] - local_extremes[starting_index][0] < interval_mult * search_interval or
             found_extreme_index == starting_index or
             abs(local_extremes[starting_index][1] - local_extremes[found_extreme_index][1]) < 30):
        if search_min:
            if local_extremes[search_index][1] < found_extreme[1]:
                found_extreme = local_extremes[search_index]
                found_extreme_index = search_index
            if local_extremes[starting_index][1] < local_extremes[search_index][1] and \
                    local_extremes[search_index][0] - local_extremes[starting_index][0] > 2. * search_interval:
                break  # already reached the other maximum
        else:
            if local_extremes[search_index][1] > found_extreme[1]:
                found_extreme = local_extremes[search_index]
                found_extreme_index = search_index
            if local_extremes[starting_index][1] > local_extremes[search_index][1] and \
                    local_extremes[search_index][0] - local_extremes[starting_index][0] > 2. * search_interval:
                break  # already reached the other minimum
        search_index += 1

    if search_index == len(local_extremes):
        return []  # data at the beginning of the graph is often unclear, rather skip the last extreme
    else:
        return ([found_extreme] if not search_min else []) \
               + right_search(local_extremes, found_extreme_index, search_interval, not search_min)


def analyse_freq_PP(trajectory_data, graph_file_path, create_graphs):
    zoom = []
    for species_idx in range(0, len(trajectory_data[0]["left"])):
        graph_data_extremes = []
        graph_data = []
        last_extreme_x = trajectory_data[0]["right"]
        prev_x_coord = trajectory_data[0]["right"]
        prev_y_coord = trajectory_data[0]["left"][species_idx]
        prev_diff = 0.
        max_diff = 0.
        max_index = 0
        max_y = 0.
        for trajectory_point in trajectory_data:
            x_coord = trajectory_point["right"]
            y_coord = trajectory_point["left"][species_idx]
            graph_data.append([x_coord, y_coord])

            # death filter
            if y_coord <= 0.:
                break

            # extreme calculation
            diff = y_coord - prev_y_coord
            if not same_sign(diff, prev_diff):
                graph_data_extremes.append([prev_x_coord, prev_y_coord])
                if abs(last_extreme_x - x_coord) > max_diff:
                    max_diff = abs(last_extreme_x - x_coord)
                if y_coord > max_y:
                    max_y = y_coord
                    max_index = len(graph_data_extremes) - 1
                last_extreme_x = prev_x_coord
            prev_x_coord = x_coord
            prev_y_coord = y_coord
            prev_diff = diff

        # recursive search
        graph_data_extremes = find_real_extremes(graph_data_extremes, max_index, max_diff)
        if zoom:
            graph_data_extremes = list(filter(lambda c: zoom[0] < c[0] < zoom[1], graph_data_extremes))
            graph_data = list(filter(lambda c: zoom[0] < c[0] < zoom[1], graph_data))
        max_y = max(list(zip(*graph_data))[1])
        points = np.array(graph_data)
        if create_graphs:
            print(graph_file_path)
            #pprint.pprint(max_diff)
            plt.clf()
            for extreme in graph_data_extremes:
                plt.plot(np.array([extreme[0], extreme[0]]), np.array([0, max_y]), 'o:r')
            plt.plot(points[:, 0], points[:, 1])
            plt.savefig(graph_file_path)
        # plt.show()
        return graph_data_extremes


def append_mean_and_st_dev(all_data, folder_results):
    all_data.append((statistics.mean(folder_results), statistics.stdev(folder_results)))


def print_stats_mean_and_st_dev(data, prop_name):
    means = [i for (i,j) in data]
    standard_deviations = [j for (i,j) in data]
    if not means:
        print("\tNot enough data to calculate mean")
    else:
        mean_of_means = statistics.mean(means)
        print("\tMean of %s is %.3g " % (prop_name, mean_of_means))
    if not len(standard_deviations) > 1:
        print("\tNot enough data to calculate standard deviation")
    else:
        mean_of_standard_deviations = statistics.mean(standard_deviations)
        print("\tStandard deviation of %s is %.3g " % (prop_name, mean_of_standard_deviations))
    #standard_deviation_of_means = statistics.stdev(means)
    #standard_deviation_of_standard_deviations = statistics.stdev(standard_deviations)

    #print("Standard deviation of means of %s is %s " % (prop_name, standard_deviation_of_means))
    #print("Standard deviation of standard deviations of %s is %s " % (prop_name,
    #                                                                  standard_deviation_of_standard_deviations))


def print_stats_normal_data(data, prop_name):
    if not data:
        print("\tNot enough data to calculate mean")
    else:
        mean = statistics.mean(data)
        print("\tMean of %s is %.3g " % (prop_name, mean))
    if not len(data) > 1:
        print("\tNot enough data to calculate standard deviation")
    else:
        standard_deviation = statistics.stdev(data)
        print("\tStandard deviation of %s is %.3g " % (prop_name, standard_deviation))


def print_model_stats(abr, data_for_model_stats, stats_file_path):
    minimum_number_of_sim_runs = 1000
    if abr == "PP":
        all_differences = []
        all_peak_heights = []
        all_death_percentage_none = []
        all_death_percentage_pred = []
        all_death_percentage_prey = []
        count_of_valid_folders = 0
        for folder_data in data_for_model_stats:
            if len(folder_data) < minimum_number_of_sim_runs:
                continue
            count_of_valid_folders += 1
            death_percentage_stats = [i for (i, j) in folder_data]
            all_death_percentage_none.append(death_percentage_stats.count(None) * 100. / len(death_percentage_stats))
            all_death_percentage_pred.append(death_percentage_stats.count("pred") * 100. / len(death_percentage_stats))
            all_death_percentage_prey.append(death_percentage_stats.count("prey") * 100. / len(death_percentage_stats))

            freq_stats = [j for (i, j) in folder_data]
            differences = []
            peak_heights = []
            for item in freq_stats:
                differences += [j[0] - i[0] for i, j in zip(item[:-1], item[1:])]
                peak_heights += [i[1] for i in item]
            append_mean_and_st_dev(all_differences, differences)
            append_mean_and_st_dev(all_peak_heights, peak_heights)

        if count_of_valid_folders == 0:
            return

        print("\tNumber of valid subfolders is %s " % count_of_valid_folders)
        print("_________________________________________________________________________")
        print_stats_normal_data(all_death_percentage_none, "nor predator nor prey dies percentage")
        print_stats_normal_data(all_death_percentage_pred, "predator dies percentage")
        print_stats_normal_data(all_death_percentage_prey, "prey dies percentage")
        print_stats_mean_and_st_dev(all_differences, "frequency")
        print_stats_mean_and_st_dev(all_peak_heights, "peak heights")
    elif abr == "TS":
        all_number_of_switches = []
        all_peak_heights = []
        all_dominance_percentage_0 = []
        all_dominance_percentage_1 = []
        all_dominance_percentage_2 = []
        all_dominance_percentage_3 = []
        all_dominance_percentage_4 = []
        all_dominance_percentage_5 = []
        count_of_valid_folders = 0
        for folder_data in data_for_model_stats:
            if len(folder_data) < minimum_number_of_sim_runs:
                continue
            count_of_valid_folders += 1
            
            dominance_stats = [j for (i, j) in folder_data]
            all_dominance_percentage_0.append(dominance_stats.count(0) * 100. / len(dominance_stats))
            all_dominance_percentage_1.append(dominance_stats.count(1) * 100. / len(dominance_stats))
            all_dominance_percentage_2.append(dominance_stats.count(2) * 100. / len(dominance_stats))
            all_dominance_percentage_3.append(dominance_stats.count(3) * 100. / len(dominance_stats))
            all_dominance_percentage_4.append(dominance_stats.count(4) * 100. / len(dominance_stats))
            all_dominance_percentage_5.append(dominance_stats.count(5) * 100. / len(dominance_stats))
            
            number_of_switches = []
            peak_heights = []
            frequence_data = [i for (i, j) in folder_data]
            for item in frequence_data:
                number_of_switches.append(item[0])
                peak_heights += item[1]
            append_mean_and_st_dev(all_number_of_switches, number_of_switches)
            append_mean_and_st_dev(all_peak_heights, peak_heights)

        print("\tNumber of valid subfolders is %s " % count_of_valid_folders)
        print("_________________________________________________________________________")
        print_stats_normal_data(all_dominance_percentage_0, "dominance 0 percentage")
        print_stats_normal_data(all_dominance_percentage_1, "dominance 1 percentage")
        print_stats_normal_data(all_dominance_percentage_2, "dominance 2 percentage")
        print_stats_normal_data(all_dominance_percentage_3, "dominance 3 percentage")
        print_stats_normal_data(all_dominance_percentage_4, "dominance 4 percentage")
        print_stats_normal_data(all_dominance_percentage_5, "dominance 5 percentage")
        print_stats_mean_and_st_dev(all_number_of_switches, "number of switches")
        print_stats_mean_and_st_dev(all_peak_heights, "peak heights")
    elif abr == "VI":
        all_death_counts = []
        all_dna_data = []
        all_rna_data = []
        all_p_data = []
        all_v_data = []
        count_of_valid_folders = 0
        for folder_data in data_for_model_stats:
            if len(folder_data) < minimum_number_of_sim_runs:
                continue
            count_of_valid_folders += 1
            death_count = 0
            dna_data = []
            rna_data = []
            p_data = []
            v_data = []
            for item in folder_data:
                if item[0]:
                    death_count += 1
                else:
                    dna_data.append(item[1])
                    rna_data.append(item[2])
                    p_data.append(item[3])
                    v_data.append(item[4])
            all_death_counts.append(death_count * 100. / len(folder_data))
            append_mean_and_st_dev(all_dna_data, dna_data)
            append_mean_and_st_dev(all_rna_data, rna_data)
            append_mean_and_st_dev(all_p_data, p_data)
            append_mean_and_st_dev(all_v_data, v_data)

        print("\tNumber of valid subfolders is %s " % count_of_valid_folders)
        print("_________________________________________________________________________")
        print_stats_normal_data(all_death_counts, "percentage of early deaths")
        print_stats_mean_and_st_dev(all_dna_data, "DNA count at t=200s")
        print_stats_mean_and_st_dev(all_rna_data, "RNA count at t=200s")
        print_stats_mean_and_st_dev(all_p_data, "P count at t=200s")
        print_stats_mean_and_st_dev(all_v_data, "V count at t=200s")
    elif abr == "EC":
        all_ribosome_death_times = []
        all_lacZ_peak_times = []
        all_lacZ_peak_heights = []
        all_product_at_2000 = []
        all_lactose_at_2000 = []
        count_of_valid_folders = 0
        for folder_data in data_for_model_stats:
            if len(folder_data) < minimum_number_of_sim_runs:
                continue
            count_of_valid_folders += 1
            ribosome_death_times = []
            lacZ_peak_times = []
            lacZ_peak_heights = []
            product_at_2000 = []
            lactose_at_2000 = []
            for item in folder_data:
                ribosome_death_times.append(item[0])
                lacZ_peak_times.append(item[1])
                lacZ_peak_heights.append(item[2])
                product_at_2000.append(item[3])
                lactose_at_2000.append(item[4])
            append_mean_and_st_dev(all_ribosome_death_times, ribosome_death_times)
            append_mean_and_st_dev(all_lacZ_peak_times, lacZ_peak_times)
            append_mean_and_st_dev(all_lacZ_peak_heights, lacZ_peak_heights)
            append_mean_and_st_dev(all_product_at_2000, product_at_2000)
            append_mean_and_st_dev(all_lactose_at_2000, lactose_at_2000)

        print("\tNumber of valid subfolders is %s " % count_of_valid_folders)
        print("_________________________________________________________________________")
        print_stats_mean_and_st_dev(all_ribosome_death_times, "ribosome death time")
        print_stats_mean_and_st_dev(all_lacZ_peak_times, "LacZ peak time")
        print_stats_mean_and_st_dev(all_lacZ_peak_heights, "LacZ peak height")
        print_stats_mean_and_st_dev(all_product_at_2000, "product count at t=2000s")
        print_stats_mean_and_st_dev(all_lactose_at_2000, "lactose count at t=2000s")
    elif abr == "RP":
        all_differences_pA = []
        all_peak_heights_pA = []
        all_differences_sA = []
        all_peak_heights_sA = []
        all_dominance_percentage_0 = []
        all_dominance_percentage_1 = []
        all_dominance_percentage_2 = []
        all_dominance_percentage_3 = []
        all_dominance_percentage_4 = []
        all_dominance_percentage_5 = []
        count_of_valid_folders = 0
        for folder_data in data_for_model_stats:
            if len(folder_data) < minimum_number_of_sim_runs:
                continue
            count_of_valid_folders += 1
            
            dominance_stats = [j for (i, j) in folder_data]
            all_dominance_percentage_0.append(dominance_stats.count(0) * 100. / len(dominance_stats))
            all_dominance_percentage_1.append(dominance_stats.count(1) * 100. / len(dominance_stats))
            all_dominance_percentage_2.append(dominance_stats.count(2) * 100. / len(dominance_stats))
            all_dominance_percentage_3.append(dominance_stats.count(3) * 100. / len(dominance_stats))
            all_dominance_percentage_4.append(dominance_stats.count(4) * 100. / len(dominance_stats))
            all_dominance_percentage_5.append(dominance_stats.count(5) * 100. / len(dominance_stats))
            
            frequence_data = [i for (i, j) in folder_data]
            differences_pA = []
            peak_heights_pA = []
            differences_sA = []
            peak_heights_sA = []
            for item in frequence_data:
                differences_pA += [j[0] - i[0] for i, j in zip(item[0][:-1], item[0][1:])]
                peak_heights_pA += [i[1] for i in item[0]]
                differences_sA += [j[0] - i[0] for i, j in zip(item[1][:-1], item[0][1:])]
                peak_heights_sA += [i[1] for i in item[1]]
            append_mean_and_st_dev(all_differences_pA, differences_pA)
            append_mean_and_st_dev(all_peak_heights_pA, peak_heights_pA)
            append_mean_and_st_dev(all_differences_sA, differences_sA)
            append_mean_and_st_dev(all_peak_heights_sA, peak_heights_sA)

        print("\tNumber of valid subfolders is %s " % count_of_valid_folders)
        print("_________________________________________________________________________")
        print_stats_normal_data(all_dominance_percentage_0, "dominance 0 percentage")
        print_stats_normal_data(all_dominance_percentage_1, "dominance 1 percentage")
        print_stats_normal_data(all_dominance_percentage_2, "dominance 2 percentage")
        print_stats_normal_data(all_dominance_percentage_3, "dominance 3 percentage")
        print_stats_normal_data(all_dominance_percentage_4, "dominance 4 percentage")
        print_stats_normal_data(all_dominance_percentage_5, "dominance 5 percentage")
        print_stats_mean_and_st_dev(all_differences_pA, "frequency (pA)")
        print_stats_mean_and_st_dev(all_peak_heights_pA, "peak heights (pA)")
        print_stats_mean_and_st_dev(all_differences_sA, "frequency (sA)")
        print_stats_mean_and_st_dev(all_peak_heights_sA, "peak heights (sA)")


def main():
    # Here set the path to the benchmarks data (to the directory where you have PP_Visual_SSA and others)
    path_to_files = "D:\\Datein\\TUM\\Forschung\\sequaia-mvn\\results\\benchmark\\20230205_visual"
    if len(sys.argv) > 1:
        path_to_files = sys.argv[1]
    
    create_graphs = False

    print("_________________________________________________________________________")
    print("__________________Model specific properties evaluation___________________")

    abr_to_model_name = {
        "PP": "Predator prey",
        "RP": "Repressilator",
        "VI": "Viral",
        "TS": "Toggle switch",
        #"EC": "E. coli"
    }
    for data_directory_name in os.listdir(path_to_files):
        if not os.path.isdir(os.path.join(path_to_files, data_directory_name)):
            continue
        print("_________________________________________________________________________")
        abr = data_directory_name[:2]
        if abr not in abr_to_model_name.keys():
            print("\tInvalid folder %s" % data_directory_name)
            print("\tUnable to detect the model from the folder's name")
            print("_________________________________________________________________________")
            continue
        print("\tModel %s" % abr_to_model_name[abr])
        print("\tFolder %s" % data_directory_name)
        all_data_for_stats = []
        for sub_dir_name in [str(i) for i in range(10000)]:
            path_to_file_folder = os.path.join(path_to_files, data_directory_name, sub_dir_name)
            if not os.path.exists(path_to_file_folder):
                break
            if create_graphs:
                graphs_dir = os.path.join(path_to_files, data_directory_name, sub_dir_name, "freq")
                if not os.path.exists(graphs_dir):
                    os.makedirs(graphs_dir)
            path_to_file_folder = os.path.join(path_to_file_folder, "data")
            data_for_model_stats = []
            for file in os.listdir(path_to_file_folder):
                if not file.endswith(".json"):
                    continue
                f = open(os.path.join(path_to_file_folder, file))
                data = json.load(f)
                trajectory_data = data["history"]
                analysis_folder = os.path.join(path_to_files, data_directory_name, sub_dir_name, "freq")
                graph_file_path = os.path.join(analysis_folder, os.path.splitext(file)[0] + ".png")
                if abr == "PP":
                    data_for_model_stats.append((who_dies_PP(trajectory_data),
                                                 analyse_freq_PP(trajectory_data, graph_file_path, create_graphs)))
                elif abr == "RP":
                    data_for_model_stats.append((analyse_freq_RP(trajectory_data, graph_file_path, create_graphs), dominance(trajectory_data)))
                elif abr == "TS":
                    data_for_model_stats.append((get_num_of_switches_TS(trajectory_data, graph_file_path, create_graphs), dominance(trajectory_data)))
                elif abr == "VI":
                    data_for_model_stats.append(is_viral_early_death_and_DNA_RNA_P_V(trajectory_data))
                elif abr == "EC":
                    data_for_model_stats.append(zero_Ribosome_peak_lacZ_EC(trajectory_data))
                f.close()
            all_data_for_stats.append(data_for_model_stats)
        print_model_stats(abr, all_data_for_stats,
                          os.path.join(path_to_files, data_directory_name, "freq", "stats.txt"))
        print("_________________________________________________________________________")


if __name__ == "__main__":
    main()
