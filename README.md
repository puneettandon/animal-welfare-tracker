# 🐾 Animal Welfare News Sentiment Tracker

An automated system that monitors Indian news coverage on animal welfare, classifies public sentiment using AI, and visualizes media framing trends to empower activists, NGOs, and citizens.

## 🌐 Live Project
- **Code4Compassion Project Assign Doc:** [Problem Statement Link](https://docs.google.com/document/d/18ZZxsBfXGeVFaLDdz0Fpawb23oosqmJbKDP6D8flDyE/edit?tab=t.0#heading=h.7cfk6vdyundq) 
- **🌍 Website:** [animal-welfare-tracker-1.onrender.com](https://animal-welfare-tracker-1.onrender.com/)
- **📊 Google Sheet Output:** [View Sheet](https://docs.google.com/spreadsheets/d/1tKj5fj7CxLryrtVvToGq2gKTMRhD-3AwYjx-dpX22aE/)
- **📽 Presentation Deck:** [View Presentation](https://docs.google.com/presentation/d/16EhPp5aJBEyoi9uRsp-v-d_QpJ02iMGCROCdlF7vkOc/edit?slide=id.p1#slide=id.p1)

---

## 🧠 What It Does
- Pulls articles from major Indian news RSS feeds
- Uses **LLMs (Large Language Models)** to:
    - Filter animal-welfare-relevant content
    - Perform sentiment analysis (positive/negative/critical)
    - Extract themes (e.g., cruelty, rescue, police, law)
- Tags articles with location, tone, festival relevance, and authority involved
- Outputs insights to:
    - A user-friendly web interface
    - A connected Google Sheet for structured data analysis

---

## 📦 Tech Stack
- **Frontend:** React + Tailwind CSS + Recharts
- **Backend:** Spring Boot + OpenAI API
- **Database:** Google Sheets for MVP storage
- **Deployment:** Render (free tier hosting)

---

## 📊 Features
- 📥 Manual “Trigger Fetch” for last 3 years data (Currently Disabled as some data already dumped)
- Automatic Daily Schedule Trigger on latest news
- 🧠 Real-time AI classification for sentiment and themes
- 📈 Visual dashboards for:
    - Sentiment over time
    - Festival-specific animal issues
    - Theme frequency and location mapping
- 📄 Google Sheet output for data export, filtering, and sharing

---

## 🎯 Use Cases
- 📰 Media watchdogs can understand bias in reporting
- 🐶 NGOs can track cruelty incidents and rescue stories
- 📢 Activists can use insights for campaigns
- 📊 Analysts can study tone around festivals like Holi or Bakra Eid

---

## 📌 Next Steps
- Add Gmail/WhatsApp bot integration for field-level alerts
- Geo-map incidents with sentiment postive/negative on the UI
- Most Prevalent Themes with Hot Spot Graph
- Export PDF reports for advocacy groups

---

## 🙌 Contributions
This was a solo-built MVP during a time-boxed sprint. Feedback and collaborations are welcome!

