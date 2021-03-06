import httplib,urllib
import time
import os
from multiprocessing import Pool
# import matplotlib.pyplot as plt
from random import randint,sample

URL = "www.google.com"
# URL = "flask3.qst6ftqmmz.us-west-2.elasticbeanstalk.com"
POST_END = "/String/"
GET_END = "/Counts/"
NUM_THREADS = 50
NUM_POST_THREADS = 10


class Envelope:

    def __init__(self, request_type, response_time, word_count = 0):
        self.request_type = request_type
        self.response_time = response_time
        self.word_count = word_count


def get_worker(data, id):
    conn = httplib.HTTPConnection(URL)
    result = []

    for i in range(len(data)):
        # if i%50 == 0:
        #     print(id, i)
        # start_time = time.time()
        conn.request("GET", GET_END+data[i])
        start_time = time.time()
        response = conn.getresponse()
        elapse_time = time.time() - start_time
        response.read()
        result.append(Envelope("GET", elapse_time))

        conn.request("GET", GET_END)
        response = conn.getresponse()
        response.read()

        conn.request("GET", GET_END+"'1' OR '1' = '1'")
        response = conn.getresponse()
        response.read()

    conn.close()
    return result


def post_worker(data, id):
    conn = httplib.HTTPConnection(URL)
    result = []
    for i in range(len(data)):
        # if i%50 == 0:
        #     print(id, i)
        item = data[i]
        word_count = len(item.split())
        # item = urllib.quote(item)
        # start_time = time.time()
        headers = {"Content-type": item, "Accept": "text/plain"}
        params = urllib.urlencode({})
        conn.request("POST", POST_END+urllib.quote(item), params, headers)
        start_time = time.time()
        response = conn.getresponse()
        elapse_time = time.time() - start_time
        response.read()
        result.append(Envelope("POST", elapse_time, word_count))

        #empty request
        conn.request("POST", POST_END)
        response = conn.getresponse()
        response.read()

        #sql injection
        headers = {"Content-type": "'1' OR '1' = '1'", "Accept": "text/plain"}
        conn.request("POST", POST_END+"'1' OR '1' = '1'", params, headers)
        response = conn.getresponse()
        response.read()
    conn.close()
    return result


def main():
    post_data = []
    get_data = []
    vicious_words = ["null", "Null", "NULL", " ", ",,,,", ",,NULL,,,null", "a,a,b, , ,,,,"]

    with open('../data/index.txt', 'r') as dict_file:
        lines = dict_file.read().split("\n")
        del (lines[-1])
        print "read in dict file"
        for i in range(NUM_POST_THREADS):
            temp = []
            for j in range(1000):
                targets = sample(range(len(lines)), 999)
                query = ' '.join(lines[x] for x in targets)
                query += " " + vicious_words[randint(0,len(vicious_words)-1)]
                temp.append(query)
            post_data.append(temp)

        for i in range(NUM_THREADS - NUM_POST_THREADS):
            temp = []
            for j in range(1000):
                targets = sample(range(len(lines)), 9)
                query = ','.join(lines[x] for x in targets)
                query += "," + vicious_words[randint(0,len(vicious_words)-1)]
                temp.append(query)
            get_data.append(temp)

    print("ready for pool")
    pool = Pool(processes=NUM_THREADS)
    res = []
    for i in range(NUM_THREADS):
        if i < NUM_POST_THREADS:
            res.append(pool.apply_async(post_worker, args=(post_data[i], i)))
            # res.append(pool.apply_async(post_worker,(post_data, i)))
        else:
            res.append(pool.apply_async(get_worker, args=(get_data[i-NUM_POST_THREADS], i)))
            # res.append(pool.apply_async(get_worker,(get_data, i)))
    pool.close()
    pool.join()
    res_processed = map(lambda x: x.get(timeout=1), res)
    results = reduce(lambda x,y: x+y, res_processed)
    total_count = 0
    total_time = 0
    for item in results:
        if item.request_type == "GET":
            continue
        total_count += item.word_count
        total_time += item.response_time

    times = map(lambda x: x.response_time, results)
    # print(times)
    times = sorted(times)
    total = 0
    worst = 0
    cur_bucket = 0
    x_axis = []
    y_axis = []
    initial = False
    for value in times:
        total += value
        if value > worst:
            worst = value

        if value > 1 and not initial:
            x_axis.append(1.0)
            y_axis.append(1)
            initial = True
        elif cur_bucket == 0 or (value <= 1 and value - cur_bucket >= 0.02):
            x_axis.append(value)
            cur_bucket = value
            y_axis.append(1)
        else:
            y_axis[-1] += 1

    print "Mean response time is: " + str(total/len(results))
    print "Worst response time is: " + str(worst)
    print "Average process time/word for POST: " + str(total_time/total_count)
    print x_axis
    print y_axis
    # plt.figure()
    # plt.bar(x_axis, y_axis, 0.02)
    # plt.show()


if __name__ == '__main__':
    main()