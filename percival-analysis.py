from collections import defaultdict
from bson import ObjectId
from pymongo import *

#todo configure in some sane fashion; probably take from envvars or provide CLI with arpgarse
client = MongoClient('mongodb://thinker:thinker@localhost:27017/')
DB_NAME = "thinker"

db = client[DB_NAME]
tasks = db.taskDefinition
runs = db.persistentTaskRun

#keys[name][class][field][value] = # of times field f had value v
keys = defaultdict(lambda: defaultdict(lambda: defaultdict(lambda: defaultdict(int))))

#counts[name][class] = # of tasks under that key group
counts = defaultdict(lambda: defaultdict(int))

finished = defaultdict(lambda: defaultdict(int))

#that[name][class][field] = # of finished tasks that match that "path"
finished_by_field = defaultdict(lambda: defaultdict(lambda: defaultdict(int)))

def print_general_stats():
    print("tasks "+str(tasks.estimated_document_count()))
    print("runs "+str(runs.estimated_document_count()))


def analyse_deeper():
    for t in tasks.find(dict()):
        k = t["key"]
        n = k.get("name", "")
        p = k.get("parameters", {"_class": ""})
        c = p["_class"]
        for k, v in dict(p).items():
            if isinstance(v, ObjectId):
                v = str(v)
            if k != "_class":
                keys[n][c][k][v] += 1
                finished_by_field[n][c][v] += 1
        counts[n][c] += 1
        if "succesfulRun" in t and t["succesfulRun"]:
            finished[n][c] += 1

def show_numbers_of_tasks():
    for n, vc in keys.items():
        for c, vf in vc.items():
            print("Name:", n)
            print("Class:", c)
            print("# of tasks:   ", counts[n][c])
            print("# of finished:", finished[n][c])
            for f, v in vf.items():
                lv = list(v.keys())
                PER_BUNCH = 10
                first_bunch = lv[:PER_BUNCH]
                last_bunch = []
                if len(v) > 2*PER_BUNCH:
                    last_bunch = lv[-PER_BUNCH:]
                elif len(v) > PER_BUNCH:
                    last_bunch = lv[PER_BUNCH:]
                print("\tField "+f+" ["+str(len(v))+"]:")
                for x in first_bunch:
                    print("\t\t", x)
                if len(v) > PER_BUNCH:
                    print("\t\t...")
                for x in last_bunch:
                    print("\t\t", x)
            soft_separator()

def show_duplicates(clazz, name):
    duplicates = [ k for k in clazz[name].keys() if clazz[name][k] > 1 ]
    print("Duplicated", name+":", duplicates)


#todo this is an area for endless refactor and fancy APIs
SCREEN_WIDTH=80

def strong_separator():
    print("="*SCREEN_WIDTH)

def soft_separator():
    print("-"*SCREEN_WIDTH)

def stars():
    print("*"*SCREEN_WIDTH)

def show_all_duplicates(field_name):
    for name, v in keys.items():
        for clazz, cv in v.items():
            #fixme will that work e.g. for integer?
            if field_name in cv:
                print("Name:", name)
                print("Parameters", clazz)
                show_duplicates(cv, field_name)
                soft_separator()

def show_field_histogram(clazz, field):
    print("Field:", field)
    print(clazz[field])

def show_all_field_histograms(field_name):
    for name, v in keys.items():
        for clazz, cv in v.items():
            #fixme will that work e.g. for integer?
            if field_name in cv:
                print("Name:", name)
                print("Parameters", clazz)
                show_field_histogram(cv, field_name)
                soft_separator()


def print_first_iterations():
    it0 = list(tasks.find({"key.parameters.iteration": 0}))
    for x in it0:
        print(x)
        print('-'*80)

    print(len(it0))


def main():
    print("GENERAL")

    print_general_stats()

    strong_separator()

    analyse_deeper()

    show_numbers_of_tasks()

    strong_separator()

    print("TASKS")

    show_numbers_of_tasks

    strong_separator()

    print("DUPLICATES OF ITERATIONS")

    show_all_duplicates("iteration")

    strong_separator()

    print("INTERPRETATIONS")

    show_all_field_histograms("interpretation")

    strong_separator()

if __name__=="__main__":
    main()