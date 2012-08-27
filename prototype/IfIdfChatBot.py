from __future__ import division

from nltk.tokenize import word_tokenize
from nltk.stem import PorterStemmer
from nltk import FreqDist
import math

    def initialize(self, corpus, previousTurnNum):
        print('Initializing...')
        startTime = time.time()
        corpusFile = open(corpus, 'r')
        self.corpus = [line.strip() for line in corpusFile.readlines()]
        corpusFile.close()

import random
import time

__author__ = 'hei'

class ChatBot(object):
    def __init__(self, corpus, previousTurnNum=2):
        self.initialize(corpus, previousTurnNum)
        stemmer = PorterStemmer()
        self.utteranceCount = FreqDist()
        self.utteranceTokenCount = list()
        for utterance in self.corpus:
            tokens = word_tokenize(utterance)
            stemToken = [stemmer.stem(token.lower()) for token in tokens]

            tokenCount = FreqDist(stemToken)
            self.utteranceTokenCount.append(tokenCount)
            self.utteranceCount.update(tokenCount.keys())

        self.allTokens = list(self.utteranceCount.keys())
        self.previousTurnNum = previousTurnNum

        self.context = list()
        corpusSize = len(self.corpus)
        for i, utterance in enumerate(self.corpus):
            previousTurns = self.utteranceTokenCount[max(i - self.previousTurnNum, 0):i]
            feature = dict()
            for j, turn in enumerate(previousTurns):
                maxCount = max(turn.values())
                for token, count in turn.items():
                    utteranceCount = self.utteranceCount[token]
                    tf = 1 + math.log(count / maxCount)
                    idf = math.log(corpusSize / utteranceCount)
                    lastTurnNum = self.previousTurnNum - j
                    h = math.exp(-lastTurnNum * lastTurnNum / 2.0)
                    weight = tf * idf * h

                    if feature.has_key(token):
                        feature[token] += weight
                    else:
                        feature[token] = weight
            self.context.append(feature)

        diff = time.time() - startTime
        print('Initialization finished in {:.2f}ms'.format(diff))

    def guess(self, turns):
        startTime = time.time()
        if len(turns) != self.previousTurnNum:
            raise ValueError("Invalid number of turns")

        corpusSize = len(self.corpus)
        stemmer = PorterStemmer()
        feature = dict()
        for i, utterance in enumerate(turns):
            tokens = word_tokenize(utterance)
            stemToken = [stemmer.stem(token.lower()) for token in tokens]
            tokenCount = FreqDist(stemToken)

            for token, count in tokenCount.items():
                utteranceCount = self.utteranceCount.get(token, 0) + 1
                tf = 1 + count
                idf = math.log((corpusSize+1) / utteranceCount)
                lastTurnNum = self.previousTurnNum - i
                h = math.exp(-lastTurnNum * lastTurnNum / 2.0)
                weight = tf * idf * h

                feature[token] = feature.get(token, 0) + weight

        tokenNum = len(self.allTokens)
        def norm(feature):
            magnitude = 0
            for _, value in feature.items():
                magnitude += value * value
            return magnitude ** (1/tokenNum)

        def dot(feature1, feature2):
            keys = set(feature1.keys())
            keys.intersection_update(feature2.keys())
            dotProduct = 0
            for key in keys:
                dotProduct += feature1[key] * feature2[key]
            return dotProduct

        corpusSize = len(self.corpus)
        maxValue = 0
        matchUtterance = ["I don't know"]
        for i in range(corpusSize):
            vector = self.context[i]
            dotProduct = dot(vector, feature)
            vectorNorm = norm(vector)
            if vectorNorm:
                value = dotProduct / vectorNorm
                if value >= maxValue:
                    utterance = self.corpus[i] + '\n'
                    for j in range(max(0, i-self.previousTurnNum), i):
                        utterance += "\nn-{}>> {}".format(i - j, self.corpus[j])

                    if value == maxValue:
                        matchUtterance.append(utterance)
                    else:
                        maxValue = value
                        matchUtterance = [utterance]

        diff = time.time() - startTime
        return random.choice(matchUtterance) + "\nSearched in {:.2f}ms".format(diff)


if __name__ == '__main__':
    bot = ChatBot('corpus', 2)
    chatLog = ['', '']
    while True:
        line = raw_input('You: ')
        chatLog.append(line)
        turns = chatLog[max(0, len(chatLog) - bot.previousTurnNum):]
        botGuess = bot.guess(turns)
        print('Bot: ' + botGuess + '\n')
