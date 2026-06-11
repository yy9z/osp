# If user query="南区食堂" campus=null (or campus="南校区")
# query is "南区食堂"
# expandedTerms = ["南区食堂", "食堂", "学生食堂", "餐厅"]
# searchByText(expandedTerms[0], campus) -> searchByText("南区食堂", "南校区")
# buildSearchKeyword("南区食堂", "南校区") -> "中科大南区南区食堂"
