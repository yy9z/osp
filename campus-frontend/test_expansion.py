# Simulate expandKeywords
expansions = {
    "食堂": ["食堂", "学生食堂", "餐厅"]
}

def expand(query):
    trimmed = query.strip()
    for k, v in expansions.items():
        if trimmed == k or k in trimmed:
            res = [trimmed]
            for val in v:
                if val != trimmed:
                    res.append(val)
            return res
    return [trimmed]

print(expand("南区食堂"))
