import requests



questions = {
    "AskPage sample question": ["Explain renewable generation trends between 2020 and 2023",
                                "What are the main sources of electricity generation in New Zealand?",
                                "Compare hydro and geothermal generation patterns",
                                "Explain hydro generation trends between 2018 and 2023",
                                ],

    "Phase 15 expected non-refusals": ["How has renewable electricity generation changed in New Zealand since 2000?",
                                       "Compare hydro and geothermal electricity generation trends over the last 20 years?",
                                       "What are the main sources of electricity generation in New Zealand today?",
                                       "Has wind generation increased faster than hydro generation in recent years?",
                                       "How has solar electricity generation changed since it was first recorded?",
                                       "What does river water quality look like across New Zealand right now?",
                                       "How has the number of excellent-quality river sites changed over time?",
                                       "Which regions have the highest proportion of poor river water quality sites?",
                                       "Are more river sites improving or degrading in water quality over time?",
                                       "How do water quality trends compare across regions?",
                                       ],
    "Phase 15 reaches": ["Which renewable fuel has grown the most in total generation since 2005?",],

    "Phase 15 expected refusals": ["Why did hydro generation drop in 2012",
                                   "Will solar overtake wind by 2030?",
                                   "Should NZ invest more in geothermal?",
                                   "What caused river quality to decline in Waikato?",
                                   ],

    "Phase 16": ["In what 5-year period did New Zealand have the biggest incre  ase in solar electricity generation?",
                 "When did wind generation grow the fastest over any 3-year period?",
                 "What 10-year period saw the largest drop in coal generation?",
                 "Which renewable fuel has increased the most in total generation since 2005?",
                 "Which fuel contributed the most to total generation growth between 2010 and 2024?",
                 "How has the share of renewable electricity changed over time?",
                 "When did renewable electricity first exceed 80% of total generation?",
                 "Which 5-year period saw the largest improvement in the number of excellent-quality river sites?",
                 "Which region has seen the largest improvement in water quality over the last decade?",
                 "Has renewable electricity growth accelerated in the last decade compared to the previous decade?",
                 ],
}


url = "http://localhost:8080/api/v1/explanations/ask"
with open("./out.txt", 'w') as f:
    for topic, questions in questions.items():
        log = f"Testing {topic}:"
        print(log)
        f.write(log + '\n')
        for index, q in enumerate(questions):
            log = f"Processing question {index+1} of {len(questions)}"
            print(log)
            payload = {"question": q}
            response = requests.post(url, json=payload)
            f.write('\t* '+q + '\n')
            f.write('\t\t'+response.text + '\n\n')
        print('\n\n')


