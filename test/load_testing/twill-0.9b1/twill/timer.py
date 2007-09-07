#import os, Numeric, time, tempfile, logging, pickle
import os
#import numpy
import time, tempfile, logging, pickle

class Timer(object):
    def __init__(self): 
        self.ROOT_DIR = '/tmp/twill.logs/'
        self.LOG_DIR = None
        self.LOG_FILE = None
        tempfile.tempdir = ''
        self.DEBUG = False

        self.begin = None
        self.end = None
        self.average = None
        self.response_times = []
        self.response_time_dates = []
        self.sigma = None

    def set_time(self):
        if self.begin is None and self.end is None:
            self.begin = time.time() 
        elif self.begin and self.end is None:
            self.end = time.time()
            self.elapsed_time()
        elif self.begin and self.end:
            self.begin = time.time()
            self.end = None

    def elapsed_time(self):
        elapsed_time = self.end - self.begin          
        print 'ELAPSED TIME: ' + str(elapsed_time) + ' secs' 
        self.response_times.append(elapsed_time)
        self.response_time_dates.append(self.begin)

    def average_response(self):

        if len(self.response_times) == 0:
            self.average = 0.0
            return

        self.average = numpy.average(self.response_times) 

        if self.DEBUG:
            print 'DEBUG: num_responses = %s' % len(self.response_times)
            print 'DEBUG: total_time = %s' % sum(self.response_times)
            print 'DEBUG: max response = %s' % max(self.response_times)
            print 'DEBUG: min reponse = %s' % min(self.response_times)
            print 'DEBUG: average response = %s' % self.average
            self.print_responses() 

    def standard_deviation(self):
        self.average_response()
        num_responses = float(len(self.response_times))
        if num_responses == 0:
            self.sigma = 0
            return

        sum_of_squared_responses = 0.0

        for i in self.response_times:
            sum_of_squared_responses += numpy.power(i,2) 

        self.sigma =  numpy.sqrt((sum_of_squared_responses/num_responses) - numpy.power(self.average,2))

    def makerootlogdir(self):
        if not os.path.exists(self.ROOT_DIR):
            try:
                os.mkdir(self.ROOT_DIR)
            except OSError:
                pass 

    def makelogdir(self):
        self.makerootlogdir()
        if self.LOG_DIR is None:
            self.LOG_DIR = self.ROOT_DIR + tempfile.mktemp()
            
        if not os.path.exists(self.LOG_DIR):
            try:
                os.mkdir(self.LOG_DIR)
            except OSError:
                pass

    def collectstats(self):
        if len(self.response_times) == 0:
            self.maximum = 0.0
            self.minimum = 0.0
        else:
            self.maximum = max(self.response_times)
            self.minimum = min(self.response_times)

        self.standard_deviation() # calls average_response() since the mean is needed anyways
        

    def print_stats(self):

        self.collectstats()
        stats = '''
                MAX RESPONSE TIME: %s
            AVERAGE RESPONSE TIME: %s
                MIN RESPONSE TIME: %s
                    STD DEVIATION: %s
        ''' % (self.maximum, self.average, self.minimum, self.sigma)
        print stats
        return stats 

    def print_responses(self):
        for i in range(0, len(self.response_times)):
            print 'RESPONSE_TIME ' + str(i) + ': ' + str(self.response_times[i])

    def clear_timer(self):
        self.begin = None
        self.end = None
        self.response_times = []
        self.average = None

    def logstats(self):

        self.collectstats()
        self.makelogdir()
        if self.LOG_FILE is None:
            self.LOG_FILE = self.LOG_DIR + '/timer.pkl'

        #if not os.path.exists(self.LOG_FILE):
        #    os.mknod(self.LOG_FILE)

        file = open(self.LOG_FILE, 'w')
        pickle.dump(self, file)
        file.close()

        print 'timer has been pickeled to: %s' % self.LOG_FILE

    def __getstate__(self):
        object_dict = self.__dict__.copy() 
        return object_dict
