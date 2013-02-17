import os
import argparse
import re
from collections import Counter
import csv
import numpy as np

from scipy import stats

import matplotlib
matplotlib.use('QT4Agg')
import matplotlib.pyplot as plt

from functools import wraps


SPLIT = re.compile('\W+')

def get_name(directory):
    return os.path.split(directory[:-1] if directory[-1] == os.path.sep else directory)[1]

def skew(func):
    @wraps(func)
    def f(n, directory):
        distribution = func(n, directory)
        if distribution:
            last = distribution[-1]
            total = 1 / last[2]
            skew_list = list()
            for _, i, p in distribution:
                skew_list.extend([i] * int(p * total))
            skew = stats.skew(skew_list)
        
            name = get_name(directory)
            print('{} skew: {}'.format(name, skew))
        return distribution
    
    return f


def zero_closeness(func):
    @wraps(func)
    def f(n, directory):
        distribution = func(n, directory)
        closeness = sum(i * v for _, i, v in distribution)
        name = get_name(directory)
        print('{} zero closeness: {}'.format(name, closeness))
        return distribution
    
    return f


def csv_out(func):
    @wraps(func)
    def f(n, directory):
        distribution = func(n, directory)
        filename = get_name(directory) + '.csv'
        with open(filename, 'w') as f:
            csv.writer(f).writerows(distribution)
        return distribution
    
    return f


PLOTS = list()
STYLES = ['r', 'b', 'g']
def plot(func):
    @wraps(func)
    def f(n, directory):
        distribution = func(n, directory)
        x, y = zip(*((i, value) for _, i, value in distribution))
        p, = plt.plot(x, y, STYLES[len(PLOTS) % len(STYLES)])
        PLOTS.append(p)
        return distribution

    return f


def ngrams(line, n):
    tokens = [None] + [token.lower() for token in SPLIT.split(line) if token] + [None]
    tokens_len = len(tokens)

    if tokens_len > n:
        ngrams = list()
        for i in range(tokens_len - n):
            ngrams.append(tuple(tokens[i:i+n]))
        return ngrams
    else:
        return [tuple(tokens + [None] * (tokens_len - n))]

@plot
@csv_out
@skew
@zero_closeness
def distribution(n, directory):
    counter = Counter()
    for path, folder, files in os.walk(directory):
        for filename in files:
            with open(os.path.join(path, filename)) as f:
                for line in f:
                    for l in line.split('.'):
                        l = l.strip()
                        if l:
                            groups = ngrams(l, n)
                            counter.update(groups)

    total_count = sum(counter.values())
    density = list((tokens, i, freq / total_count) for i, (tokens, freq) in enumerate(counter.most_common()))

    return density

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Analyse uniquness with n-gram')
    parser.add_argument('-n', dest='n', type=int, help='N of the n-gram')
    parser.add_argument('-c', dest='dirs', nargs='+', help='Directory of corpus to analyse')

    args = parser.parse_args()
    dirs = args.dirs
    n = args.n

    for i, directory in enumerate(dirs):
        distribution(n, directory)

    plt.legend(PLOTS, (get_name(directory) for directory in dirs))
    plt.grid(True)
    plt.show()
