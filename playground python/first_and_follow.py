non_terminals = ("S", "A", "B")
terminals = ("a", "b")
start = "S"
production_rules = {
    "S" : ["A", "B"],
    "A" : ["a", []],
    "B" : ["b"]
}

def first(non_terminals, terminals, production_rules, start):
    