from __future__ import division

from nltk.tokenize import word_tokenize
from nltk.stem import PorterStemmer
from nltk import FreqDist
import math
import random
import time
import pickle
import sqlite3
import os.path


__author__ = 'hei'

class CorpusStat(object):
    @staticmethod
    def loadFromDb(database):
        corpusStat = CorpusStat.__new__(CorpusStat)

        print("Loading CorpusStat from database " + database)
        startTime = time.time()

        connection = None
        try:
            connection = sqlite3.connect(database)
            connection.text_factory = str


            cursor = connection.cursor()

            cursor.execute("SELECT * from Corpus")
            corpusFromDb = cursor.fetchall()
            corpusStat.corpus = [lineTuple[0] for lineTuple in corpusFromDb]

            print("Loaded corpus")

            cursor.execute("SELECT Token, Count from UtteranceCount")
            utteranceCount = cursor.fetchall()
            corpusStat.utteranceCount = {token: count for (token, count) in utteranceCount}

            print("Loaded utterance count")

            cursor.execute("SELECT LineID, Token, Value FROM Context ORDER BY LineID")
            contexts = cursor.fetchall()
            corpusStat.context = list()
            for lineId, token, value in contexts:
                while lineId >= len(corpusStat.context):
                    corpusStat.context.append(dict())
                corpusStat.context[lineId][token] = value

            print("Loaded contexts")

            cursor.execute("SELECT Attribute, Value FROM Attribute")
            attributes = cursor.fetchall()
            for attribute, value in attributes:
                value = pickle.loads(value)
                setattr(corpusStat, attribute, value)

            print("Loaded attributes")

        except sqlite3.Error:
            pass
        finally:
            if connection:
                connection.close()

        diff = time.time() - startTime
        print("Finished loading from database in {:.2f}s".format(diff))

        return corpusStat

    def __init__(self, corpus, previousTurnNum=1):
        self.initialize(corpus, previousTurnNum)

    def initialize(self, corpus, previousTurnNum):
        print('Initializing...')
        startTime = time.time()
        corpusFile = open(corpus, 'r')
        self.corpus = [line.strip() for line in corpusFile.readlines()]
        corpusFile.close()

        stemmer = PorterStemmer()
        self.utteranceCount = FreqDist()
        utteranceTokenCount = list()
        for utterance in self.corpus:
            tokens = word_tokenize(utterance)
            stemToken = [stemmer.stem(token.lower()) for token in tokens]

            tokenCount = FreqDist(stemToken)
            utteranceTokenCount.append(tokenCount)
            self.utteranceCount.update(tokenCount.keys())

        self.previousTurnNum = previousTurnNum

        self.context = list()
        corpusSize = len(self.corpus)
        for i, utterance in enumerate(self.corpus):
            previousTurns = utteranceTokenCount[max(i - self.previousTurnNum, 0):i]
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
        print('Initialization finished in {:.2f}s'.format(diff))

    @property
    def allTokens(self):
        return list(self.utteranceCount.keys())

    def dumpToDatabase(self, database):
        startTime = time.time()
        connection = None
        try:
            connection = sqlite3.connect('corpus.db')
            connection.text_factory = str

            cursor = connection.cursor()

            cursor.execute("DROP TABLE IF EXISTS Corpus")
            cursor.execute("CREATE TABLE Corpus(Utterance TEXT)")
            cursor.executemany("INSERT INTO Corpus VALUES(?)", [tuple([line]) for line in self.corpus])

            print('Dumped corpus')

            cursor.execute("DROP TABLE IF EXISTS UtteranceCount")
            cursor.execute("CREATE TABLE UtteranceCount(Token TEXT, Count INTEGER)")
            cursor.executemany("INSERT INTO UtteranceCount VALUES(?, ?)", self.utteranceCount.items())

            print('Dumped utterance count')

            cursor.execute("DROP TABLE IF EXISTS Context")
            cursor.execute("CREATE TABLE Context(LineID INTEGER, Token TEXT, Value REAL)")
            for i, context in enumerate(self.context):
                for token, value in context.items():
                    cursor.execute("INSERT INTO Context VALUES(?, ?, ?)", (i, token, value))

            print('Dumped context')

            cursor.execute("DROP TABLE IF EXISTS Attribute")
            cursor.execute("CREATE TABLE Attribute(Attribute TEXT, Value Text)")
            previousTurnNumDump = pickle.dumps(self.previousTurnNum)
            cursor.execute("INSERT INTO Attribute VALUES(?, ?)", ("previousTurnNum", previousTurnNumDump))

            print('Dumped attribute')

            connection.commit()
        finally:
            if connection:
                connection.close()

        diff = time.time() - startTime
        print('Dumped to database in {:.2f}s'.format(diff))

class ChatBot(object):
    def __init__(self, corpusStat):
        self.corpusStat = corpusStat


    def guess(self, turns):
        startTime = time.time()
        if len(turns) != self.corpusStat.previousTurnNum:
            raise ValueError("Invalid number of turns")

        corpusSize = len(self.corpusStat.corpus)
        stemmer = PorterStemmer()
        feature = dict()
        for i, utterance in enumerate(turns):
            tokens = word_tokenize(utterance)
            stemToken = [stemmer.stem(token.lower()) for token in tokens]
            tokenCount = FreqDist(stemToken)

            for token, count in tokenCount.items():
                utteranceCount = self.corpusStat.utteranceCount.get(token, 0) + 1
                tf = 1 + count
                idf = math.log((corpusSize+1) / utteranceCount)
                lastTurnNum = self.corpusStat.previousTurnNum - i
                h = math.exp(-lastTurnNum * lastTurnNum / 2.0)
                weight = tf * idf * h

                feature[token] = feature.get(token, 0) + weight

        tokenNum = len(self.corpusStat.allTokens)
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

        corpusSize = len(self.corpusStat.corpus)
        maxValue = 0
        matchUtterance = ["I don't know"]
        for i in range(corpusSize):
            vector = self.corpusStat.context[i]
            dotProduct = dot(vector, feature)
            vectorNorm = norm(vector)
            if vectorNorm:
                value = dotProduct / vectorNorm
                if value >= maxValue:
                    utterance = self.corpusStat.corpus[i] + '\n'
                    for j in range(max(0, i-self.corpusStat.previousTurnNum), i):
                        utterance += "\nn-{}>> {}".format(i - j, self.corpusStat.corpus[j])

                    if value == maxValue:
                        matchUtterance.append(utterance)
                    else:
                        maxValue = value
                        matchUtterance = [utterance]

        diff = time.time() - startTime
        return random.choice(matchUtterance) + "\nSearched in {:.2f}s".format(diff)


if __name__ == '__main__':
    dbExists = os.path.isfile('corpus.db')
    if dbExists:
        corpusStat = CorpusStat.loadFromDb('corpus.db')
    else:
        corpusStat = CorpusStat('corpus')
        corpusStat.dumpToDatabase('corpus.db')

    bot = ChatBot(corpusStat)
    chatLog = ['', '']
    while True:
        line = raw_input('You: ')
        chatLog.append(line)
        turns = chatLog[max(0, len(chatLog) - bot.corpusStat.previousTurnNum):]
        botGuess = bot.guess(turns)
        print('Bot: ' + botGuess + '\n')
